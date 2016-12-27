package com.SoftwareOverflow.CookingScheduler;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Creates notifications
 */
public class NotificationReceiver extends BroadcastReceiver {

    private boolean showNoMeals = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        String name = bundle.getString("name");
        String mainText = bundle.getString("mainText");
        showNoMeals = bundle.getBoolean("showNoMeals", true);
        int id = bundle.getInt("id");


        Notification notification = new NotificationCompat.Builder(context)
                .setAutoCancel(true)
                .setContentTitle("Cooking Scheduler: '" + name + "' now!")
                .setContentText(mainText)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(getNotificationIntent(context))
                .setSmallIcon(R.drawable.icon).build();

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notification.vibrate = new long[] {0};
        manager.notify((int) System.currentTimeMillis(), notification);

        cancelPendingIntent(id, context.getApplicationContext(), false);
    }

    private PendingIntent getNotificationIntent(Context context){
        Intent myIntent = new Intent(context, ShowTimes.class);

        SharedPreferences sharedPrefs = context.getSharedPreferences(
                "foodItems", Context.MODE_PRIVATE);
        String jsonString = sharedPrefs.getString("jsonString", "");
        Long readyTimeMillis = sharedPrefs.getLong("readyTime", 0);

        if (!jsonString.matches("") && readyTimeMillis != 0) {
            Calendar readyTimeCal = Calendar.getInstance();
            readyTimeCal.setTimeInMillis(readyTimeMillis);
            myIntent.putExtra("readyTimeCal", readyTimeCal).putExtra("jsonString", jsonString);
        }
        else if (showNoMeals) {
           myIntent = new Intent(context, HomeScreen.class);
            Toast.makeText(context, context.getString(R.string.no_previous_meal),
                    Toast.LENGTH_SHORT).show();
        }


        return PendingIntent.getActivity(
                context,
                0,
                myIntent,
                PendingIntent.FLAG_ONE_SHOT);
    }

    public void setReminders(Calendar readyTimeCal, FoodItem.Stage[] itemList, Context c) {
        ArrayList<NotificationClass> thisMealAlarms = new ArrayList<>();
        List<NotificationClass> alarmList = JsonHandler.getAlarmList(c, true);

        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        for (FoodItem.Stage stage : itemList) {

            String name = stage.getFoodItemName() + " - " + stage.getName();
            int time = stage.getEffectiveTotalTime();

            Calendar alarmCal = Calendar.getInstance();
            alarmCal.setTimeInMillis(readyTimeCal.getTimeInMillis());
            alarmCal.add(Calendar.MINUTE, -time);

            NotificationClass alarm = new NotificationClass(time, alarmCal.getTimeInMillis(),
                    name, (int) System.currentTimeMillis());
            alarmList.add(alarm);
            thisMealAlarms.add(alarm);

            PendingIntent pi = createPendingIntent(c.getApplicationContext(), alarm);
            am.set(AlarmManager.RTC_WAKEUP, alarmCal.getTimeInMillis(), pi);
        }
        Toast.makeText(c, c.getString(R.string.reminders_set), Toast.LENGTH_SHORT).show();
        JsonHandler.putAlarmList(c, alarmList, true);
        JsonHandler.putAlarmList(c, thisMealAlarms, false);
    }

    private static PendingIntent createPendingIntent(Context c, NotificationClass alarm) {
        Intent alarmIntent = new Intent(c.getApplicationContext(), NotificationReceiver.class);
        alarmIntent.putExtra("name", alarm.name);
        alarmIntent.putExtra("id", alarm.id);
        alarmIntent.putExtra("showNoMeals", false);
        return PendingIntent.getBroadcast(c.getApplicationContext(),
                alarm.id, alarmIntent, PendingIntent.FLAG_ONE_SHOT);
    }

    public static void cancelPendingIntent(final int id, final Context context, boolean showToast) {
        List<NotificationClass> alarmList = JsonHandler.getAlarmList(context, true);

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

        JsonHandler.putAlarmList(context.getApplicationContext(), alarmList, true);
        if (showToast) {
            if (!success) Toast.makeText(context,
                    context.getString(R.string.error_deleting_reminder), Toast.LENGTH_SHORT).show();
            else Toast.makeText(context,
                    context.getString(R.string.reminder_deleted), Toast.LENGTH_SHORT).show();
        }
    }

}
