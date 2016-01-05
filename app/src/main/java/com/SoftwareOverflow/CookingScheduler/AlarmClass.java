package com.SoftwareOverflow.CookingScheduler;

/**
 * Stores info about the upcoming notifications
 */
public class AlarmClass {

    private int id;
    public long alarmTime;
    public String name;

    public AlarmClass(Long alarmTimeMillis, String name, int id) {

        this.id = id;
        this.alarmTime = alarmTimeMillis;
        this.name = name;
    }

    public String getInfo(){
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("|").append(alarmTime).append("|").append(id).append("||");
        return sb.toString();
    }
}
