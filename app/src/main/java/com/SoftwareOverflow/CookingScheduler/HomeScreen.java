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
import android.os.Bundle;
import android.support.annotation.NonNull;
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

import com.SoftwareOverflow.CookingScheduler.util.BillingClass;
import com.google.android.gms.ads.MobileAds;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class HomeScreen extends Activity implements Dialog.OnClickListener {

    private Toast mToast;
    public static int screenHeight;
    protected static BillingClass billing;
    private ViewGroup parentViewGroup = null;

    @Override
    @SuppressLint("ShowToast")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        parentViewGroup = (ViewGroup) findViewById(R.id.activity_home_screen);

        //force portrait for phones
        if (getResources().getBoolean(R.bool.portrait_only))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);

        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        MobileAds.initialize(this, getString(R.string.admob_app_id));

        setupBilling(this);
    }

    protected static void setupBilling(Context c) {
        billing = new BillingClass(c);
    }

    public void createMeal(View v) {
        SharedPreferences sp = getSharedPreferences("currentItems", MODE_PRIVATE);
        sp.edit().putString("currentItems", "").apply();
        startActivity(new Intent(this, ItemScreen.class));
    }

    public void getTimings(View v) {
        SharedPreferences sharedPrefs = getSharedPreferences("foodItems", MODE_PRIVATE);
        String jsonString = sharedPrefs.getString("jsonString", "");
        Long readyTimeMillis = sharedPrefs.getLong("readyTime", 0);

        if (!jsonString.matches("") && readyTimeMillis != 0) {
            Calendar readyTimeCal = Calendar.getInstance();
            readyTimeCal.setTimeInMillis(readyTimeMillis);
            startActivity(new Intent(this, ShowTimes.class).putExtra("readyTimeCal", readyTimeCal).putExtra("jsonString", jsonString));
        } else {
            mToast.setText("No previous timings found.");
            mToast.show();
        }
    }

    public void loadMeal(View v) {
        if (BillingClass.isUpgraded) { //upgraded => can save meals
            MealDatabase mealDB = new MealDatabase(HomeScreen.this, null);
            if (mealDB.getRowNum() != 0)
                startActivity(new Intent(HomeScreen.this, SavedMeals.class));
            else {
                mToast.setText("No Saved Meals");
                mToast.show();
            }
        } else {
            mToast.setText(getString(R.string.upgrade_to_unlock));
            mToast.show();
        }

    }

    private void showUpcomingReminders() {
        final List<NotificationClass> alarmList = JsonHandler.getAlarmList(this, true);
        Collections.sort(alarmList);

        if (alarmList.size() > 0) {
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_reminders, parentViewGroup, false);
            ListView alarmListView = (ListView) dialogView.findViewById(R.id.alarmListView);
            final String[] adapterStrings = new String[alarmList.size()];
            for (int i = 0; i < alarmList.size(); i++) {
                adapterStrings[i] = alarmList.get(i).getInfo();
            }
            ListAdapter adapter = new AdapterReminders(this, adapterStrings);
            alarmListView.setAdapter(adapter);
            final AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setView(dialogView).show();

            Button cancelButton = (Button) alertDialog.findViewById(R.id.cancelRemindersButton);
            Button deleteAllButton =
                    (Button) alertDialog.findViewById(R.id.deleteAllRemindersButton);
            deleteAllButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(HomeScreen.this)
                            .setTitle("Delete All Reminders?\nThis Can't Be Undone")
                            .setPositiveButton("Delete All Reminders", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    for (NotificationClass alarm : alarmList) {
                                        NotificationReceiver.cancelPendingIntent(
                                                alarm.id, HomeScreen.this, false);
                                    }
                                    dialog.dismiss();
                                    alertDialog.dismiss();
                                    showUpcomingReminders();
                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
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
                                    NotificationClass alarm = alarmList.get(position);
                                    NotificationReceiver.cancelPendingIntent(
                                            alarm.id, HomeScreen.this, true);
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
    protected void onDestroy() {
        super.onDestroy();

        mToast.cancel();
        billing.dispose();
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
        AdapterReminders(Context context, String[] adapterStrings) {
            super(context, R.layout.lv_reminders, adapterStrings);
        }

        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());

                convertView = inflater.inflate(R.layout.lv_reminders, parent, false);

                TextView nameTV = (TextView) convertView.findViewById(R.id.upcomingReminderItemTV);
                TextView timeTV = (TextView) convertView.findViewById(R.id.upcomingReminderTimeTV);


                String[] info = getItem(position).split("\\|");
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(Long.parseLong(info[1]));

                nameTV.setText(info[0]);
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm",
                        Locale.getDefault());
                String dateString = sdf.format(cal.getTime());
                timeTV.setText(dateString);
            }
            return convertView;
        }
    }
}
