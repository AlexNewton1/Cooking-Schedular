package com.SoftwareOverflow.CookingScheduler;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class HomeScreen extends AppCompatActivity implements Dialog.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        //force portrait for phones
        if(getResources().getBoolean(R.bool.portrait_only)) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);

        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);
        ImageView logo = (ImageView) findViewById(R.id.homeScreenImageView);
        int size = Math.min(outMetrics.heightPixels, outMetrics.widthPixels);
        logo.getLayoutParams().height = size/2;
        logo.getLayoutParams().width = size/2;
    }

    public void createMeal(View v){
        Intent i = new Intent(this, ItemScreen.class);
        if (v.getId() == R.id.loadPreviousMeal) i.putExtra("loadValues", true);
        startActivity(i);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home_screen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch(id){
            case R.id.reminders_menu_item:
                showUpcomingReminders();
        }

        return super.onOptionsItemSelected(item);
    }

    private void showUpcomingReminders() {
        SharedPreferences sharedPrefs = getSharedPreferences("alarms", MODE_PRIVATE);
        String alarmString = sharedPrefs.getString("alarmInfo", "");

        if(!alarmString.equals("")){
            View dialogView = View.inflate(this, R.layout.upcoming_reminders_dialog, null);
            ListView alarmList = (ListView) dialogView.findViewById(R.id.alarmListView);
            final String[] adapterStrings = alarmString.split("\\|\\|");
            ListAdapter adapter = new UpcomingRemindersAdapter(this, adapterStrings);
            alarmList.setAdapter(adapter);
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setView(dialogView)
                    .setNegativeButton("Cancel", this)
                    .setPositiveButton("Delete All Reminders", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(HomeScreen.this);
                            builder.setTitle("Delete All Reminders?\nThis Can't Be Undone").setNegativeButton("Cancel", this)
                                    .setPositiveButton("Delete All Reminders", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            for (String info : adapterStrings) {
                                                String[] splitInfo = info.split("\\|");
                                                ShowTimes.cancelPendingIntent(splitInfo[0], Integer.parseInt(splitInfo[2]), getApplicationContext(), true);
                                            }
                                            dialog.dismiss();
                                            showUpcomingReminders();
                                        }
                                    }).show();
                        }
                    });

            final Dialog mainDialog = dialogBuilder.show();

            alarmList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                    new AlertDialog.Builder(HomeScreen.this)
                            .setTitle("Delete Reminder?\nThis Can't Be Undone")
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).setPositiveButton("Delete Reminder", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String[] info = adapterStrings[position].split("\\|");
                            ShowTimes.cancelPendingIntent(info[0], Integer.parseInt(info[2]), getApplicationContext(), true);
                            dialog.dismiss();
                            mainDialog.dismiss();
                            showUpcomingReminders();
                        }
                    }).show();

                    return true;
                }
            });
        }else Toast.makeText(this, R.string.no_upcoming_alarms, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
    }
}
