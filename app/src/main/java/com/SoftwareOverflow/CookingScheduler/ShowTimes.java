package com.SoftwareOverflow.CookingScheduler;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.BaseAdapter;
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
    private InterstitialAd interstitialAd;
    private int currentItem = 0;
    private BaseAdapter adapter;
    private boolean[] isStageDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_times);
        if(HomeScreen.billing == null){
            HomeScreen.setupBilling(this);
        }

        //force portrait mode for phones
        if (getResources().getBoolean(R.bool.portrait_only))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //region --------------------- GETTING INTENT / SHARED PREFERENCE EXTRAS -------------------
        Bundle extras = getIntent().getExtras();
        SharedPreferences sharedPrefs = getSharedPreferences("foodItems", MODE_PRIVATE);

        currentItem = (extras.getString("origin", "").equals("ItemScreen")) ?
                0 : extras.getInt("currentItem", 0);

        readyTimeCal = (Calendar) extras.get("readyTimeCal");
        final String jsonString = sharedPrefs.getString("jsonString", "");
        if (jsonString.matches("")) {
            foodItemList = JsonHandler.getFoodItemList(this, extras.getString("jsonString"));
        } else {
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

        if (extras.getBoolean("reminders")) {
            NotificationReceiver nr = new NotificationReceiver();
            nr.setReminders(readyTimeCal, stageInfo, this);
        } else if (!extras.getBoolean("reminders", false) && extras.getString("origin", "").equals("ItemScreen")) {
            JsonHandler.putAlarmList(ShowTimes.this, new ArrayList<NotificationClass>(), false);
        }

        TextView readyTimeTV = (TextView) findViewById(R.id.readyTimeTV);
        readyTimeTV.setText(String.format(Locale.getDefault(), "%02d:%02d",
                readyTimeCal.get(Calendar.HOUR_OF_DAY), readyTimeCal.get(Calendar.MINUTE)));


        //endregion


        Button editMeal = (Button) findViewById(R.id.edit_meal);
        editMeal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final AlertDialog dlg = new AlertDialog.Builder(ShowTimes.this).create();
                dlg.setTitle(getString(R.string.edit_meal));
                dlg.setIcon(android.R.drawable.ic_dialog_alert);
                dlg.setMessage(getString(R.string.edit_meal_msg));
                dlg.setButton(DialogInterface.BUTTON_NEGATIVE,
                        getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dlg.cancel();
                            }
                        });
                dlg.setButton(DialogInterface.BUTTON_POSITIVE,
                        getString(R.string.cont), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                List<NotificationClass> mealReminders =
                                        JsonHandler.getAlarmList(ShowTimes.this, false);

                                if (!mealReminders.isEmpty()) {
                                    for (NotificationClass nf : mealReminders) {
                                        NotificationReceiver.cancelPendingIntent(nf.id, ShowTimes.this, false);
                                    }
                                }

                                Intent intent = new Intent(ShowTimes.this, ItemScreen.class)
                                        .putExtra("jsonString", JsonHandler.getFoodItemJsonString(foodItemList));
                                startActivity(intent);
                                finish();
                            }
                        });
                dlg.show();
            }
        });


        adapter = new AdapterShowTimes(this, stageInfo, stageInfo.length);
        showListView();


        final Handler handler = new Handler();
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (currentItem <= stageList.size()) {
                    isStageDone[currentItem] = true;
                    adapter.notifyDataSetChanged();
                    currentItem++;

                    if (currentItem < stageList.size()) {
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
        handler.postAtTime(r, getNextStageTime(stageList));


        //load advert
        final AdView adView = (AdView) findViewById(R.id.showTimesScreenBannerAd);
        if (!BillingClass.isUpgraded) {
            Handler adHandler = new Handler();
            adHandler.post(new Runnable() {
                @Override
                public void run() {
                    AdRequest adRequest = new AdRequest.Builder().
                            addTestDevice("2A0E7D2865A3C592033F3707402D0BBB").build();
                    adView.loadAd(adRequest);
                    interstitialAd = new InterstitialAd(ShowTimes.this);
                    interstitialAd.setAdUnitId(getResources().
                            getString(R.string.interstitial_ad_unit_id));
                    interstitialAd.loadAd(adRequest);
                }
            });
        } else adView.setVisibility(View.GONE);
    }


    private long getNextStageTime(List<FoodItem.Stage> stageList) {
        Calendar stageStartTime = Calendar.getInstance();
        stageStartTime.setTimeInMillis(readyTimeCal.getTimeInMillis());
        int stageTime = stageList.get(currentItem).getEffectiveTotalTime();
        stageStartTime.add(Calendar.MINUTE, -stageTime);

        long now = Calendar.getInstance().getTimeInMillis();
        long timeSinceStartup = SystemClock.uptimeMillis();
        long startupTime = now - timeSinceStartup;

        return stageStartTime.getTimeInMillis() - startupTime;
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
        if (BillingClass.isUpgraded) { //upgraded => allowed to save meals
            LayoutInflater inflater = LayoutInflater.from(this);
            View dialogView = inflater.inflate(R.layout.dialog_save_meal,
                    (ViewGroup) view.getRootView(), false);
            final AlertDialog saveMealDialog = new AlertDialog.Builder(this).setView(dialogView).show();

            final EditText nameET = (EditText) dialogView.findViewById(R.id.mealNameET);
            final EditText notesET = (EditText) dialogView.findViewById(R.id.mealNotesET);

            Button saveButton = (Button) dialogView.findViewById(R.id.saveMealButton);
            Button deleteButton = (Button) dialogView.findViewById(R.id.cancelSavingMealButton);
            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String name = nameET.getText().toString();
                    String notes = notesET.getText().toString();


                    if (notes.isEmpty()) notes = "No Notes Saved";
                    if (name.isEmpty()) {
                        Toast.makeText(ShowTimes.this,
                                "Please enter a meal name first", Toast.LENGTH_SHORT).show();
                    } else {
                        MealDatabase mealDB = new MealDatabase(ShowTimes.this, null);
                        String jsonString = JsonHandler.getFoodItemJsonString(foodItemList);
                        if (mealDB.addMeal(name, jsonString, notes, -1)) {
                            saveMealDialog.dismiss();
                        }
                    }
                }
            });
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveMealDialog.dismiss();
                }
            });
        } else Toast.makeText(this, "Please upgrade to unlock this feature",
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


    private class AdapterShowTimes extends BaseAdapter {
        int length;
        FoodItem.Stage[] stages;

        AdapterShowTimes(Context context, FoodItem.Stage[] stages, int length) {
            super();
            this.length = length;
            this.stages = stages;
        }

        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {

            ShowTimesViewHolder viewHolder;

            if (convertView == null) {
                //create a new viewHolder

                viewHolder = new ShowTimesViewHolder();

                LayoutInflater inflater = LayoutInflater.from(ShowTimes.this);
                convertView = inflater.inflate(R.layout.lv_show_times, parent, false);

                viewHolder.startTime = (TextView) convertView.findViewById(R.id.startTimeTV);
                viewHolder.startItem = (TextView) convertView.findViewById(R.id.startItemTV);


                convertView.setTag(viewHolder);
            } else {
                //instantiate the old view holder
                viewHolder = (ShowTimesViewHolder) convertView.getTag();
            }

            LinearLayout ll = (LinearLayout) convertView.findViewById(R.id.show_times_layout);
            if (isStageDone[position]) {
                ll.setBackgroundColor(getResources().getColor(R.color.dark_green));
                convertView.setBackgroundColor(getResources().getColor(R.color.dark_green));
            }
            else{
                ll.setBackgroundColor(getResources().getColor(R.color.light_grey));
                convertView.setBackgroundColor(getResources().getColor(R.color.light_grey));
            }

            FoodItem.Stage stage = (FoodItem.Stage) getItem(position);
            int effectiveTotalTime = stage.getEffectiveTotalTime();

            viewHolder.foodTime = Calendar.getInstance();
            viewHolder.foodTime.setTime(ShowTimes.getReadyTimeCal().getTime());
            viewHolder.foodTime.add(Calendar.MINUTE, -effectiveTotalTime);

            viewHolder.startItem.setText(String.format(Locale.getDefault(), "%s - %s \n(%s " +
                            getString(R.string.minutes) + ")", stage.getFoodItemName(),
                    stage.getName(), stage.getTime()));
            viewHolder.startTime.setText(String.format(Locale.getDefault(), "%02d:%02d",
                    viewHolder.foodTime.get(Calendar.HOUR_OF_DAY),
                    viewHolder.foodTime.get(Calendar.MINUTE)));


            return convertView;
        }

        @Override
        public int getCount() {
            return stages.length;
        }

        @Override
        public Object getItem(int i) {
            return stages[i];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        private class ShowTimesViewHolder {
            TextView startTime, startItem;
            Calendar foodTime;
        }
    }

}