package com.SoftwareOverflow.CookingScheduler;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.SoftwareOverflow.CookingScheduler.util.BillingClass;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class ShowTimes extends Activity {

    private static Calendar readyTimeCal;
    private List<FoodItem> foodItemList = new ArrayList<>();
    private MealDatabase mealDatabase;
    private InterstitialAd interstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_times);
        //force portrait mode for phones
        if (getResources().getBoolean(R.bool.portrait_only))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);


        mealDatabase = new MealDatabase(this, null);
        //getting Intent extras
        Bundle extras = getIntent().getExtras();
        foodItemList = JsonHandler.getFoodItemList(this,extras.getString("jsonString"));
        readyTimeCal = (Calendar) extras.get("readyTimeCal");


        //preparing String array for listView & reminders
        //Maps  EffectiveTimeForReminder --> foodItemName|stageName|stageTime
        Map<Integer, String> map = new TreeMap<>();
        for(FoodItem aFoodItem : foodItemList){
            List<String> currentFoodItemStages = aFoodItem.getFoodStages();
            int effectiveTotalTime = 0;
            for(int i=currentFoodItemStages.size()-1; i>=0;  i--){
                String[] foodStageString= currentFoodItemStages.get(i).split("\\|");
                effectiveTotalTime += Integer.parseInt(foodStageString[1]);
                map.put(effectiveTotalTime, aFoodItem.name + "|" + foodStageString[0] + "|"
                        + foodStageString[1] + "|" + effectiveTotalTime);
            }
        }
        String[] stageInfo = map.values().toArray(new String[map.values().size()]);

        TextView readyTimeTV = (TextView) findViewById(R.id.readyTimeTV);
        readyTimeTV.setText(String.format(Locale.getDefault(), "%02d:%02d",
                readyTimeCal.get(Calendar.HOUR_OF_DAY), readyTimeCal.get(Calendar.MINUTE)));
        if(extras.getBoolean("reminders")) setReminders(readyTimeCal, stageInfo);
        showListView(stageInfo);

        //start loading ad 1 second after screen loaded
        final AdView adView = (AdView) findViewById(R.id.showTimesScreenBannerAd);
        if(!BillingClass.isUpgraded) {
            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    AdRequest adRequest = new AdRequest.Builder().build();
                    adView.loadAd(adRequest);
                    interstitialAd = new InterstitialAd(ShowTimes.this);
                    interstitialAd.setAdUnitId(getResources().
                            getString(R.string.interstitial_ad_unit_id));
                    interstitialAd.loadAd(adRequest);
                }
            });
        }
        else adView.setVisibility(View.GONE);
    }

    private void setReminders(Calendar readyTimeCal, String[] itemList) {
        List<NotificationClass> alarmList = JsonHandler.getAlarmList(this);
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        for(String infoString : itemList) {
            String[] info = infoString.split("\\|");
            String name = info[0] + " - " + info[1];
            int time = Integer.parseInt(info[3]);

            Calendar alarmCal = Calendar.getInstance();
            alarmCal.setTimeInMillis(readyTimeCal.getTimeInMillis());
            alarmCal.add(Calendar.MINUTE, -time);

            NotificationClass alarm = new NotificationClass(time, alarmCal.getTimeInMillis(),
                    name, (int) System.currentTimeMillis());
            alarmList.add(alarm);

            PendingIntent pi = createPendingIntent(getApplicationContext(), alarm);
            am.set(AlarmManager.RTC_WAKEUP, alarmCal.getTimeInMillis(), pi);
        }
        Toast.makeText(this, "Reminders Set", Toast.LENGTH_SHORT).show();
        JsonHandler.putAlarmList(getApplicationContext(), alarmList);
    }

    private static PendingIntent createPendingIntent(Context c, NotificationClass alarm){
        Intent alarmIntent = new Intent(c.getApplicationContext(), NotificationReceiver.class);
        alarmIntent.putExtra("name", alarm.name);
        alarmIntent.putExtra("id", alarm.id);
        return PendingIntent.getBroadcast(c.getApplicationContext(),
                alarm.id, alarmIntent, PendingIntent.FLAG_ONE_SHOT);
    }

    public static void cancelPendingIntent(final int id, final Context context, boolean showToast){
        List<NotificationClass> alarmList = JsonHandler.getAlarmList(context);

        boolean success = false;
        for(int i=0; i<alarmList.size(); i++) {
            int intentID = alarmList.get(i).id;
            if (intentID == id) {
                createPendingIntent(context.getApplicationContext(), alarmList.get(i)).cancel();
                alarmList.remove(i);
                success = true;
                break;
            }
        }

        JsonHandler.putAlarmList(context.getApplicationContext(), alarmList);
        if(showToast) {
            if (!success) Toast.makeText(context, "Error Deleting Reminder",
                    Toast.LENGTH_SHORT).show();
            else Toast.makeText(context, "Reminder Deleted", Toast.LENGTH_SHORT).show();
        }
    }

    private void showListView(String[] adapterStrings) {
        //send in length to show LV in correct (reverse) order
        ArrayAdapter adapter = new AdapterShowTimes(this, adapterStrings, adapterStrings.length);
        ListView foodList = (ListView) findViewById(R.id.foodListView);
        foodList.setAdapter(adapter);
    }


    public static Calendar getReadyTimeCal(){return readyTimeCal;}

    public void saveMeal(View view){
        if(BillingClass.isUpgraded) {
            LayoutInflater inflater = LayoutInflater.from(this);
            View v = inflater.inflate(R.layout.dialog_save_meal,
                    (ViewGroup) view.getRootView(), false);
            final AlertDialog dialog = new AlertDialog.Builder(this).setView(v).show();

            final EditText mealName = (EditText) v.findViewById(R.id.mealNameET);
            final EditText mealNotes = (EditText) v.findViewById(R.id.mealNotesET);


            Button save = (Button) v.findViewById(R.id.saveMealButton);
            Button cancel = (Button) v.findViewById(R.id.cancelSavingMealButton);
            save.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String name = mealName.getText().toString();
                    String notes = mealNotes.getText().toString();

                    if (!name.equals("")) {
                        String jsonString = JsonHandler.getFoodItemJsonString(foodItemList);
                        mealDatabase.addMeal(name, jsonString, notes, -1);
                    } else
                        Toast.makeText(ShowTimes.this, "Please enter a name for the meal",
                                Toast.LENGTH_SHORT).show();
                }
            });
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }
        else Toast.makeText(this, getString(R.string.upgrade_to_unlock),
                Toast.LENGTH_SHORT).show();
    }

    public void finished(View v){
        final Intent i = new Intent(this, HomeScreen.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if(interstitialAd != null && interstitialAd.isLoaded()){
            interstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    super.onAdClosed();
                    startActivity(i);
                }

                @Override
                public void onAdFailedToLoad(int errorCode) {
                    super.onAdFailedToLoad(errorCode);
                    startActivity(i);
                }
            });
            interstitialAd.show();
        }
        else{
            startActivity(i);
        }

    }

    @Override
    protected void onDestroy() {
        String jsonString = JsonHandler.getFoodItemJsonString(foodItemList);
        long readyTimeMillis = readyTimeCal.getTimeInMillis();

        SharedPreferences sharedPrefs = getSharedPreferences("foodItems", MODE_PRIVATE);
        sharedPrefs.edit().putString("jsonString", jsonString )
                .putLong("readyTime", readyTimeMillis).apply();
        super.onDestroy();
    }



    private class AdapterShowTimes extends ArrayAdapter<String> {
        int length;

        public AdapterShowTimes(Context context, String[] info, int length) {
            super(context, R.layout.lv_show_times, info);
            this.length = length;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(ShowTimes.this);
                convertView = inflater.inflate(R.layout.lv_show_times, parent, false);

                String[] info = getItem(length - position - 1).split("\\|");
                int effectiveTotalTime = Integer.parseInt(info[3]);

                TextView startTime = (TextView) convertView.findViewById(R.id.startTimeTV);
                TextView startItem = (TextView) convertView.findViewById(R.id.startItemTV);

                Calendar readyTimeCal = Calendar.getInstance();
                readyTimeCal.setTime(ShowTimes.getReadyTimeCal().getTime());
                readyTimeCal.add(Calendar.MINUTE, -effectiveTotalTime);

                startItem.setText(String.format(Locale.getDefault(), "%s - %s \n(%s mins)",
                        info[0], info[1], info[2]));
                startTime.setText(String.format(Locale.getDefault(), "%02d:%02d",
                        readyTimeCal.get(Calendar.HOUR_OF_DAY), readyTimeCal.get(Calendar.MINUTE)));
            }
            return convertView;
        }
    }

}
