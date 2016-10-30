package com.SoftwareOverflow.CookingScheduler;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

/**
 * Database storing meal names, json strings of food items in the meal & notes entered by the user
 */
public class MealDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME = "saved_meals.db", TABLE_MEALS = "meals",
            COLUMN_MEAL_NAME = "_name", COLUMN_JSON_STRING = "_jsonString", COLUMN_NOTES = "_notes",
            COLUMN_ID = "_id";
    private static final int DB_VERSION = 1;
    private Context context;

    protected MealDatabase(Context c, SQLiteDatabase.CursorFactory factory) {
        super(c, DB_NAME, factory, DB_VERSION);
        this.context = c;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE " + TABLE_MEALS + " (" + COLUMN_ID +
                " INTEGER PRIMARY KEY AUTOINCREMENT, "  + COLUMN_MEAL_NAME + " TEXT NOT NULL, " +
                COLUMN_JSON_STRING + " TEXT NOT NULL, " + COLUMN_NOTES + " TEXT NOT NULL);";
        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists " + TABLE_MEALS);
        onCreate(db);
    }

    /**
     * @return  true if meal saved or updated successfully. false if a meal with the same
     *          mealName is found
     */
    protected boolean addMeal(String mealName, String jsonString, String notes, int rowToUpdate) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MEAL_NAME, mealName);
        values.put(COLUMN_JSON_STRING, jsonString);
        values.put(COLUMN_NOTES, notes);

        //saving new meal
        if (rowToUpdate == -1) {
            if (!isDuplicateMeal(db, mealName)) { //write to database
                db.insert(TABLE_MEALS, null, values);
                db.close();
                Toast.makeText(context, "Meal saved successfully", Toast.LENGTH_SHORT).show();
                return true;
            }
            db.close();
            Toast.makeText(context, "A meal with this name already exists." +
                    "\nPlease choose a different meal name", Toast.LENGTH_SHORT).show();
            return false; //return false if duplicate found, toast to notify user
        }
        //updating previous meal
        else {
            db.update(TABLE_MEALS, values, COLUMN_ID + "=" + rowToUpdate, null);
            db.close();
            Toast.makeText(context, "Update saved successfully", Toast.LENGTH_SHORT).show();
            return true;
        }
    }


    /**
     * @param mealName - name of the meal to be checked
     * @return - true if duplicate meal name found in database
     * - false if no duplicate found
     */
    private boolean isDuplicateMeal(SQLiteDatabase db, String mealName) {
        String[] columns = new String[]{COLUMN_MEAL_NAME};
        String whereClause = COLUMN_MEAL_NAME + "= ?";
        String[] whereArgs = new String[]{mealName};

        Cursor cursor = db.query(TABLE_MEALS, columns, whereClause, whereArgs, null, null, null);
        int numMatches = cursor.getCount();
        cursor.close();
        return (numMatches != 0); //return true if and only if no matches
    }

    protected int getRowNum() {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_MEALS;
        Cursor cursor = db.rawQuery(query, null);
        int rows = cursor.getCount();
        cursor.close();
        db.close();
        return rows;
    }

    protected int getIdFromName(String mealName) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_MEALS + " WHERE " + COLUMN_MEAL_NAME + " = '"
                + mealName + "'";
        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToFirst();
        int ID = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
        cursor.close();
        db.close();
        return ID;
    }

    /**
     * @param searchString - Returns all meals with searchString contained within that meal name
     * @return - String Array = {MealName, JsonString (used to convert back to FoodItem), Notes}
     */
    protected String[] getSavedMeals(String searchString) {
        try {
            String mealNameQuery = "%" + searchString + "%";
            String query = "SELECT * FROM " + TABLE_MEALS + " WHERE " + COLUMN_MEAL_NAME + " LIKE "
                    + "'" + mealNameQuery + "' ORDER BY " + COLUMN_MEAL_NAME
                    + " COLLATE NOCASE DESC";
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.rawQuery(query, null);
            int rowCount = cursor.getCount();
            String[] results = new String[rowCount];
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);
                results[i] = cursor.getString(cursor.getColumnIndex(COLUMN_MEAL_NAME)) + "|*|"
                        + cursor.getString(cursor.getColumnIndex(COLUMN_JSON_STRING)) + "|*|"
                        + cursor.getString(cursor.getColumnIndex(COLUMN_NOTES));

            }
            cursor.close();
            db.close();
            return results;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    protected boolean deleteMeal(String mealName) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            String query = "SELECT * FROM " + TABLE_MEALS + " WHERE " + COLUMN_MEAL_NAME + " = '"
                    + mealName + "'";
            Cursor cursor = db.rawQuery(query, null);
            cursor.moveToFirst();
            int ID = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
            cursor.close();
            db.delete(TABLE_MEALS, COLUMN_ID + "=" + ID, null);
            db.close();
            Toast.makeText(context, "Meal deleted successfully", Toast.LENGTH_SHORT).show();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Unable to delete meal", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}
