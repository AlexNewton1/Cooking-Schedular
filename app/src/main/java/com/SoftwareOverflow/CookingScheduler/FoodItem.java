package com.SoftwareOverflow.CookingScheduler;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores information about food items used in meals. Each FoodItem can have multiple stages associated
 * with it, for example the FoodItem titled 'Steak' might have a 'cook' and a 'rest' stage for it's
 * production.
 *
 *      name - the name of the meal
 *      total time - the sum of times for each stage of the food item
 *      numStages - the number of stages involved in the cooking/making/preparing of the foodItem
 *      stagesList - a List of stages stored in the form "stageName|stageTime"
 */
public class FoodItem implements Comparable<FoodItem> {

    public String name;
    public int totalTime = 0;
    public int numStages = 0;
    private List<String> stagesList = new ArrayList<>();

    public FoodItem(String name){
        this.name = name;
    }


    public void addStage(String name, String time){
        stagesList.add(name + "|" + time);
        totalTime += Integer.parseInt(time);
        numStages++;
    }
    public void removeStage(int stageNum){
        stagesList.remove(stageNum);
        numStages--;
    }
    public void updateStage(String newStage, int index){
        int previousTime = Integer.parseInt(stagesList.get(index).split("\\|")[1]);
        totalTime -= previousTime;
        stagesList.remove(index);
        stagesList.add(index, newStage);
        totalTime += Integer.parseInt(newStage.split("\\|")[1]);
    }

    public void setFoodStages(List<String> stagesList){
        this.stagesList = stagesList;
    }
    public List<String> getFoodStages() { return stagesList; }
    public String getInfo(){
        return name + "|" + totalTime + "|" + numStages;
    }


    @Override
    //returns a sorted list in descending time order when collections.sort(List) is called)
    public int compareTo(FoodItem other) {
        if (this.totalTime < other.totalTime) return 1;
        else return -1;
    }
}
