package com.SoftwareOverflow.CookingScheduler;

/**
 * Holds a name and cooking time for an item
 */
public class FoodItem implements Comparable<FoodItem> {

    public String name;
    public int time;

    public FoodItem(String name, int time){
        this.name = name;
        this.time = time;
    }

    public String getInfo(){
        return name + "|" + time;
    }

    @Override
    public int compareTo(FoodItem other) { //returns a sorted list in descending time order
        if(this.time > other.time) return -1;
        else if (this.time<other.time) return 1;
        else return 0;
    }
}
