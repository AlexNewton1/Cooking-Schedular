package com.SoftwareOverflow.CookingScheduler;

/**
 * Stores info about the upcoming notifications
 */
public class AlarmClass implements Comparable{

    public int id;
    public long alarmTime;
    public String name;
    public int cookingTime;

    public AlarmClass(int cookingTime, Long alarmTimeMillis, String name, int id) {

        this.id = id;
        this.alarmTime = alarmTimeMillis;
        this.name = name;
        this.cookingTime = cookingTime;
    }

    public String getInfo(){
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("|").append(alarmTime).append("|");
        return sb.toString();
    }

    @Override
    public int compareTo(Object another) {
        AlarmClass other = (AlarmClass) another;

        if(this.alarmTime > other.alarmTime) return 1;
        else if(this.alarmTime < other.alarmTime) return -1;
        else return 0;
    }
}
