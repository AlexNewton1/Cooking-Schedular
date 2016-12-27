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

        foodItem.addStage("stage3", String.valueOf(17));
        foodItem.addStage("stage4", String.valueOf(13));
        foodItem.addStage("stage5", String.valueOf(1));
        foodItem.addStage("stage6", String.valueOf(10));
        foodItem.addStage("stage7", String.valueOf(35));
        foodItem.addStage("stage8", String.valueOf(46));
        foodItem.addStage("stage9", String.valueOf(5));

        foodItem.setEffectiveTotalTimes();

        assertEquals(5, foodItem.getStages().get(0).getEffectiveTotalTime());
        assertEquals(20, foodItem.getStages().get(1).getEffectiveTotalTime());
        assertEquals(37, foodItem.getStages().get(2).getEffectiveTotalTime());
        assertEquals(50, foodItem.getStages().get(3).getEffectiveTotalTime());
        assertEquals(51, foodItem.getStages().get(4).getEffectiveTotalTime());
        assertEquals(61, foodItem.getStages().get(5).getEffectiveTotalTime());
        assertEquals(96, foodItem.getStages().get(6).getEffectiveTotalTime());
        assertEquals(142, foodItem.getStages().get(7).getEffectiveTotalTime());
        assertEquals(147, foodItem.getStages().get(8).getEffectiveTotalTime());
    }

}