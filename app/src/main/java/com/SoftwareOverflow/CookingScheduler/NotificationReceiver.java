package com.SoftwareOverflow.CookingScheduler;

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

import java.util.Calendar;

/**
 * Creates notifications
 */
public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        String name = bundle.getString("name");
        String mainText = bundle.getString("mainText");
        int id = bundle.getInt("id");



        Notification notification = new NotificationCompat.Builder(context)
                .setAutoCancel(true)
                .setContentTitle("Cooking Scheduler: '" + name + "' now!")
                .setContentText(mainText)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(getNotificationIntent(context))
                .setSmallIcon(R.drawable.launcher).build();

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notification.vibrate = new long[] {0};
        manager.notify((int) System.currentTimeMillis(), notification);

        ShowTimes.cancelPendingIntent(id, context.getApplicationContext(), false);
    }

    private PendingIntent getNotificationIntent(Context context){
        Intent myIntent = new Intent(context, ShowTimes.class);

        SharedPreferences sharedPrefs = context.getSharedPreferences(
                "foodItems", Context.MODE_PRIVATE);
        String jsonString = sharedPrefs.getString("jsonString", "");
        Long readyTimeMillis = sharedPrefs.getLong("readyTime", 0);

        if(!jsonString.matches("") && readyTimeMillis!=0) {
            Calendar readyTimeCal = Calendar.getInstance();
            readyTimeCal.setTimeInMillis(readyTimeMillis);
            myIntent.putExtra("readyTimeCal", readyTimeCal).putExtra("jsonString", jsonString);
        }
        else {
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

}
