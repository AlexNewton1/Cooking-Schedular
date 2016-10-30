package com.SoftwareOverflow.CookingScheduler;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Store/retrieve NotificationClass & FoodItem objects (using shared preferences and json)
 * Methods relating to foodItem objects are for use with database
 * All methods static for use without creating object first
 * All json strings delimited with triple pipe character (|||)
 */
class JsonHandler {

    private static Gson gson = new Gson();

    /**
     * @param c - context
     * @return - list of NotificationClass objects saved in sharedPrefs (contains alarm times etc)
     */
     static List<NotificationClass> getAlarmList(Context c){
        SharedPreferences sp = c.getApplicationContext()
                .getSharedPreferences("alarms", Context.MODE_PRIVATE);
        String infoString = sp.getString("alarmInfo", "");
        List<NotificationClass> alarmList = new ArrayList<>();

        if(!infoString.isEmpty()) {
            Type type = new TypeToken<NotificationClass>() {}.getType();

            try {
                for (String info : infoString.split("\\|\\|\\|")) {
                    NotificationClass alarm = gson.fromJson(info, type);
                    alarmList.add(alarm);
                }
            }
            catch(JsonSyntaxException ex){
                Toast.makeText(c, "Error loading values", Toast.LENGTH_SHORT).show();
            }
        }
        return alarmList;
    }

    /**
     * @param c - context
     * @param alarmList - List of NotificationClass objects to be saved in SharedPreferences
     */
    static void putAlarmList(Context c, List<NotificationClass> alarmList){
        SharedPreferences sp = c.getApplicationContext()
                .getSharedPreferences("alarms", Context.MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        for(NotificationClass alarm: alarmList) sb.append(gson.toJson(alarm)).append("|||");
        sp.edit().putString("alarmInfo", sb.toString()).apply();
    }

    static List<FoodItem> getFoodItemList(Context c, String jsonString){
        Type foodType = new TypeToken<FoodItem>(){}.getType();
        List<FoodItem> foodItemList = new ArrayList<>();

        try {
            for(String foodItem : jsonString.split("\\|\\|\\|")){
                if(!foodItem.matches("")) {
                    FoodItem food = gson.fromJson(foodItem, foodType);
                    foodItemList.add(food);
                }
            }

        }
        catch (JsonSyntaxException ex){
            Toast.makeText(c, "Error loading values", Toast.LENGTH_SHORT).show();
        }

        return foodItemList;
    }

    static String getFoodItemJsonString(List<FoodItem> foodItemList){
        StringBuilder sb = new StringBuilder();
        for (FoodItem food: foodItemList) sb.append(gson.toJson(food)).append("|||");
        return sb.toString();
    }

}
