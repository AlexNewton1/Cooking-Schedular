package com.SoftwareOverflow.CookingScheduler;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
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
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.SoftwareOverflow.CookingScheduler.R.string.delete;

public class ItemScreen extends Activity implements View.OnClickListener {

    private ViewGroup parentViewGroup = null;
    private LayoutInflater inflater;
    private View addItemView;
    //dialogs declared in this scope to enable closing in overridden onClick method
    private AlertDialog addItemDialog, addStageDialog, saveMealDialog;

    private Toast mToast; //single toast to prevent multiple toasts using system resources.

    private static List<FoodItem> foodItemList = new ArrayList<>();
    private FoodItem tempFoodItem;

    private EditText stageName, stageTime, itemName;
    private Button finishAddingItemButton, finishAddingStageButton,
            cancelAddingItemButton, cancelAddingStageButton;

    private int updatingSavedMeal = -1; //value of the SQLite row to update (-1 if not updating).
    private String mealName, mealNotes;


    @SuppressLint("ShowToast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_screen);

        parentViewGroup = (ViewGroup) findViewById(R.id.activity_item_screen);

        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        //force portrait for phones
        if (getResources().getBoolean(R.bool.portrait_only)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }


        //region------------------------ INITIALIZING DIALOGS & BUTTONS ----------------------------
        inflater = LayoutInflater.from(this);
        addItemView = inflater.inflate(R.layout.dialog_add_item, parentViewGroup, false);
        addItemDialog = new AlertDialog.Builder(this).setView(addItemView).create();
        View addStageView = inflater.inflate(R.layout.dialog_add_stage,
                (ViewGroup) addItemView.getParent(), false);
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
            //SQLite row ID of loaded meal (-1 if creating new meal)
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

        //load ad
        final AdView adView = (AdView) findViewById(R.id.itemScreenBannerAd);
        if(!BillingClass.isUpgraded){
            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    AdRequest adRequest = new AdRequest.Builder()
                            .addTestDevice("2A0E7D2865A3C592033F3707402D0BBB").build();
                    adView.loadAd(adRequest);
                }
            });
        }
        else adView.setVisibility(View.GONE);
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
                    foodItem.setStages(tempFoodItem.getStages());
                    foodItem.totalTime = tempFoodItem.totalTime;
                    foodItem.numStages = tempFoodItem.numStages;
                    foodItemList.add(foodItem);
                    String[] adapterStrings = new String[foodItemList.size()];
                    for (int i = 0; i < foodItemList.size(); i++) {
                        adapterStrings[i] = foodItemList.get(i).getInfo();
                    }
                    foodItem.addNameToStages();
                    showItemsLV(adapterStrings);
                    addItemDialog.dismiss();
                }
            }
        });
        cancelAddingItemButton.setOnClickListener(this);
    }

    private void addStage() {
        addStageDialog.show();

        Button deleteButton = (Button) addStageDialog.findViewById(R.id.deleteStage);
        deleteButton.setVisibility(View.GONE);

        //set EditTexts to empty
        stageName.setText("");
        stageTime.setText("");

        finishAddingStageButton.setOnClickListener(this);
        cancelAddingStageButton.setOnClickListener(this);
    }

    private void showStagesLV(final FoodItem foodItem) {
        List<FoodItem.Stage> stagesList = foodItem.getStages();

        ListView stagesLV = (ListView) addItemView.findViewById(R.id.stagesListView);
        stagesLV.getLayoutParams().height = HomeScreen.screenHeight;

        if (stagesList != null && stagesList.size() != 0) {
            ListAdapter adapter = new AdapterItemStages(new ArrayList<>(stagesList));
            stagesLV.setAdapter(adapter);
        } else stagesLV.setAdapter(null);

        stagesLV.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                addStageDialog.show();
                final List<FoodItem.Stage> stageList = foodItem.getStages();

                stageName.setText(stageList.get(position).getName());
                stageTime.setText(String.format(Locale.getDefault(),
                        "%2d", stageList.get(position).getTime()));

                tempFoodItem = foodItem;

                Button deleteButton = (Button) addStageDialog.findViewById(R.id.deleteStage);
                cancelAddingStageButton.setOnClickListener(ItemScreen.this);
                finishAddingStageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String name = stageName.getText().toString();
                        String time = stageTime.getText().toString().trim();
                        if (name.isEmpty() || time.isEmpty()) {
                            mToast.setText("Please enter a name and a time");
                            mToast.show();
                        } else if (name.contains("|")) {
                            mToast.setText("Name can't contain pipe character '|'");
                            mToast.show();
                        } else {
                            foodItem.updateStage(name, Integer.parseInt(time), position);
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
                                .setTitle("Delete Stage '" + stageList.get(position).getName()
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
                            foodItem.setStages(tempFoodItem.getStages());
                            foodItem.totalTime = tempFoodItem.totalTime;
                            foodItem.numStages = tempFoodItem.numStages;
                            foodItemList.remove(position); //remove old
                            foodItemList.add(foodItem); //add updated
                            foodItem.name = itemName.getText().toString();
                            foodItem.addNameToStages();

                            String[] adapterStrings = new String[foodItemList.size()];
                            for (int i = 0; i < foodItemList.size(); i++) {
                                adapterStrings[i] = foodItemList.get(i).getInfo();
                            }
                            showItemsLV(adapterStrings);
                            addItemDialog.dismiss();
                        }
                    }
                });

                Button deleteButton = (Button) addItemDialog.findViewById(R.id.cancelAddingItemButton);
                deleteButton.setText(getString(delete));
                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(ItemScreen.this)
                                .setTitle("Delete Item '" + foodItem.name +
                                        "'?\nThis Cannot be Undone")
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
            View dialogView = inflater.inflate(R.layout.dialog_ready_time, parentViewGroup, false);
            if (dialogView.getParent() != null) {
                ((ViewGroup) dialogView.getParent()).removeView(dialogView);
            }
            final AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setView(dialogView).show();
            final CheckBox reminderCheckBox =
                    (CheckBox) dialogView.findViewById(R.id.notificationSwitch);
            final TextView readyTimeTV = (TextView) dialogView.findViewById(R.id.readyTimeTV);
            final ScrollView manualSelectSV = (
                    ScrollView) dialogView.findViewById(R.id.manualTimeSelectSV);
            final TimePicker timePicker =
                    (TimePicker) dialogView.findViewById(R.id.readyTimePicker);
            final DatePicker datePicker =
                    (DatePicker) dialogView.findViewById(R.id.readyDatePicker);
            timePicker.setIs24HourView(true);
            Button getTimingsButton = (Button) dialogView.findViewById(R.id.getTimingsButton);
            RadioButton manualSelect =
                    (RadioButton) dialogView.findViewById(R.id.manualTimeSelectRB);
            final RadioButton earliestSelect = (RadioButton) dialogView.findViewById(R.id.ASAP_RB);

            final SimpleDateFormat sdf = new SimpleDateFormat(" HH:mm", Locale.getDefault());
            readyTimeTV.setText(sdf.format(earliestReadyTime().getTime()));
            earliestSelect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        readyTimeTV.setText(sdf.format(earliestReadyTime().getTime()));
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

                        setEffectiveTotalTimes();

                        String jsonString = JsonHandler.getFoodItemJsonString(foodItemList);
                        startActivity(new Intent(ItemScreen.this, ShowTimes.class)
                                .putExtra("jsonString", jsonString)
                                .putExtra("readyTimeCal", chosenTime)
                                .putExtra("origin", "ItemScreen")
                                .putExtra("currentItem", 0)
                                .putExtra("reminders", reminderCheckBox.isChecked())
                                .putExtra("currentItem", 0));

                        SharedPreferences sharedPrefs = getSharedPreferences("foodItems", MODE_PRIVATE);
                        sharedPrefs.edit().putString("jsonString", "").apply();
                        alertDialog.dismiss();
                        finish();

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

    public void setEffectiveTotalTimes(){
        for(FoodItem food : foodItemList) {
            food.setEffectiveTotalTimes();
        }
    }

    public void saveMeal(View v) {
        if (BillingClass.isUpgraded) { //upgraded => allowed to save meals
            if (foodItemList.isEmpty()) {
                mToast.setText("Please add at least 1 item first");
                mToast.show();
            } else {
                View dialogView = inflater.inflate(
                        R.layout.dialog_save_meal, parentViewGroup,false);
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
        } else Toast.makeText(this, "Please upgrade to unlock this feature",
                Toast.LENGTH_SHORT).show();
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
    protected void onStop() {
        foodItemList.clear();
        SharedPreferences sp = getSharedPreferences("currentItems", MODE_PRIVATE);
        sp.edit().putString("currentItems", "").apply();
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        String jsonString = JsonHandler.getFoodItemJsonString(foodItemList);
        SharedPreferences sp = getSharedPreferences("currentItems", MODE_PRIVATE);
        sp.edit().putString("currentItems", jsonString).apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sp = getSharedPreferences("currentItems", MODE_PRIVATE);
        String json = sp.getString("currentItems", "");
        if(!json.matches("")) {
            foodItemList.clear();
            foodItemList.addAll(JsonHandler.getFoodItemList(this, json));
        }
    }



    private class AdapterNewItem extends ArrayAdapter<String> {

        AdapterNewItem(Context context, String[] items) {
            super(context, R.layout.lv_food_item, items);
        }

        @Override @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.lv_food_item, parent, false);
            }

            String[] info = getItem(position).split("\\|");
            TextView nameTV = (TextView) convertView.findViewById(R.id.itemNameTV);
            TextView stagesTV = (TextView) convertView.findViewById(R.id.itemStagesTV);
            TextView timeTV = (TextView) convertView.findViewById(R.id.itemTimeTV);
            nameTV.setText(String.valueOf(info[0]));
            stagesTV.setText(info[2]);
            timeTV.setText(String.format("%s " + getString(R.string.minutes), info[1]));

            return convertView;
        }
    }


    private class AdapterItemStages extends BaseAdapter {

        private int startPos, currentPos;
        private ArrayList<FoodItem.Stage> data, tempData;
        private long lastTouchTime = 0;
        private int lastTouchY;
        private final int MAX_TOUCH_DISTANCE = 100;
        private final long MAX_TOUCH_DELAY = 1000;


        AdapterItemStages(ArrayList<FoodItem.Stage> items) {
            data = items;
            tempData = new ArrayList<>(data);
        }

        @Override
        @SuppressWarnings("deprecation")
        public View getView(final int position, View convertView, final ViewGroup parent) {
            ViewHolder holder;


            if (convertView == null) {
                convertView = inflater.inflate(R.layout.lv_item_stage, parent, false);

                holder = new ViewHolder();
                holder.stageName = (TextView) convertView.findViewById(R.id.stageName);
                holder.stageTime = (TextView) convertView.findViewById(R.id.stageTime);
                holder.stageNumber = (TextView) convertView.findViewById(R.id.stageNumberTV);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            FoodItem.Stage stage = (FoodItem.Stage) getItem(position);
            holder.stageName.setText(stage.getName());
            holder.stageTime.setText(String.format(Locale.getDefault(), "%1d", stage.getTime()));
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
                        if (!isDoubleClick) {
                            lastTouchTime = thisTouchTime;
                            lastTouchY = thisTouchY;
                            return false;
                        } else { //start drag
                            startPos = position;
                            currentPos = position;

                            ClipData clipData = ClipData.newPlainText("", "");
                            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                            //USING DEPRECATED METHOD FOR API < 24
                            //startDrag() (deprecated in API 24) --> startDragAndDrop()
                            if (Build.VERSION.SDK_INT < 24) {
                                view.startDrag(clipData, shadowBuilder, view, 0);
                            } else {
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

                            if (position > startPos) {
                                //view dragged downwards
                                ((LinearLayout) v).setGravity(Gravity.TOP);
                            } else {
                                //view dragged upwards
                                ((LinearLayout) v).setGravity(Gravity.BOTTOM);
                            }

                            moveData(currentPos, position);
                            currentPos = position;
                            v.getLayoutParams().height = v.getHeight() * 2;

                            break;
                        case DragEvent.ACTION_DRAG_EXITED:
                            //go straight into ACTION_DROP case (resize view to original size)
                        case DragEvent.ACTION_DROP:
                            v.setLayoutParams(new AbsListView.LayoutParams(
                                    v.getWidth(), v.getHeight() / 2));
                            break;
                        case DragEvent.ACTION_DRAG_ENDED:
                            //update list view if drop valid (inside list view)
                            if (event.getResult()) {
                                updateListView();
                            } else { //drop invalid - reset tempData back to original data
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

        private void moveData(int from, int to) {
            FoodItem.Stage temp = tempData.get(from);
            tempData.remove(from);
            tempData.add(to, temp);

            updateListView();
        }

        private void updateListView() {
            data.clear();
            data.addAll(tempData);

            tempFoodItem.setStages(data);
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
    static class ViewHolder {
        TextView stageName, stageTime, stageNumber;
    }

}