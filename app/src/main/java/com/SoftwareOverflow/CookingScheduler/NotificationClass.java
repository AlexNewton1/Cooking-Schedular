package com.SoftwareOverflow.CookingScheduler;

import android.support.annotation.NonNull;

/**
 * Stores info about the upcoming notifications
 */
public class NotificationClass implements Comparable<NotificationClass>{

    public int id;
    private long alarmTime;
    public String name;
    public int cookingTime;

    protected NotificationClass(int cookingTime, Long alarmTimeMillis, String name, int id) {
        this.id = id;
        this.alarmTime = alarmTimeMillis;
        this.name = name;
        this.cookingTime = cookingTime;
    }

    protected String getInfo(){
        return name + "|" + alarmTime;
    }

    @Override
    public int compareTo(@NonNull NotificationClass other) {
        if(this.alarmTime > other.alarmTime) return 1;
        else if(this.alarmTime < other.alarmTime) return -1;
        else return 0;
    }

}
