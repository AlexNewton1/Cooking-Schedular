package com.SoftwareOverflow.CookingScheduler;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.SearchView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

public class SavedMeals extends Activity {

    private MealDatabase mealDB;
    private ExpandableListView mealsLV;
    private String searchString = "";
    private TextView noSavedMealsTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_meals);

        noSavedMealsTV = (TextView) findViewById(R.id.noSavedMealsTV);
        SearchView searchView = (SearchView) findViewById(R.id.savedMealSearch);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if(newText!=null) searchString = newText;
                showListView(searchString);
                return true;
            }
        });

        showListView(searchString);
    }

    private void showListView(String searchString){
        mealDB = new MealDatabase(this, null);
        String[] adapterStrings = mealDB.getSavedMeals(searchString);
        if(adapterStrings.length == 0) noSavedMealsTV.setVisibility(View.VISIBLE);
        else noSavedMealsTV.setVisibility(View.GONE);
        BaseExpandableListAdapter adapter = new AdapterSavedMeals(this, adapterStrings);
        mealsLV = (ExpandableListView) findViewById(R.id.savedMealLV);
        mealsLV.bringToFront();
        mealsLV.setAdapter(adapter);
    }


    /**
     * Class to work as adapter for expandable listView.
     */
    private class AdapterSavedMeals extends BaseExpandableListAdapter{

        private String[] adapterStrings;
        private Context context;
        private int lastExpandedGroupPosition;

        public AdapterSavedMeals(Context context, String[] adapterStrings){
            this.context = context;
            this.adapterStrings = adapterStrings;
        }

        @Override
        public void onGroupExpanded(int groupPosition) {
            if(groupPosition != lastExpandedGroupPosition){
                mealsLV.collapseGroup(lastExpandedGroupPosition);
                lastExpandedGroupPosition = groupPosition;
            }
            super.onGroupExpanded(groupPosition);
        }

        @Override
        public int getGroupCount() {
            return adapterStrings.length;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return 1; //only 1 child (the options button bar)
        }

        @Override
        public Object getGroup(int groupPosition) {
            return adapterStrings[groupPosition];
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return findViewById(R.id.savedMealButtonBar);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            if(convertView == null){
                LayoutInflater layoutInflater = (LayoutInflater) this.context.getSystemService(LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.lv_saved_meals, null);
            }

            TextView titleTV = (TextView) convertView.findViewById(R.id.savedMealName);
            TextView notesTV = (TextView) convertView.findViewById(R.id.savedMealNotes);
            TextView numStagesTV = (TextView) convertView.findViewById(R.id.savedMealNumFoodItems);
            TextView totalTimeTV = (TextView) convertView.findViewById(R.id.savedMealTotalTime);

            String[] headerInfo = adapterStrings[groupPosition].split("\\|\\*\\|");
            List<FoodItem> foodItemList = JsonHandler.getFoodItemList(context, headerInfo[1]);
            Collections.sort(foodItemList);
            int numStages = 0;
            for(FoodItem aFoodItem : foodItemList){
                numStages+=aFoodItem.numStages;
            }

            titleTV.setText(headerInfo[0]);
            notesTV.setText(headerInfo[2]);
            numStagesTV.setText(foodItemList.size() + " items - " + numStages + " stages");
            totalTimeTV.setText(Integer.toString(foodItemList.get(0).totalTime));

            return convertView;
        }

        @Override
        public View getChildView(final int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            if(convertView == null){
                LayoutInflater layoutInflater = (LayoutInflater) this.context.getSystemService(LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.lv_saved_meals_expandable_button_bar, null);
            }

            Button loadButton = (Button) convertView.findViewById(R.id.loadSavedMeal);
            Button deleteButton = (Button) convertView.findViewById(R.id.deleteSavedMeal);

            loadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    loadSavedMeal(groupPosition);
                }
            });
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String mealName = adapterStrings[groupPosition].split("\\|\\*\\|")[0];
                    new AlertDialog.Builder(SavedMeals.this)
                            .setTitle("Delete meal '" + mealName + "'?")
                            .setMessage("This cannot be undone")
                            .setPositiveButton("Delete Meal", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mealDB.deleteMeal(mealName);
                                    showListView(searchString);
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).create().show();
                }
            });

            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        public void loadSavedMeal(int groupPosition){
            String[] savedInfo = adapterStrings[groupPosition].split("\\|\\*\\|");
            String mealName = savedInfo[0], jsonString = savedInfo[1], notes = savedInfo[2];
            int mealID = mealDB.getIdFromName(mealName);

            startActivity(new Intent(SavedMeals.this, ItemScreen.class)
                    .putExtra("updatingMeal", mealID)
                    .putExtra("mealName", mealName)
                    .putExtra("mealNotes", notes)
                    .putExtra("jsonString", jsonString));
            finish();
        }
    }

}
