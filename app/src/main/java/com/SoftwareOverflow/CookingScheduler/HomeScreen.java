package com.SoftwareOverflow.CookingScheduler;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class HomeScreen extends Activity implements Dialog.OnClickListener {

    //TODO - add in app billing for pro version (ad free & save meals)

    private Toast mToast;
    public static int screenHeight;

    @Override
    @SuppressLint("ShowToast")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenHeight = size.y;

        //force portrait for phones
        if (getResources().getBoolean(R.bool.portrait_only))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);

        mToast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
    }

    public void createMeal(View v) {
        startActivity(new Intent(this, ItemScreen.class));
    }
    public void getTimings(View v){
        SharedPreferences sharedPrefs = getSharedPreferences("foodItems", MODE_PRIVATE);
        String jsonString = sharedPrefs.getString("jsonString", "");
        Long readyTimeMillis = sharedPrefs.getLong("readyTime", 0);

        if(!jsonString.matches("") && readyTimeMillis!=0) {
            Calendar readyTimeCal = Calendar.getInstance();
            readyTimeCal.setTimeInMillis(readyTimeMillis);
            startActivity(new Intent(this, ShowTimes.class).putExtra("readyTimeCal", readyTimeCal).putExtra("jsonString", jsonString));
        }
        else {
            mToast.setText("No previous timings found.");
            mToast.show();
        }
    }
    public void loadMeal(View v){
        //TODO - check if upgraded before showing saved meals.....
        MealDatabase mealDB = new MealDatabase(HomeScreen.this, null);
        if(mealDB.getRowNum()!=0) startActivity(new Intent(HomeScreen.this, SavedMeals.class));
        else {
            mToast.setText( "No Saved Meals");
            mToast.show();
        }
    }

    private void showUpcomingReminders() {
        final List<AlarmClass> alarmList = JsonHandler.getAlarmList(this);
        Collections.sort(alarmList);

        if (alarmList.size() > 0) {
            View dialogView = View.inflate(this, R.layout.dialog_reminders, null);
            ListView alarmListView = (ListView) dialogView.findViewById(R.id.alarmListView);
            final String[] adapterStrings = new String[alarmList.size()];
            for (int i = 0; i < alarmList.size(); i++) {
                adapterStrings[i] = alarmList.get(i).getInfo();
            }
            ListAdapter adapter = new AdapterReminders(this, adapterStrings);
            alarmListView.setAdapter(adapter);
            final AlertDialog alertDialog = new AlertDialog.Builder(this).setView(dialogView).show();

            Button cancelButton = (Button) alertDialog.findViewById(R.id.cancelRemindersButton);
            Button deleteAllButton = (Button) alertDialog.findViewById(R.id.deleteAllRemindersButton);
            deleteAllButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(HomeScreen.this)
                            .setTitle("Delete All Reminders?\nThis Can't Be Undone")
                            .setNegativeButton("Cancel", HomeScreen.this)
                            .setPositiveButton("Delete All Reminders", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    for (AlarmClass alarm : alarmList) {
                                        ShowTimes.cancelPendingIntent(alarm.name, alarm.id, getApplicationContext(), false);
                                    }
                                    dialog.dismiss();
                                    alertDialog.dismiss();
                                    showUpcomingReminders();
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).show();
                }
            });
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    alertDialog.dismiss();
                }
            });


            alarmListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                    new AlertDialog.Builder(HomeScreen.this)
                            .setTitle("Delete Reminder?\nThis Can't Be Undone")
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .setPositiveButton("Delete Reminder", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    AlarmClass alarm = alarmList.get(position);
                                    ShowTimes.cancelPendingIntent(alarm.name, alarm.id, getApplicationContext(), true);
                                    alertDialog.dismiss();
                                    showUpcomingReminders();
                                    dialog.dismiss();
                                }
                            }).show();
                    return true;
                }
            });
        } else Toast.makeText(this, R.string.no_upcoming_alarms, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        mToast.cancel();
        super.onPause();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home_screen, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.reminders_menu_item:
                showUpcomingReminders();
                break;
            case R.id.upgrade_menu_item:
                startActivity(new Intent(HomeScreen.this, UpgradeScreen.class));
                break;
            case R.id.show_saved_meals_menu_item:
                loadMeal(getWindow().getDecorView());
                break;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
    }



    private class AdapterReminders extends ArrayAdapter<String> {
        public AdapterReminders(Context context, String[] adapterStrings) {
            super(context, R.layout.lv_reminders, adapterStrings);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View customView = inflater.inflate(R.layout.lv_reminders, null);

            TextView nameTV = (TextView) customView.findViewById(R.id.upcomingReminderItemTV);
            TextView timeTV = (TextView) customView.findViewById(R.id.upcomingReminderTimeTV);

            String[] info = getItem(position).split("\\|");
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(Long.parseLong(info[1]));

            nameTV.setText(info[0]);
            timeTV.setText(String.format("%02d-%02d-%04d at %02d:%02d", cal.get(Calendar.DAY_OF_MONTH),
                    cal.get(Calendar.MONTH), cal.get(Calendar.YEAR), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)));

            return customView;
        }
    }
}
