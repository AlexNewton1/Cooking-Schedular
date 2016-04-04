package com.SoftwareOverflow.CookingScheduler;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class ItemScreen extends Activity implements View.OnClickListener {

    private static List<FoodItem> foodItemList = new ArrayList<>();
    private FoodItem tempFoodItem;
    private EditText stageName, stageTime, itemName;
    private Button finishAddingItemButton, addNewStageButton, finishAddingStageButton, cancelAddingItemButton, cancelAddingStageButton;
    private View addItemView;
    private AlertDialog addItemDialog, addStageDialog, saveMealDialog; //dialogs declared in this scope to enable closing in overridden onClick method
    private Toast mToast; //single toast to prevent multiple toasts causing user to have to wait.
    private int updatingSavedMeal = -1; //holds the value of the SQLite row to update (-1 if not updating).
    private String mealName, mealNotes;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_screen);
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        //force portrait for phones
        if(getResources().getBoolean(R.bool.portrait_only)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }


        //region------------------------ INITIALIZING DIALOGS & BUTTONS ---------------------------------------------------------------------
        LayoutInflater inflater = LayoutInflater.from(this);
        addItemView = inflater.inflate(R.layout.dialog_add_item, null);
        addItemDialog = new AlertDialog.Builder(this).setView(addItemView).create();
        View addStageView = inflater.inflate(R.layout.dialog_add_stage, null);
        addStageDialog = new AlertDialog.Builder(this).setView(addStageView).create();

        itemName = (EditText) addItemView.findViewById(R.id.newItemName);
        stageName = (EditText) addStageView.findViewById(R.id.stageNameET);
        stageTime = (EditText) addStageView.findViewById(R.id.stageTimeET);

        addNewStageButton = (Button) addItemView.findViewById(R.id.addStageButton);
        cancelAddingItemButton = (Button) addItemView.findViewById(R.id.cancelAddingItemButton);
        finishAddingItemButton = (Button) addItemView.findViewById(R.id.addItemButton);
        finishAddingStageButton = (Button) addStageView.findViewById(R.id.finishAddingStage);
        cancelAddingStageButton = (Button) addStageView.findViewById(R.id.cancelAddStage);
        //endregion initializing dialogs & buttons

        //getting intent extras (only used when loading a saved meal)
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            updatingSavedMeal = extras.getInt("updatingMeal", -1); //value of SQLite row ID of meal to be updated (-1 if creating new meal)
            mealName = extras.getString("mealName", "");
            mealNotes = extras.getString("mealNotes", "");
            String jsonString = extras.getString("jsonString", "");
            foodItemList = JsonHandler.getFoodItemList(this, jsonString);
            String[] adapterStrings = new String[foodItemList.size()];
            for (int i = 0; i < foodItemList.size(); i++)
                adapterStrings[i] = foodItemList.get(i).getInfo();
            showItemsLV(adapterStrings);
        }

        /*
        //load ad in thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                final AdView adView = (AdView)findViewById(R.id.itemScreenBannerAd);
                AdRequest adRequest = new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR) //adding emulator and phone as test devices
                        .addTestDevice("2A0E7D2865A3C592033F3707402D0BBB").build();
                adView.loadAd(adRequest);
            }
        }).start();
        */
    }

    public void addItem(View view){
        tempFoodItem = new FoodItem("");
        showStagesLV(tempFoodItem);
        itemName.setText("");
        addItemDialog.show();


        finishAddingItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tempFoodItem.numStages == 0 || itemName.getText().toString().isEmpty()) {
                    mToast.setText("Please enter a name and add at least 1 stage");
                    mToast.show();
                }
                else {
                    FoodItem foodItem = new FoodItem(itemName.getText().toString());
                    foodItem.setFoodStages(tempFoodItem.getFoodStages());
                    foodItem.totalTime = tempFoodItem.totalTime;
                    foodItem.numStages = tempFoodItem.numStages;
                    foodItemList.add(foodItem);
                    String[] adapterStrings = new String[foodItemList.size()];
                    for (int i = 0; i < foodItemList.size(); i++) {
                        adapterStrings[i] = foodItemList.get(i).getInfo();
                    }
                    showItemsLV(adapterStrings);
                    addItemDialog.dismiss();
                }
            }
        });
        addNewStageButton.setOnClickListener(this);
        cancelAddingItemButton.setOnClickListener(this);
    }

    public void addStage(){
        finishAddingStageButton.setOnClickListener(this);
        cancelAddingStageButton.setOnClickListener(this);

        //set EditTexts to empty
        stageName.setText("");
        stageTime.setText("");

        addStageDialog.show();
    }

    public void showStagesLV(final FoodItem foodItem){
        ListView stagesLV = (ListView) addItemView.findViewById(R.id.stagesListView);

        if(foodItem.getFoodStages() != null && foodItem.getFoodStages().size()!=0) {
            String[] foodStages = new String[foodItem.getFoodStages().size()];
            for(int i=0; i<foodStages.length; i++){
                foodStages[i] = foodItem.getFoodStages().get(i);
            }
            ListAdapter adapter = new AdapterItemStages(this, foodStages);
            stagesLV.setAdapter(adapter);
        }
        else stagesLV.setAdapter(null);

        stagesLV.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                addStageDialog.show();
                List<String> stageList = foodItem.getFoodStages();
                final String[] stages = stageList.toArray(new String[stageList.size()]);

                stageName.setText(stages[position].split("\\|")[0]);
                stageTime.setText(stages[position].split("\\|")[1]);

                tempFoodItem = foodItem;

                Button deleteButton = (Button) addStageDialog.findViewById(R.id.deleteStage);
                cancelAddingStageButton.setOnClickListener(ItemScreen.this);
                finishAddingStageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String name = stageName.getText().toString();
                        String time = stageTime.getText().toString();
                        if (name.isEmpty() || time.isEmpty()) {
                            mToast.setText("Please enter a name and a time");
                            mToast.show();
                        } else if (name.contains("|")) {
                            mToast.setText("Name can't contain pipe character '|'");
                            mToast.show();
                        } else {
                            foodItem.updateStage(name + "|" + time, position);
                            addStageDialog.dismiss();
                            showStagesLV(foodItem);
                        }
                    }
                });
                deleteButton.setVisibility(View.VISIBLE);
                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(ItemScreen.this)
                                .setTitle("Delete Stage '" + stages[position].split("\\|")[0] + "'?\nThis Cannot be Undone")
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .setPositiveButton("Delete Stage", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        foodItem.removeStage(position);
                                        addStageDialog.dismiss();
                                        showStagesLV(foodItem);
                                    }
                                })
                                .show();
                    }
                });
                return true;
            }
        });
    }

    public void showItemsLV(String[] adapterStrings){
        ListAdapter adapter = new AdapterNewItem(this, adapterStrings);
        ListView itemsLV = (ListView) findViewById(R.id.mealItemListView);
        itemsLV.setAdapter(adapter);


        itemsLV.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                addItemDialog.show();

                final FoodItem foodItem = foodItemList.get(position);
                itemName.setText(foodItem.name);
                showStagesLV(foodItem);

                finishAddingItemButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (tempFoodItem.numStages == 0 || itemName.getText().toString().isEmpty()) {
                            mToast.setText("Please enter a name and add at least 1 stage");
                            mToast.show();
                        } else {
                            foodItem.setFoodStages(tempFoodItem.getFoodStages());
                            foodItem.name = itemName.getText().toString();
                            foodItem.totalTime = tempFoodItem.totalTime;
                            foodItem.numStages = tempFoodItem.numStages;
                            String[] adapterStrings = new String[foodItemList.size()];
                            for (int i = 0; i < foodItemList.size(); i++) {
                                adapterStrings[i] = foodItemList.get(i).getInfo();
                            }
                            showItemsLV(adapterStrings);
                            addItemDialog.dismiss();
                        }
                    }
                });

                Button delete = (Button) addItemDialog.findViewById(R.id.cancelAddingItemButton);
                delete.setText("Delete Item");
                delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(ItemScreen.this)
                                .setTitle("Delete Item '" + foodItem.name + "'?\nThis Cannot be Undone")
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .setPositiveButton("Delete Item", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        foodItemList.remove(position);
                                        String[] adapterStrings = new String[foodItemList.size()];
                                        for (int i = 0; i < foodItemList.size(); i++) {
                                            adapterStrings[i] = foodItemList.get(i).getInfo();
                                        }
                                        showItemsLV(adapterStrings);
                                        addItemDialog.dismiss();
                                    }
                                })
                                .show();
                    }
                });
                return true;
            }
        });
    }

    @SuppressWarnings("deprecation")
    public void selectReadyTime(View v){
        if(foodItemList.isEmpty()){
            mToast.setText("Please add at least 1 item first");
            mToast.show();
        }
        else {
            View dialogView = View.inflate(this, R.layout.dialog_ready_time, null);
            final AlertDialog alertDialog = new AlertDialog.Builder(this).setView(dialogView).show();

            final CheckBox reminderCheckBox = (CheckBox) dialogView.findViewById(R.id.notificationSwitch);
            final TextView readyTimeTV = (TextView) dialogView.findViewById(R.id.readyTimeTV);
            final ScrollView manualSelectSV = (ScrollView) dialogView.findViewById(R.id.manualTimeSelectSV);
            final TimePicker timePicker = (TimePicker) dialogView.findViewById(R.id.readyTimePicker);
            final DatePicker datePicker = (DatePicker) dialogView.findViewById(R.id.readyDatePicker);
            timePicker.setIs24HourView(true);
            Button getTimingsButton = (Button) dialogView.findViewById(R.id.getTimingsButton);
            RadioButton manualSelect = (RadioButton) dialogView.findViewById(R.id.manualTimeSelectRB);
            final RadioButton earliestSelect = (RadioButton) dialogView.findViewById(R.id.ASAP_RB);

            readyTimeTV.setText(String.format(" %02d:%02d", earliestReadyTime().get(Calendar.HOUR_OF_DAY), earliestReadyTime().get(Calendar.MINUTE)));
            earliestSelect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        Calendar cal = earliestReadyTime();
                        readyTimeTV.setText(String.format(" %02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)));
                    }
                }
            });
            manualSelect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        manualSelectSV.setVisibility(View.VISIBLE);
                        Calendar cal = earliestReadyTime();
                        if (Build.VERSION.SDK_INT >= 23) {
                            timePicker.setHour(cal.get(Calendar.HOUR_OF_DAY));
                            timePicker.setMinute(cal.get(Calendar.MINUTE));
                        } else {
                            //USING DEPRECATED METHODS FOR API < 23
                            //(setCurrentHour() & setCurrentMinute() deprecated in API 23)
                            timePicker.setCurrentHour(cal.get(Calendar.HOUR_OF_DAY));
                            timePicker.setCurrentMinute(cal.get(Calendar.MINUTE));
                        }
                        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
                            @Override
                            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                                readyTimeTV.setText(String.format(" %02d:%02d",hourOfDay, minute));
                            }
                        });

                    } else manualSelectSV.setVisibility(View.INVISIBLE);
                }
            });

            getTimingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Calendar earliestTime = earliestReadyTime();
                    Calendar chosenTime = Calendar.getInstance();
                    if(earliestSelect.isChecked()){
                        chosenTime.setTimeInMillis(earliestReadyTime().getTimeInMillis());
                    }
                    else {
                        if (Build.VERSION.SDK_INT >= 23) {
                            chosenTime.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(),
                                    timePicker.getHour(), timePicker.getMinute());
                        } else {
                            //USING DEPRECATED METHODS FOR GETTING TIME FROM TIME PICKER
                            //getCurrentHour() & getCurrentMinute() deprecated in API 23
                            chosenTime.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(),
                                    timePicker.getCurrentHour(), timePicker.getCurrentMinute());
                        }
                    }
                    if (chosenTime.getTimeInMillis() >= earliestTime.getTimeInMillis()) {


                        String jsonString = JsonHandler.getFoodItemJsonString(getApplicationContext(), foodItemList);
                        startActivity(new Intent(ItemScreen.this, ShowTimes.class)
                                .putExtra("jsonString", jsonString)
                                .putExtra("readyTimeCal", chosenTime)
                                .putExtra("reminders", reminderCheckBox.isChecked()));
                        alertDialog.dismiss();

                    } else {
                        mToast.setText("Food cannot be ready at that time" +
                                "\nPlease select a different time/date");
                        mToast.show();
                    }
                }
            });
        }
    }

    public static Calendar earliestReadyTime(){
        Collections.sort(foodItemList);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, foodItemList.get(0).totalTime);
        return cal;
    }

    public void saveMeal(View v){
        //TODO -- check if free/upgraded
        if(foodItemList.isEmpty()) {
            mToast.setText("Please add at least 1 item first");
            mToast.show();
        }
        else{
            LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
            View dialogView = inflater.inflate(R.layout.dialog_save_meal, null);
            saveMealDialog = new AlertDialog.Builder(this).setView(dialogView).show();

            final EditText nameET = (EditText) dialogView.findViewById(R.id.mealNameET);
            final EditText notesET = (EditText) dialogView.findViewById(R.id.mealNotesET);
            nameET.setText(mealName);
            notesET.setText(mealNotes);

            Button saveButton = (Button) dialogView.findViewById(R.id.saveMealButton);
            Button deleteButton = (Button) dialogView.findViewById(R.id.cancelSavingMealButton);
            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String name = nameET.getText().toString();
                    String notes = notesET.getText().toString();

                    if(notes.isEmpty()) notes = "No Notes Saved";
                    if(name.isEmpty()) {
                        mToast.setText("Please enter a meal name first");
                        mToast.show();
                    }
                    else{
                        MealDatabase mealDB = new MealDatabase(ItemScreen.this, null);
                        String jsonString = JsonHandler.getFoodItemJsonString(ItemScreen.this, foodItemList);
                        if(mealDB.addMeal(name, jsonString, notes, updatingSavedMeal)){
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
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.cancelAddingItemButton:
                addItemDialog.dismiss();
                break;
            case R.id.addStageButton:
                addStage();
                break;
            case R.id.finishAddingStage:
                String name = stageName.getText().toString();
                String time = stageTime.getText().toString();
                if(name.isEmpty() || time.isEmpty()){
                    mToast.setText("Please enter a name and a time");
                    mToast.show();
                }
                else if(name.contains("|")){
                    mToast.setText("Name can't contain pipe character '|'");
                    mToast.show();
                } else {
                    tempFoodItem.addStage(name, time);
                    showStagesLV(tempFoodItem);
                    addStageDialog.dismiss();
                }
                break;
            case R.id.cancelAddStage:
                addStageDialog.dismiss();
                break;
            case R.id.cancelSavingMealButton:
                saveMealDialog.dismiss();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        foodItemList.clear();
        super.onDestroy();
    }




    private class AdapterNewItem extends ArrayAdapter<String> {

        public AdapterNewItem(Context context, String[] items) {
            super(context, R.layout.lv_food_item, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            if(convertView == null) {
                convertView = inflater.inflate(R.layout.lv_food_item, null);
            }

            String[] info = getItem(position).split("\\|");
            TextView name = (TextView) convertView.findViewById(R.id.itemNameTV);
            TextView stages = (TextView) convertView.findViewById(R.id.itemStagesTV);
            TextView time = (TextView) convertView.findViewById(R.id.itemTimeTV);
            name.setText(info[0]);
            stages.setText(info[2]);
            time.setText(info[1]  + " mins");

            return convertView;
        }
    }


    private class AdapterItemStages extends ArrayAdapter<String> {

        public AdapterItemStages(Context context, String[] items) {
            super(context, R.layout.lv_item_stage, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = LayoutInflater.from(getContext());
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.lv_item_stage, null);
            }

            TextView stageName = (TextView) convertView.findViewById(R.id.stageName);
            TextView stageTime = (TextView) convertView.findViewById(R.id.stageTime);

            String[] info = getItem(position).split("\\|");

            if (info.length>1) {
                stageName.setText(info[0]);
                stageTime.setText(info[1]);
            }

            return convertView;
        }
    }
}