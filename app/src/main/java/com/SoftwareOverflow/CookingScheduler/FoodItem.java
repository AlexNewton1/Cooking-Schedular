package com.SoftwareOverflow.CookingScheduler;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores information about food items used in meals. Each FoodItem can have multiple stages
 * associated with it, for example the FoodItem titled 'Steak' might have a 'cook' and a 'rest'
 * stage for it's production.
 *
 * name - the name of the meal
 * total time - the sum of times for each stage of the food item
 * numStages - the number of stages involved in the cooking/making/preparing of the foodItem
 * stagesList - a List of stages stored in the form "stageName|stageTime"
 */
class FoodItem implements Comparable<FoodItem> {

    public String name;
    int totalTime = 0;
    int numStages = 0;
    private List<Stage> stages = new ArrayList<>();

    FoodItem(String name) {
        this.name = name;
    }


    void addStage(String name, String time) {
        stages.add(new Stage(name, Integer.parseInt(time)));
        totalTime += Integer.parseInt(time);
        numStages++;
    }

    void removeStage(int stageNum) {
        totalTime -= stages.get(stageNum).getTime();
        stages.remove(stageNum);
        numStages--;
    }

    void updateStage(String name, int time, int index) {
        int previousTime = stages.get(index).getTime();
        totalTime -= previousTime;
        stages.remove(index);
        stages.add(index, new Stage(name, time));
        totalTime += time;

    }

    /**
     * This method associates the stages with the FoodItem by storing the FoodItem name.
     * This cannot be done at Stage creation due to unknown FoodItem name at this point.
     */
    void addNameToStages() {
        for (Stage stage : stages) stage.foodItemName = name;
    }

    void setStages(List<Stage> stagesList) {
        this.stages = stagesList;
    }

    List<Stage> getStages() {
        return stages;
    }

    String getInfo() {
        return name + "|" + totalTime + "|" + numStages;
    }

    void setStagesEffectiveTime() {
        int effectiveStageTime = 0;

        for(int i=stages.size()-1; i>=0; i--){
            effectiveStageTime += stages.get(i).getTime();
            stages.get(i).effectiveTotalTime = effectiveStageTime;
        }
    }


    @Override
    //returns a sorted list in descending time order when collections.sort(List) is called
    public int compareTo(@NonNull FoodItem other) {
        if (this.totalTime < other.totalTime) return 1;
        else return -1;
    }

    /**
     * Class to hold information for each individual stage for food items.
     * For example: 'Make biscuit base' and 'Create filling' might be stages to create a
     * FoodItem named 'cheesecake'
     */
    class Stage implements Comparable<Stage> {

        private String stageName;
        private String foodItemName;
        private int time;
        private int effectiveTotalTime = 0; //the time of the stage plus all following stages

        private Stage(String stageName, int time) {
            this.stageName = stageName;
            this.time = time;
            this.foodItemName = name;
        }

        public int getTime() {
            return time;
        }

        public String getName() {
            return stageName;
        }

        int getEffectiveTotalTime() {
            return effectiveTotalTime;
        }

        String getFoodItemName() {
            return foodItemName;
        }

        @Override
        public int compareTo(@NonNull Stage otherStage) {
            if (effectiveTotalTime > otherStage.effectiveTotalTime) return 1;
            else if (effectiveTotalTime < otherStage.effectiveTotalTime) return -1;
            else return 0;
        }

        @Override
        public String toString() {
            return stageName + "|" + time;
        }
    }
}
