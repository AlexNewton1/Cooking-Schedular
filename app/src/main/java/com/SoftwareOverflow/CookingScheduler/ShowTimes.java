package com.SoftwareOverflow.CookingScheduler;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class ShowTimes extends AppCompatActivity {

    private static Calendar readyTimeCal;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_times);


        if (getResources().getBoolean(R.bool.portrait_only)) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT); //force portrait mode for phones

        //set up adView
        AdView adView = (AdView) findViewById(R.id.showTimesScreenBannerAd);
        AdRequest adRequest = new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR) //adding emulator and phone as test devices
                .addTestDevice("2A0E7D2865A3C592033F3707402D0BBB").build();
        adView.loadAd(adRequest);

        //getting Intent extras
        Bundle extras = getIntent().getExtras();
        String[] itemList = extras.getStringArray("itemArray");
        readyTimeCal = (Calendar) extras.get("readyTimeCal");

        TextView readyTimeTV = (TextView) findViewById(R.id.readyTimeTV);
        readyTimeTV.setText(String.format("%02d:%02d", readyTimeCal.get(Calendar.HOUR_OF_DAY), readyTimeCal.get(Calendar.MINUTE)));

        if(extras.getBoolean("reminders")) setReminders(itemList, readyTimeCal);

        showListView(itemList);
        }

    private void setReminders(String[] itemList, Calendar readyTimeCal) {
        SharedPreferences sharedPrefs = getSharedPreferences("alarms", MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        sb.append(sharedPrefs.getString("alarmInfo", ""));

        for(String item : itemList) { //foreach loop
            String[] splitInfo = item.split("\\|");
            String name = splitInfo[0];
            int cookingTime = Integer.parseInt(splitInfo[1]);

            Calendar alarmCal = Calendar.getInstance();
            alarmCal.setTimeInMillis(readyTimeCal.getTimeInMillis());
            alarmCal.add(Calendar.MINUTE, -cookingTime);

            int id = (int) System.currentTimeMillis();

            AlarmClass alarmClass = new AlarmClass(alarmCal.getTimeInMillis(), name, id);
            sb.append(alarmClass.getInfo()); //contains | delimiter between strings and || between classes

            Intent alarmIntent = new Intent(getApplicationContext(), AlarmReceiver.class);
            alarmIntent.putExtra("name", name);
            alarmIntent.putExtra("id", id);
            Log.d("cal", "alarmIntent extra: " + id);
            PendingIntent pIntent =  PendingIntent.getBroadcast(getApplicationContext(), id, alarmIntent, PendingIntent.FLAG_ONE_SHOT);


            AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
            manager.set(AlarmManager.RTC_WAKEUP, alarmCal.getTimeInMillis(), pIntent);
        }

        sharedPrefs.edit().putString("alarmInfo", sb.toString()).apply();
    }


    public static void cancelPendingIntent(String name,final int id, final Context context, boolean showToast){
        Intent alarmIntent = new Intent(context, AlarmReceiver.class);
        alarmIntent.putExtra("name", name);
        alarmIntent.putExtra("id", id);
        PendingIntent.getBroadcast(context, id, alarmIntent, PendingIntent.FLAG_ONE_SHOT).cancel();

        final IncomingHandlerCallback incomingHandler = new IncomingHandlerCallback(context);
        incomingHandler.showToast = showToast;
        final Handler handler = new Handler(incomingHandler);

        new Thread(new Runnable() { //Thread to run in background
            @Override
            public void run() {
                SharedPreferences sharedPrefs = context.getSharedPreferences("alarms", MODE_PRIVATE);
                String infoString = sharedPrefs.getString("alarmInfo", "");

                String[] alarmInfo = infoString.split("\\|\\|");
                List<String> alarmList = new ArrayList<>(Arrays.asList(alarmInfo));

                boolean success = false;
                for(int i=0; i<alarmList.size(); i++) {
                    int intentID = Integer.parseInt(alarmList.get(i).split("\\|")[2]);
                    Log.d("cal", "Inside runnable: " + intentID);
                    if (intentID == id) {
                        Log.d("cal", "match!");
                        alarmList.remove(i);
                        success = true;
                    }
                }

                StringBuilder sb = new StringBuilder();
                for(String item : alarmList) sb.append(item);
                Log.d("cal", sb.toString());

                sharedPrefs.edit().putString("alarmInfo", sb.toString()).apply();
                incomingHandler.removed = success;
                handler.sendEmptyMessage(0); //handles toast (deleted / failed to delete)
            }
        }).start();


    }

    private void showListView(String[] adapterStrings) {
        ArrayAdapter adapter = new ShowTimesAdapter(this, adapterStrings);
        ListView foodList = (ListView) findViewById(R.id.foodListView);
        foodList.setAdapter(adapter);
    }

    public void finished(View v){
        Intent i = new Intent(this, HomeScreen.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    public static Calendar getReadyTimeCal(){return readyTimeCal;}


    /**
     * class to handle toasts after deletion of upcoming reminders
     */
    static class IncomingHandlerCallback implements Handler.Callback {
        private Context context;
        public boolean removed = false;
        public boolean showToast = true;

        public IncomingHandlerCallback(Context c){
            this.context = c;
        }

        @Override
        public boolean handleMessage(Message msg) {
            if(showToast) {
                if (removed) Toast.makeText(context, "Reminder Deleted", Toast.LENGTH_SHORT).show();
                else Toast.makeText(context, "Error Deleting Reminder", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
}
}
