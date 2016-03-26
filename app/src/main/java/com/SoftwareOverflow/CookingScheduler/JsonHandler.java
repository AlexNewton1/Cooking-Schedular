package com.SoftwareOverflow.CookingScheduler;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Store/retrieve AlarmClass & FoodItem objects ( using shared preferences and json)
 * Methods relating to foodItem objects are fo use with database
 * All methods static for use without creating object first
 * All json strings delimited with triple pipe character (|||)
 */
public class JsonHandler {

    private static Gson gson = new Gson();

    /**
     * @param c - context
     * @return - list of AlarmClass objects saved in sharedPrefs (contains alarm times etc)
     */
    public static List<AlarmClass> getAlarmList(Context c){
        SharedPreferences sp = c.getApplicationContext().getSharedPreferences("alarms", Context.MODE_PRIVATE);
        String infoString = sp.getString("alarmInfo", "");
        List<AlarmClass> alarmList = new ArrayList<>();

        if(!infoString.isEmpty()) {
            Type type = new TypeToken<AlarmClass>() {}.getType();

            try {
                for (String info : infoString.split("\\|\\|\\|")) {
                    AlarmClass alarm = gson.fromJson(info, type);
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
     * @param alarmList - List of AlarmClass objects to be saved in SharedPreferences
     */
    public static void putAlarmList(Context c, List<AlarmClass> alarmList){
        SharedPreferences sp = c.getApplicationContext().getSharedPreferences("alarms", Context.MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        for(AlarmClass alarm: alarmList) sb.append(gson.toJson(alarm)).append("|||");
        sp.edit().putString("alarmInfo", sb.toString()).apply();
    }

    public static List<FoodItem> getFoodItemList(Context c, String jsonString){
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
            Log.e("json", jsonString);
            Log.e("json", ex.toString());
            Toast.makeText(c, "Error loading values", Toast.LENGTH_SHORT).show();
        }

        return foodItemList;
    }

    public static String getFoodItemJsonString(Context c, List<FoodItem> foodItemList){
        StringBuilder sb = new StringBuilder();
        for (FoodItem food: foodItemList) sb.append(gson.toJson(food)).append("|||");
        return sb.toString();
    }

    public static void putFoodItemList(Context c, List<FoodItem> foodItems){
        SharedPreferences sp = c.getApplicationContext().getSharedPreferences("foods", Context.MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        for(FoodItem food: foodItems) sb.append(gson.toJson(food)).append("|||");
        sp.edit().putString("foodInfo", sb.toString()).apply();
    }
}