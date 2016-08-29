package com.SoftwareOverflow.CookingScheduler;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.SoftwareOverflow.CookingScheduler.util.BillingClass;
import com.SoftwareOverflow.CookingScheduler.util.IabHelper;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ItemScreen extends Activity implements View.OnClickListener {

    //TODO - sort parentViewGroup to avoid passing null when inflating layouts
    private ViewGroup parentViewGroup = null;
    private View addItemView;
    private AlertDialog addItemDialog, addStageDialog, saveMealDialog;
    //dialogs declared in this scope to enable closing in overridden onClick method
    private Toast mToast; //single toast to prevent multiple toasts causing user to have to wait.

    private static List<FoodItem> foodItemList = new ArrayList<>();
    private FoodItem tempFoodItem;

    private EditText stageName, stageTime, itemName;
    private Button finishAddingItemButton, finishAddingStageButton,
            cancelAddingItemButton, cancelAddingStageButton;

    private int updatingSavedMeal = -1; //value of the SQLite row to update (-1 if not updating).
    private String mealName, mealNotes;

    private IabHelper mHelper;


    @SuppressLint("ShowToast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_screen);


        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        //force portrait for phones
        if (getResources().getBoolean(R.bool.portrait_only)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }


        //region------------------------ INITIALIZING DIALOGS & BUTTONS ----------------------------
        LayoutInflater inflater = LayoutInflater.from(this);
        addItemView = inflater.inflate(R.layout.dialog_add_item, parentViewGroup);
        addItemDialog = new AlertDialog.Builder(this).setView(addItemView).create();
        View addStageView = inflater.inflate(R.layout.dialog_add_stage, (ViewGroup) addItemView.getParent());
        addStageDialog = new AlertDialog.Builder(this).setView(addStageView).create();

        itemName = (EditText) addItemView.findViewById(R.id.newItemName);
        stageName = (EditText) addStageView.findViewById(R.id.stageNameET);
        stageTime = (EditText) addStageView.findViewById(R.id.stageTimeET);

        Button addNewStageButton = (Button) addItemView.findViewById(R.id.addStageButton);
        addNewStageButton.setOnClickListener(this);
        cancelAddingItemButton = (Button) addItemView.findViewById(R.id.cancelAddingItemButton);
        finishAddingItemButton = (Button) addItemView.findViewById(R.id.addItemButton);
        finishAddingStageButton = (Button) addStageView.findViewById(R.id.finishAddingStage);
        cancelAddingStageButton = (Button) addStageView.findViewById(R.id.cancelAddStage);
        //endregion initializing


        //getting intent extras (only when loading saved meals)
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            //SQLite ID of loaded meal (-1 if creating new meal)
            updatingSavedMeal = extras.getInt("updatingMeal", -1);
            mealName = extras.getString("mealName", "");
            mealNotes = extras.getString("mealNotes", "");
            String jsonString = extras.getString("jsonString", "");
            foodItemList = JsonHandler.getFoodItemList(this, jsonString);
            String[] adapterStrings = new String[foodItemList.size()];
            for (int i = 0; i < foodItemList.size(); i++)
                adapterStrings[i] = foodItemList.get(i).getInfo();
            showItemsLV(adapterStrings);
        }

        String base64PublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAorDc/" +
                "54H1bPEWKCxT8Qh00lBaacIvXIRUX8K+EAfVaNUpzLNlbbziKbKUAumrasQF+iIff2" +
                "oslvupLLPCZcG8k4v9ujfPs1g9CGKc8bTYQ47yBI1hIYq6GEoGffpHe+xA0+bQ2ujn" +
                "rT9g+3E6Dc0TaqfH+O0shw3zwgnh9nRWbb/ebMevgU4h7/tcjx8Omx7S13KxHvnwFJ" +
                "5lWg8RJPvTKYBVDkUyhfqvYKikux7V5UZmK9fCR4kea/ULwzRf+AsbG7YgVXjQQIMH" +
                "GmpxWWs1OioXeVR8TbYdRa0aMmp8aUHlnHMhxhlZCUotCrYfjftPhLImm88TPb14tW" +
                "/nLj5EQIDAQAB";
        List<String> keyList;


        //load ad
        if(!BillingClass.isUpgraded) {
            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    final AdView adView = (AdView) findViewById(R.id.itemScreenBannerAd);
                    //adding emulator and phone as test devices
                    AdRequest adRequest = new AdRequest.Builder().build();
                    adView.loadAd(adRequest);
                    adView.loadAd(adRequest);
                }
            });
        }
    }

    public void addItem(View view) {
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
                } else {
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
        cancelAddingItemButton.setOnClickListener(this);
    }

    private void addStage() {
        finishAddingStageButton.setOnClickListener(this);
        cancelAddingStageButton.setOnClickListener(this);

        //set EditTexts to empty
        stageName.setText("");
        stageTime.setText("");

        addStageDialog.show();
    }

    private void showStagesLV(final FoodItem foodItem) {
        List<String> foodStagesList = foodItem.getFoodStages();

        ListView stagesLV = (ListView) addItemView.findViewById(R.id.stagesListView);
        stagesLV.getLayoutParams().height = HomeScreen.screenHeight;

        if (foodStagesList != null && foodStagesList.size() != 0) {
            ListAdapter adapter = new AdapterItemStages(new ArrayList<>(foodStagesList));
            stagesLV.setAdapter(adapter);
        } else stagesLV.setAdapter(null);

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
                                .setTitle("Delete Stage '" + stages[position].split("\\|")[0]
                                        + "'?\nThis Cannot be Undone")
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

    private void showItemsLV(String[] adapterStrings) {
        ListAdapter adapter = new AdapterNewItem(this, adapterStrings);
        ListView itemsLV = (ListView) findViewById(R.id.mealItemListView);
        itemsLV.setAdapter(adapter);

        itemsLV.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                addItemDialog.show();

                final FoodItem foodItem = foodItemList.get(position);
                tempFoodItem = foodItem;
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
                delete.setText(R.string.delete);
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
    public void selectReadyTime(View v) {
        if (foodItemList.isEmpty()) {
            mToast.setText("Please add at least 1 item first");
            mToast.show();
        } else {
            View dialogView = View.inflate(this, R.layout.dialog_ready_time, parentViewGroup);
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

            readyTimeTV.setText(String.format(Locale.getDefault()," %02d:%02d",
                    earliestReadyTime().get(Calendar.HOUR_OF_DAY),
                    earliestReadyTime().get(Calendar.MINUTE)));
            earliestSelect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        Calendar cal = earliestReadyTime();
                        readyTimeTV.setText(String.format(Locale.getDefault(), "%02d:%02d",
                                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)));
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
                            //setCurrentHour() & setCurrentMinute() (deprecated in API 23)
                            timePicker.setCurrentHour(cal.get(Calendar.HOUR_OF_DAY));
                            timePicker.setCurrentMinute(cal.get(Calendar.MINUTE));
                        }
                        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
                            @Override
                            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                                readyTimeTV.setText(String.format(Locale.getDefault(),
                                        " %02d:%02d", hourOfDay, minute));
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
                    if (earliestSelect.isChecked()) {
                        chosenTime.setTimeInMillis(earliestReadyTime().getTimeInMillis());
                    } else {
                        if (Build.VERSION.SDK_INT >= 23) {
                            chosenTime.set(datePicker.getYear(), datePicker.getMonth(),
                                    datePicker.getDayOfMonth(), timePicker.getHour(),
                                    timePicker.getMinute());
                        } else {
                            //USING DEPRECATED METHODS FOR GETTING TIME FROM TIME PICKER
                            //getCurrentHour() & getCurrentMinute() deprecated in API 23
                            chosenTime.set(datePicker.getYear(), datePicker.getMonth(),
                                    datePicker.getDayOfMonth(), timePicker.getCurrentHour(),
                                    timePicker.getCurrentMinute());
                        }
                    }
                    if (chosenTime.getTimeInMillis() >= earliestTime.getTimeInMillis()) {
                        String jsonString = JsonHandler.getFoodItemJsonString(foodItemList);
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

    public static Calendar earliestReadyTime() {
        Collections.sort(foodItemList);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, foodItemList.get(0).totalTime);
        return cal;
    }

    public void saveMeal(View v) {
        if(BillingClass.isUpgraded) { //upgraded => allowed to save meals
            if (foodItemList.isEmpty()) {
                mToast.setText("Please add at least 1 item first");
                mToast.show();
            } else {
                LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
                View dialogView = inflater.inflate(R.layout.dialog_save_meal, parentViewGroup);
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

                        if (notes.isEmpty()) notes = "No Notes Saved";
                        if (name.isEmpty()) {
                            mToast.setText("Please enter a meal name first");
                            mToast.show();
                        } else {
                            MealDatabase mealDB = new MealDatabase(ItemScreen.this, null);
                            String jsonString = JsonHandler.getFoodItemJsonString(foodItemList);
                            if (mealDB.addMeal(name, jsonString, notes, updatingSavedMeal)) {
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
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancelAddingItemButton:
                addItemDialog.dismiss();
                break;
            case R.id.addStageButton:
                addStage();
                break;
            case R.id.finishAddingStage:
                String name = stageName.getText().toString();
                String time = stageTime.getText().toString();
                if (name.isEmpty() || time.isEmpty()) {
                    mToast.setText("Please enter a name and a time");
                    mToast.show();
                } else if (name.contains("|")) {
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
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.lv_food_item, parentViewGroup);
            }

            String[] info = getItem(position).split("\\|");
            TextView name = (TextView) convertView.findViewById(R.id.itemNameTV);
            TextView stages = (TextView) convertView.findViewById(R.id.itemStagesTV);
            TextView time = (TextView) convertView.findViewById(R.id.itemTimeTV);
            name.setText(info[0]);
            stages.setText(info[2]);
            time.setText(String.format("%s mins", info[1]));

            return convertView;
        }
    }


    private class AdapterItemStages extends BaseAdapter {

        private int startPos, currentPos;
        private ArrayList<String> data, tempData;
        private long lastTouchTime = 0;
        private int lastTouchY;
        private final int MAX_TOUCH_DISTANCE = 100;
        private final long MAX_TOUCH_DELAY = 1000;


        public AdapterItemStages(ArrayList<String> items) {
            data = items;
            tempData = new ArrayList<>(data);
        }

        @Override
        @SuppressWarnings("deprecation")
        public View getView(final int position, View convertView, final ViewGroup parent) {
            ViewHolder holder;

            LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.lv_item_stage, parent, false);

                holder = new ViewHolder();
                holder.stageName = (TextView) convertView.findViewById(R.id.stageName);
                holder.stageTime = (TextView) convertView.findViewById(R.id.stageTime);
                holder.stageNumber = (TextView) convertView.findViewById(R.id.stageNumberTV);

                convertView.setTag(holder);
            }
            else{
                holder = (ViewHolder) convertView.getTag();
            }

            String s = (String) getItem(position);
            String[] info = s.split("\\|");
            holder.stageName.setText(info[0]);
            holder.stageTime.setText(info[1]);
            holder.stageNumber.setText(String.format(Locale.getDefault(), "%1d", position + 1));

            convertView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        //only initiate drag on double click
                        long thisTouchTime = System.currentTimeMillis();
                        int thisTouchY = (int) motionEvent.getRawY();
                        boolean isDoubleClick = (thisTouchTime < lastTouchTime + MAX_TOUCH_DELAY) &&
                                (Math.abs(thisTouchY - lastTouchY) <= MAX_TOUCH_DISTANCE);
                        Log.d("listView", "deltaY: " + Math.abs(thisTouchY - lastTouchY));
                        if(!isDoubleClick){
                            lastTouchTime = thisTouchTime;
                            lastTouchY = thisTouchY;
                            return false;
                        }
                        else { //start drag
                            startPos = position;
                            currentPos = position;

                            ClipData clipData = ClipData.newPlainText("", "");
                            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                            //USING DEPRECATED METHOD FOR API < 24
                            //startDrag() (deprecated in API 24) --> startDragAndDrop()
                            //startDrag() (deprecated in API 24) --> startDragAndDrop()
                            if(Build.VERSION.SDK_INT < 24){
                                view.startDrag(clipData, shadowBuilder, view, 0);
                            }
                            else{
                                view.startDragAndDrop(clipData, shadowBuilder, view, 0);
                            }
                            view.setVisibility(View.GONE);

                            return true;
                        }
                    } else {
                        return false;
                    }
                }
            });

            convertView.setOnDragListener(new View.OnDragListener() {
                @Override
                public boolean onDrag(final View v, DragEvent event) {
                    switch (event.getAction()) {
                        case DragEvent.ACTION_DRAG_ENTERED:

                            if(position > startPos){
                                //view dragged downwards
                                ((LinearLayout) v).setGravity(Gravity.TOP);
                            }
                            else{
                                //view dragged upwards
                                ((LinearLayout ) v).setGravity(Gravity.BOTTOM);
                            }

                            moveData(currentPos, position);
                            currentPos = position;
                            v.getLayoutParams().height = v.getHeight()*2;

                            break;
                        case DragEvent.ACTION_DRAG_EXITED:
                            //go straight into ACTION_DROP case (resize view to original size)
                        case DragEvent.ACTION_DROP:
                            v.setLayoutParams(new AbsListView.LayoutParams(v.getWidth(), v.getHeight()/2));
                            break;
                        case DragEvent.ACTION_DRAG_ENDED:
                            //update list view if drop valid (inside list view)
                            if (event.getResult()){
                                updateListView();
                            }
                            else{ //drop invalid - reset tempData back to original data
                                tempData = new ArrayList<>(data);
                            }

                            final View droppedView = (View) event.getLocalState();
                            droppedView.post(new Runnable() {
                                @Override
                                public void run() {
                                    droppedView.setVisibility(View.VISIBLE);
                                }
                            });
                            break;
                        default:
                            break;
                    }
                    return true;
                }
            });
            return convertView;
        }

        private void moveData(int from, int to){
            String temp = tempData.get(from);
            tempData.remove(from);
            tempData.add(to, temp);
        }

        private void updateListView(){
            data.clear();
            data.addAll(tempData);

            tempFoodItem.setFoodStages(data);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int i) {
            return data.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

    }

    /**
     * Class to hold views used in custom list view to increase scrolling smoothness and aid view recycling
     */
    static class ViewHolder{
        TextView stageName, stageTime, stageNumber;
    }

}