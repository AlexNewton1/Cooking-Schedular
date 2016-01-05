package com.SoftwareOverflow.CookingScheduler;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class ItemScreen extends AppCompatActivity {

    private ArrayList<FoodItem> foodItemList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer_screen);

        //force portrait for phones
        if(getResources().getBoolean(R.bool.portrait_only)) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);

        final AdView adView = (AdView)findViewById(R.id.itemScreenBannerAd);
        final AdRequest adRequest = new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR) //adding emulator and phone as test devices
                .addTestDevice("2A0E7D2865A3C592033F3707402D0BBB").build();
        adView.loadAd(adRequest);


        if(getIntent().getBooleanExtra("loadValues", false)){ //load previous meal
            SharedPreferences preferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
            String[] foodArray = preferences.getString("foodListString", "").split("\\|\\|");
            if(foodArray[0].equals("")) Toast.makeText(this, R.string.no_previous_meal, Toast.LENGTH_SHORT).show();
            else{
                for (String aFoodArray : foodArray) { //foreach loop
                    Log.d("sp", aFoodArray);
                    String[] info = aFoodArray.split("\\|");
                    FoodItem item = new FoodItem(info[0], Integer.parseInt(info[1]));
                    foodItemList.add(item);
                }
            }
            showListView();
        }
    }

    /**
     * shows prompt for adding item (name & time)
     */
    public void showAddPrompt(final View v) {
        int layoutID = R.layout.add_item_dialog;
        View promptView = View.inflate(this, layoutID, null);
        final AlertDialog dialog = createDialog(promptView);

        final EditText nameET = (EditText) promptView.findViewById(R.id.newItemName);
        final EditText timeET = (EditText) promptView.findViewById(R.id.newItemTime);

        promptView.findViewById(R.id.addItemButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = nameET.getText().toString();
                String time = timeET.getText().toString();

                if (name.contains("|")) {
                    Toast.makeText(ItemScreen.this, R.string.name_no_pipe, Toast.LENGTH_SHORT).show();
                } else if (!name.replaceAll("\\s", "").isEmpty() && !time.isEmpty()) {
                    foodItemList.add(new FoodItem(name, Integer.parseInt(time)));
                    dialog.dismiss();
                    showListView();
                } else
                    Toast.makeText(ItemScreen.this, R.string.no_name_time, Toast.LENGTH_SHORT).show();
            }
        });
        promptView.findViewById(R.id.cancelAddingItemButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    private void showListView() {
        ListView foodListView = (ListView) findViewById(R.id.foodListView);
        List<String> stringList = new ArrayList<>();

        for(int i=0; i<foodItemList.size(); i++){
            stringList.add(foodItemList.get(i).getInfo());
        } //get string info for each FoodItem

        String[] adapterStrings = stringList.toArray(new String[stringList.size()]); //convert string to array
        ArrayAdapter adapter = new AddingItemAdapter(this, adapterStrings); //custom adapter
        foodListView.setAdapter(adapter);

        //region -----------------  ADDING ITEM LONG CLICK LISTENER TO LIST VIEW ITEMS  ------------------------------------
        foodListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                int layoutID = R.layout.update_dialog;
                View promptView = View.inflate(ItemScreen.this, layoutID, null);
                final AlertDialog dialog = createDialog(promptView);

                final EditText updateNameET = (EditText) promptView.findViewById(R.id.updateItemName);
                final EditText updateTimeET = (EditText) promptView.findViewById(R.id.updateItemTime);

                FoodItem updateItem = foodItemList.get(position);
                updateNameET.setText(updateItem.name);
                updateTimeET.setText("" + updateItem.time);

                promptView.findViewById(R.id.updateItemButton).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String name = updateNameET.getText().toString();
                        String time = updateTimeET.getText().toString();

                        if (!name.replaceAll("\\s", "").isEmpty() && !time.isEmpty()) {
                            foodItemList.remove(position);
                            foodItemList.add(position, new FoodItem(name, Integer.parseInt(time)));
                            dialog.dismiss();
                            showListView();
                        } else
                            Toast.makeText(ItemScreen.this, R.string.no_name_time, Toast.LENGTH_SHORT).show();
                    }
                });
                promptView.findViewById(R.id.cancelUpdateButton).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                promptView.findViewById(R.id.deleteItemButton).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        foodItemList.remove(position);
                        dialog.dismiss();
                        showListView();
                    }
                });

                return true;
            }
        });
        //endregion
    }

    public void selectReadyTime(View v){
        if(foodItemList.size() == 0) Toast.makeText(this, R.string.no_food_item_found, Toast.LENGTH_SHORT).show();
        else {
            int layoutID = R.layout.choose_ready_time_dialog;
            View promptView = View.inflate(this, layoutID, null);
            final AlertDialog dialog = createDialog(promptView);

            Collections.sort(foodItemList);
            //-----------   DECLARATIONS & INITIALISATIONS      --------------------------------------------------------------------------------
            final Calendar earliestReadyTime = earliestReadyTime();
            final Calendar readyTime = Calendar.getInstance();
            final RadioButton autoTime = (RadioButton) promptView.findViewById(R.id.ASAP_RB);
            RadioButton manualTime = (RadioButton) promptView.findViewById(R.id.manualTimeSelectRB);
            final TextView readyTimeTV = (TextView) promptView.findViewById(R.id.readyTimeTV);
            readyTimeTV.setText(String.format("%02d:%02d", earliestReadyTime.get(Calendar.HOUR_OF_DAY), earliestReadyTime.get(Calendar.MINUTE)));
            final TimePicker timePicker = (TimePicker) promptView.findViewById(R.id.readyTimePicker);
            timePicker.setIs24HourView(true);
            final DatePicker datePicker = (DatePicker) promptView.findViewById(R.id.readyDatePicker);
            datePicker.setMinDate(earliestReadyTime.get(Calendar.DATE));
            final ScrollView pickerSV = (ScrollView) promptView.findViewById(R.id.manualTimeSelectSV);
            final CheckBox notifications = (CheckBox) promptView.findViewById(R.id.notificationSwitch);
            Button getTimingsButton = (Button) promptView.findViewById(R.id.getTimingsButton);

            //region-----------------     SETTING CHANGE LISTENERS    --------------------------------------------------------------------------------
            autoTime.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        Calendar earliestTime = earliestReadyTime();
                        pickerSV.setVisibility(View.INVISIBLE);
                        readyTimeTV.setText(String.format("%02d:%02d", earliestTime.get(Calendar.HOUR_OF_DAY), earliestTime.get(Calendar.MINUTE)));
                    }
                }
            });

            manualTime.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) pickerSV.setVisibility(View.VISIBLE);
                }
            });

            timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
                @Override
                public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                    readyTimeTV.setText(String.format("%02d:%02d", hourOfDay, minute));
                }
            });

            getTimingsButton.setOnClickListener(new View.OnClickListener() {
                Toast toast = new Toast(ItemScreen.this);


                @Override
                @SuppressWarnings("deprecation")
                //Min API = 18, getCurrentHour/Min deprecated in API 23 --> using deprecated for API<23
                public void onClick(View v) {
                    if (autoTime.isChecked()) {
                        getTimings(earliestReadyTime(), notifications.isChecked());

                    } else {
                        readyTime.set(Calendar.YEAR, datePicker.getYear());
                        readyTime.set(Calendar.MONTH, datePicker.getMonth());
                        readyTime.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());

                        if (Build.VERSION.SDK_INT >= 23) {
                            readyTime.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                            readyTime.set(Calendar.MINUTE, timePicker.getMinute());
                        } else {
                            readyTime.set(Calendar.HOUR_OF_DAY, timePicker.getCurrentHour());
                            readyTime.set(Calendar.MINUTE, timePicker.getCurrentMinute());
                        }

                        if (readyTime.compareTo(earliestReadyTime) <= 0)
                            Toast.makeText(ItemScreen.this, R.string.food_not_ready_on_time, Toast.LENGTH_LONG).show();
                        else {
                            toast.cancel();
                            getTimings(readyTime, notifications.isChecked());
                        }
                    }
                dialog.dismiss();
                }
            });
            //endregion
        }
    }

    private AlertDialog createDialog(View view){
        AlertDialog.Builder selectTimePrompt = new AlertDialog.Builder(this);
        selectTimePrompt.setView(view);

        return selectTimePrompt.show();
    }

    /**
     * @return calender corresponding to the earliest possible ready time
     */
    private Calendar earliestReadyTime() {
        Collections.sort(foodItemList); //sort into descending order
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, foodItemList.get(0).time);
        return cal;
    }

    public void getTimings(Calendar readyTimeCal, boolean showReminders){

        String[] itemStrings = new String[foodItemList.size()];
        for(int i=0; i<foodItemList.size(); i++){
            itemStrings[i] = foodItemList.get(i).name + "|" + foodItemList.get(i).time; //pipe char delimited
        }

        Intent i = new Intent(this, ShowTimes.class);
        i.putExtra("itemArray", itemStrings);
        i.putExtra("readyTimeCal", readyTimeCal);
        i.putExtra("reminders", showReminders);
        startActivity(i);
    }

    @Override
    protected void onResume() {
        showListView();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if(foodItemList.size()> 0) {
            StringBuilder sb = new StringBuilder();

            //double pipe char delimited for different items
            for (int i = 0; i < foodItemList.size(); i++) sb.append(foodItemList.get(i).getInfo()).append("||");

            SharedPreferences sharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("foodListString", sb.toString());
            editor.apply();
        }
        super.onDestroy();
    }
}
