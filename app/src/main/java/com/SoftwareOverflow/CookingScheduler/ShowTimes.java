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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ShowTimes extends Activity {

    private static Calendar readyTimeCal;
    private List<FoodItem> foodItemList = new ArrayList<>();
    private MealDatabase mealDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_times);
        //force portrait mode for phones
        if (getResources().getBoolean(R.bool.portrait_only)) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);

        /*
        //set up adView
        AdView adView = (AdView) findViewById(R.id.showTimesScreenBannerAd);
        AdRequest adRequest = new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR) //adding emulator and phone as test devices
                .addTestDevice("2A0E7D2865A3C592033F3707402D0BBB").build();
        adView.loadAd(adRequest);
        */


        mealDatabase = new MealDatabase(this, null);
        //getting Intent extras
        Bundle extras = getIntent().getExtras();
        foodItemList = JsonHandler.getFoodItemList(this,extras.getString("jsonString"));
        readyTimeCal = (Calendar) extras.get("readyTimeCal");


        //preparing String array for listView & reminders
        Map<Integer, String> map = new TreeMap<>(); //holds information in the form  EffectiveTimeForReminder --> foodItemName|stageName|stageTime
        for(FoodItem aFoodItem : foodItemList){
            List<String> currentFoodItemStages = aFoodItem.getFoodStages();
            int effectiveTotalTime = 0;
            for(int i=0; i<currentFoodItemStages.size(); i++){
                String[] foodStageString= currentFoodItemStages.get(i).split("\\|");
                effectiveTotalTime += Integer.parseInt(foodStageString[1]);
                map.put(effectiveTotalTime, aFoodItem.name + "|" + foodStageString[0] + "|" + foodStageString[1]);
            }
        }
        String[] stageInfo = map.values().toArray(new String[map.values().size()]);

        TextView readyTimeTV = (TextView) findViewById(R.id.readyTimeTV);
        readyTimeTV.setText(String.format("%02d:%02d", readyTimeCal.get(Calendar.HOUR_OF_DAY), readyTimeCal.get(Calendar.MINUTE)));
        if(extras.getBoolean("reminders")) setReminders(readyTimeCal, stageInfo);
        showListView(stageInfo);
        }

    private void setReminders(Calendar readyTimeCal, String[] itemList) {
        List<AlarmClass> alarmList = JsonHandler.getAlarmList(getApplicationContext());
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        for(String infoString : itemList) {
            String[] info = infoString.split("\\|");
            String name = info[0] + " - " + info[1];
            int time = Integer.parseInt(info[2]);

            Calendar alarmCal = Calendar.getInstance();
            alarmCal.setTimeInMillis(readyTimeCal.getTimeInMillis());
            alarmCal.add(Calendar.MINUTE, -time);

            AlarmClass alarm = new AlarmClass(time, alarmCal.getTimeInMillis(), name, (int) System.currentTimeMillis());
            alarmList.add(alarm);

            PendingIntent pi = createPendingIntent(getApplicationContext(), alarm.name, alarm.id);
            am.set(AlarmManager.RTC_WAKEUP, alarmCal.getTimeInMillis(), pi);
        }
        Toast.makeText(this, "Reminders Set", Toast.LENGTH_SHORT).show();
        JsonHandler.putAlarmList(getApplicationContext(), alarmList);
    }

    //TODO -- fix deletion of reminders after completion
    private static PendingIntent createPendingIntent(Context c, String name, int id){
        Intent alarmIntent = new Intent(c.getApplicationContext(), AlarmReceiver.class);
        alarmIntent.putExtra("name", name);
        alarmIntent.putExtra("id", id);
        return PendingIntent.getBroadcast(c.getApplicationContext(), id, alarmIntent, PendingIntent.FLAG_ONE_SHOT);
    }

    public static void cancelPendingIntent(String name,final int id, final Context context, boolean showToast){
        List<AlarmClass> alarmList = JsonHandler.getAlarmList(context);

        boolean success = false;
        for(int i=0; i<alarmList.size(); i++) {
            int intentID = alarmList.get(i).id;
            if (intentID == id) {
                createPendingIntent(context.getApplicationContext(), name, id).cancel();
                alarmList.remove(i);
                success = true;
                break;
            }
        }

        JsonHandler.putAlarmList(context.getApplicationContext(), alarmList);
        if(showToast) {
            if (!success) Toast.makeText(context, "Error Deleting Reminder", Toast.LENGTH_SHORT).show();
            else Toast.makeText(context, "Reminder Deleted", Toast.LENGTH_SHORT).show();
        }
    }

    private void showListView(String[] adapterStrings) {
        //send in length to show LV in correct (reverse) order
        ArrayAdapter adapter = new AdapterShowTimes(this, adapterStrings, adapterStrings.length);
        ListView foodList = (ListView) findViewById(R.id.foodListView);
        foodList.setAdapter(adapter);
    }

    public void finished(View v){
        Intent i = new Intent(this, HomeScreen.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    public static Calendar getReadyTimeCal(){return readyTimeCal;}

    public void saveMeal(View view){
        //TODO - check if free/upgraded

        LayoutInflater inflater = LayoutInflater.from(this);
        View v = inflater.inflate(R.layout.dialog_save_meal, null);
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
                    String jsonString = JsonHandler.getFoodItemJsonString(ShowTimes.this, foodItemList);
                    mealDatabase.addMeal(name, jsonString, notes, -1);
                } else
                    Toast.makeText(ShowTimes.this, "Please enter a name for the meal", Toast.LENGTH_SHORT).show();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    @Override
    protected void onDestroy() {
        String jsonString = JsonHandler.getFoodItemJsonString(this, foodItemList);
        long readyTimeMillis = readyTimeCal.getTimeInMillis();

        SharedPreferences sharedPrefs = getSharedPreferences("foodItems", MODE_PRIVATE);
        sharedPrefs.edit().putString("jsonString", jsonString ).putLong("readyTime", readyTimeMillis).apply();
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
            LayoutInflater inflater = LayoutInflater.from(ShowTimes.this);
            View customView = inflater.inflate(R.layout.lv_show_times, null);

            String[] info = getItem(length-position-1).split("\\|");
            int cookingTime = Integer.parseInt(info[2]);

            TextView startTime = (TextView) customView.findViewById(R.id.startTimeTV);
            TextView startItem = (TextView) customView.findViewById(R.id.startItemTV);

            Calendar readyTimeCal = Calendar.getInstance();
            readyTimeCal.setTime(ShowTimes.getReadyTimeCal().getTime());
            readyTimeCal.add(Calendar.MINUTE, -cookingTime );

            startItem.setText(info[0] + " - " + info[1] + "\n(" + info[2] + " mins)");
            startTime.setText(String.format("%02d:%02d", readyTimeCal.get(Calendar.HOUR_OF_DAY), readyTimeCal.get(Calendar.MINUTE)));

            return customView;
        }
    }

}
