package com.SoftwareOverflow.CookingScheduler.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/**
 * Handles all In App Billing
 */
public class BillingClass extends Activity implements IabBroadcastReceiver.IabBroadcastListener{

    private Context context;
    private IabHelper mHelper;
    private final String UPGRADE_SKU = "pro_version_upgrade";
    private IabBroadcastReceiver mBroadcastReceiver;
    private IabHelper.QueryInventoryFinishedListener mGotInventoryListener;

    public static boolean isUpgraded = true;

    public BillingClass(Context c){
        this.context = c;

        setup();
    }

    public void setContext(Context context){
        this.context = context;
    }

    private void setup(){
       mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
            @Override
            public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                if(mHelper != null){
                    if(result.isFailure()){
                        showAlert("Failed to query inventory: " + result);
                    }
                    else{ //check for items we own
                        Purchase upgradePurchase = inv.getPurchase(UPGRADE_SKU);
                        isUpgraded = (upgradePurchase!=null);
                    }

                    mHelper.flagEndAsync();
                }
            }
        };

        mBroadcastReceiver = new IabBroadcastReceiver(this);
        IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
        context.registerReceiver(mBroadcastReceiver, broadcastFilter);

        //Get public base64EncodedKey from separate class (built at runtime
        //using string transformations & encoding)
        String base64EncodedKey = PublicKeyCreation.stringTransform();
        mHelper = new IabHelper(context, base64EncodedKey);

        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                String logMessage = result.isSuccess() ?
                        "IAB fully set up" : "Problem Setting Up In App Billing: " + result;
                Log.d("IAB", logMessage);

                if(mHelper != null) {
                    queryInventory();
                }
            }
        });
    }

    @Override
    public void receivedBroadcast() {
        //received broadcast notification that inventory of items has changed
        Log.d("IAB", "Received broadcast notification. Querying inventory.");
        try{
            mHelper.queryInventoryAsync(mGotInventoryListener);
        } catch (IabHelper.IabAsyncInProgressException e) {
            showAlert("Error querying inventory. Another async operation in progress");
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mHelper.handleActivityResult(requestCode, resultCode, data);
    }


    public void queryInventory(){
        try{
            mHelper.queryInventoryAsync(mGotInventoryListener);
        } catch (IabHelper.IabAsyncInProgressException e){
            showAlert("Problem querying inventory.");
        }
    }

    public void purchaseUpgrade(Activity activity) {

        final String payloadString = ""; //using blank payload string - no server on which to store

        final int RC_REQUEST = 10001; //arbitrary request code for purchase flow
        try{
            mHelper.launchPurchaseFlow(activity, UPGRADE_SKU, RC_REQUEST, new IabHelper.OnIabPurchaseFinishedListener() {
                @Override
                public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
                    if(mHelper != null){
                        mHelper.flagEndAsync();

                        if(result.isSuccess()){
                            Log.d("IAB", "Successful purchase!");
                            if(purchase.getSku().equals(UPGRADE_SKU)) {
                                isUpgraded = true;
                                showAlert("Thank you for upgrading to premium!");
                            }
                        }
                        else showAlert("Problem Purchasing: " + result);

                    }
                }
            }, payloadString);
        } catch (IabHelper.IabAsyncInProgressException e){
            e.printStackTrace();
            showAlert("Problem Purchasing Item (Exception caught)");
        }
    }

    private void showAlert(String message){
        AlertDialog.Builder bld = new AlertDialog.Builder(context);
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        bld.create().show();
        mHelper.flagEndAsync();
    }

    public void dispose() {
        if(mBroadcastReceiver!=null) unregisterReceiver(mBroadcastReceiver);
        if (mHelper != null) {
            mHelper.disposeWhenFinished();
            mHelper = null;
        }
    }
}
