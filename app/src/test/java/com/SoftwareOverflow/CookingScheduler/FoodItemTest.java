package com.SoftwareOverflow.CookingScheduler;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class FoodItemTest {

    private String name = "beef", stage1 = "prepare", stage2 = "cook";
    private FoodItem foodItem;
    private int time1 = 5, time2 = 15;

    @Before
    public void setUp() throws Exception {
        foodItem = new FoodItem(name);
    }

    private void populateTestItem(int num){
        if(num == 1 || num ==2) foodItem.addStage(stage1, Integer.toString(time1));
        if(num ==2) foodItem.addStage(stage2, Integer.toString(time2));
    }

    @Test
    public void testAddStage() throws Exception {
        foodItem.addStage(stage1, Integer.toString(time1));

        assertEquals(name, foodItem.name);
        assertEquals(1, foodItem.numStages);
        assertEquals(name + "|" + foodItem.totalTime + "|" + foodItem.numStages, foodItem.getInfo());

        foodItem.addStage(stage2, Integer.toString(time2));

        assertEquals(2, foodItem.numStages);
        assertEquals( name + "|" + foodItem.totalTime +  "|" + foodItem.numStages, foodItem.getInfo());
    }

    @Test
    public void testRemoveStage() throws Exception {
        populateTestItem(2);
        foodItem.removeStage(0);
        assertEquals(1, foodItem.numStages);
        assertEquals(time2, foodItem.totalTime);
    }

    @Test
    public void testUpdateStage() throws Exception {
        populateTestItem(2);
        int newTotalTime = time2 + 10;
        foodItem.updateStage("newStageName", 10, 0);

        assertEquals("newStageName", foodItem.getStages().get(0).getName());
        assertEquals(2, foodItem.numStages);
        assertEquals(newTotalTime, foodItem.totalTime);
    }

    @Test
    public void testEffectiveTotalTime() throws Exception{
        setUp();
        populateTestItem(2);
        foodItem.setStagesEffectiveTime();

        int effectiveStageTime;
        effectiveStageTime = foodItem.getStages().get(0).getTime();
        assertEquals(time1, effectiveStageTime);

        effectiveStageTime+=foodItem.getStages().get(1).getTime();
        assertEquals(time1+time2, effectiveStageTime);
    }

}