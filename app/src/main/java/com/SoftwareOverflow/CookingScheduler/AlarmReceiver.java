package com.SoftwareOverflow.CookingScheduler;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

/**
 * Creates notifications
 */
public class AlarmReceiver extends BroadcastReceiver {

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
                .setSmallIcon(R.drawable.launcher).build();

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notification.vibrate = new long[] {0};
        manager.notify((int) System.currentTimeMillis(), notification);

        ShowTimes.cancelPendingIntent(name, id, context.getApplicationContext(), false);
    }



}
