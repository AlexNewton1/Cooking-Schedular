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
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;


public class ShowTimes extends Activity {

    private static Calendar readyTimeCal;
    private List<FoodItem> foodItemList = new ArrayList<>();
    private MealDatabase mealDatabase;
    private InterstitialAd interstitialAd;
    private int currentItem = 0;
    private ArrayAdapter adapter;
    private boolean[] isStageDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_times);
        HomeScreen.setupBilling(this);

        //force portrait mode for phones
        if (getResources().getBoolean(R.bool.portrait_only))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        SharedPreferences sharedPrefs = getSharedPreferences("foodItems", MODE_PRIVATE);
        currentItem = sharedPrefs.getInt("currentItem", 0);
        String jsonString = sharedPrefs.getString("jsonString", "");

        mealDatabase = new MealDatabase(this, null);
        //getting Intent extras
        Bundle extras = getIntent().getExtras();
        readyTimeCal = (Calendar) extras.get("readyTimeCal");
        currentItem = extras.getInt("currentItem");

        if(jsonString.matches("")){
            foodItemList = JsonHandler.getFoodItemList(this, extras.getString("jsonString"));
        }
        else{
            foodItemList = JsonHandler.getFoodItemList(this, jsonString);
        }


        final List<FoodItem.Stage> stageList = new ArrayList<>();
        for (FoodItem aFoodItem : foodItemList) {
            for (FoodItem.Stage stage : aFoodItem.getStages()) {
                stageList.add(stage);
            }
        }
        Collections.sort(stageList);
        final FoodItem.Stage[] stageInfo = stageList.toArray(new FoodItem.Stage[stageList.size()]);
        isStageDone = new boolean[stageList.size()];

        TextView readyTimeTV = (TextView) findViewById(R.id.readyTimeTV);
        readyTimeTV.setText(String.format(Locale.getDefault(), "%02d:%02d",
                readyTimeCal.get(Calendar.HOUR_OF_DAY), readyTimeCal.get(Calendar.MINUTE)));
        if (extras.getBoolean("reminders")) setReminders(readyTimeCal, stageInfo);

        adapter = new AdapterShowTimes(this, stageInfo, stageInfo.length);
        showListView();

        //color done stages in grey

        for (int i = 0; i <=currentItem; i++) {
            if (i > 0) {
                isStageDone[i - 1] = true;
            }
        }

        final Handler handler = new Handler();
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (currentItem <= stageList.size()) {
                    if (currentItem > 0) {
                        isStageDone[currentItem - 1] = true;
                        adapter = new AdapterShowTimes(ShowTimes.this, stageInfo, stageInfo.length);
                        showListView();
                    }

                    currentItem++;

                    if(currentItem<=stageList.size()) {
                        long nextStageTime = getNextStageTime(stageList);
                        if ((nextStageTime - SystemClock.uptimeMillis()) > 0) //in the future
                            handler.postAtTime(this, nextStageTime);
                        else { //in the past
                            handler.post(this);
                        }
                    }

                } else {
                    handler.removeCallbacks(this);
                }
            }
        };


        handler.post(r);

        //load advert
       final AdView adView = (AdView) findViewById(R.id.showTimesScreenBannerAd);
        if(!BillingClass.isUpgraded) {
            Handler adHandler = new Handler();
            adHandler.post(new Runnable() {
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


    private long getNextStageTime(List<FoodItem.Stage> stageList) {
        Calendar stageStartTime = Calendar.getInstance();
        stageStartTime.setTimeInMillis(readyTimeCal.getTimeInMillis());
        int stageTime = stageList.get(stageList.size() - currentItem).getEffectiveTotalTime();
        stageStartTime.add(Calendar.MINUTE, -stageTime);

        long now = Calendar.getInstance().getTimeInMillis();
        long timeSinceStartup = SystemClock.uptimeMillis();
        long startupTime = now - timeSinceStartup;

        return stageStartTime.getTimeInMillis() - startupTime;
    }

    private void setReminders(Calendar readyTimeCal, FoodItem.Stage[] itemList) {
        List<NotificationClass> alarmList = JsonHandler.getAlarmList(this);
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        for (FoodItem.Stage stage : itemList) {

            String name = stage.getFoodItemName() + " - " + stage.getName();
            int time = stage.getEffectiveTotalTime();

            Calendar alarmCal = Calendar.getInstance();
            alarmCal.setTimeInMillis(readyTimeCal.getTimeInMillis());
            alarmCal.add(Calendar.MINUTE, -time);

            NotificationClass alarm = new NotificationClass(time, alarmCal.getTimeInMillis(),
                    name, (int) System.currentTimeMillis());
            alarmList.add(alarm);

            PendingIntent pi = createPendingIntent(getApplicationContext(), alarm);
            am.set(AlarmManager.RTC_WAKEUP, alarmCal.getTimeInMillis(), pi);
        }
        Toast.makeText(this, getString(R.string.reminders_set), Toast.LENGTH_SHORT).show();
        JsonHandler.putAlarmList(getApplicationContext(), alarmList);
    }

    private static PendingIntent createPendingIntent(Context c, NotificationClass alarm) {
        Intent alarmIntent = new Intent(c.getApplicationContext(), NotificationReceiver.class);
        alarmIntent.putExtra("name", alarm.name);
        alarmIntent.putExtra("id", alarm.id);
        return PendingIntent.getBroadcast(c.getApplicationContext(),
                alarm.id, alarmIntent, PendingIntent.FLAG_ONE_SHOT);
    }

    public static void cancelPendingIntent(final int id, final Context context, boolean showToast) {
        List<NotificationClass> alarmList = JsonHandler.getAlarmList(context);

        boolean success = false;
        for (int i = 0; i < alarmList.size(); i++) {
            int intentID = alarmList.get(i).id;
            if (intentID == id) {
                createPendingIntent(context.getApplicationContext(), alarmList.get(i)).cancel();
                alarmList.remove(i);
                success = true;
                break;
            }
        }

        JsonHandler.putAlarmList(context.getApplicationContext(), alarmList);
        if (showToast) {
            if (!success) Toast.makeText(context,
                    context.getString(R.string.error_deleting_reminder), Toast.LENGTH_SHORT).show();
            else Toast.makeText(context,
                    context.getString(R.string.reminder_deleted), Toast.LENGTH_SHORT).show();
        }
    }

    private void showListView() {
        //send in length to show LV in correct (reverse) order
        ListView foodList = (ListView) findViewById(R.id.foodListView);
        foodList.setAdapter(adapter);

    }


    public static Calendar getReadyTimeCal() {
        return readyTimeCal;
    }

    public void saveMeal(View view) {
        if (BillingClass.isUpgraded) {
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
                        if (mealDatabase.addMeal(name, jsonString, notes, -1)) {
                            dialog.dismiss(); //dismiss dialog when meal saved successfully
                        }
                    } else
                        Toast.makeText(ShowTimes.this, getString(R.string.enter_meal_name),
                                Toast.LENGTH_SHORT).show();
                }
            });
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        } else Toast.makeText(this, getString(R.string.upgrade_to_unlock),
                Toast.LENGTH_SHORT).show();
    }

    public void finished(View v) {
        final Intent i = new Intent(this, HomeScreen.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (interstitialAd != null && interstitialAd.isLoaded()) {
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
        } else {
            startActivity(i);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        String jsonString = JsonHandler.getFoodItemJsonString(foodItemList);
        long readyTimeMillis = readyTimeCal.getTimeInMillis();

        SharedPreferences sharedPrefs = getSharedPreferences("foodItems", MODE_PRIVATE);
        sharedPrefs.edit()
                .putString("jsonString", jsonString)
                .putLong("readyTime", readyTimeMillis)
                .putInt("currentItem", currentItem)
                .apply();
    }


    private class AdapterShowTimes extends ArrayAdapter<FoodItem.Stage> {
        int length;

        AdapterShowTimes(Context context, FoodItem.Stage[] stages, int length) {
            super(context, R.layout.lv_show_times, stages);
            this.length = length;
        }

        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(ShowTimes.this);
                convertView = inflater.inflate(R.layout.lv_show_times, parent, false);


                if (isStageDone[position]) {
                    LinearLayout ll = (LinearLayout) convertView.findViewById(R.id.show_times_layout);
                    ll.setBackgroundColor(getResources().getColor(R.color.lightgrey));
                    convertView.setBackgroundColor(getResources().getColor(R.color.lightgrey));
                }

                FoodItem.Stage stage = getItem(length - position - 1);
                int effectiveTotalTime = stage.getEffectiveTotalTime();

                TextView startTime = (TextView) convertView.findViewById(R.id.startTimeTV);
                TextView startItem = (TextView) convertView.findViewById(R.id.startItemTV);

                Calendar readyTimeCal = Calendar.getInstance();
                readyTimeCal.setTime(ShowTimes.getReadyTimeCal().getTime());
                readyTimeCal.add(Calendar.MINUTE, -effectiveTotalTime);

                startItem.setText(String.format(Locale.getDefault(), "%s - %s \n(%s " +
                                getString(R.string.minutes) + ")", stage.getFoodItemName(),
                        stage.getName(), stage.getTime()));
                startTime.setText(String.format(Locale.getDefault(), "%02d:%02d",
                        readyTimeCal.get(Calendar.HOUR_OF_DAY), readyTimeCal.get(Calendar.MINUTE)));
            }
            return convertView;
        }
    }

}
