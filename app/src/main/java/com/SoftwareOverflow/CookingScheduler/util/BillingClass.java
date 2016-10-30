package com.SoftwareOverflow.CookingScheduler.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

import static android.content.ContentValues.TAG;

/**
 * Handles all In App Billing
 */
public class BillingClass extends Activity implements IabBroadcastReceiver.IabBroadcastListener {

    private Context context;
    private IabHelper mHelper;
    private final String UPGRADE_SKU = "pro_version_upgrade";

    private IabBroadcastReceiver mBroadcastReceiver;
    private IabHelper.QueryInventoryFinishedListener mGotInventoryListener;

    public static boolean isUpgraded = true;

    public BillingClass(Context c) {
        context = c;

        setup();
    }

    private void setup() {
        //Get public base64EncodedKey from separate class (built at runtime
        //using string transformations & encoding)
        String base64EncodedKey = PublicKeyCreation.stringTransform();
        mHelper = new IabHelper(context, base64EncodedKey);
        Log.d("db", "mHelper assigned");

        mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
            @Override
            public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                if (mHelper != null) {
                    if (result.isFailure()) {
                        showAlert("Failed to query inventory.\n" + result.getMessage());
                    } else { //check for items we own
                        Purchase upgradePurchase = inv.getPurchase(UPGRADE_SKU);
                        isUpgraded = (upgradePurchase != null);
                    }

                    mHelper.flagEndAsync();
                }
            }
        };

        Log.d("db", "pre startSetup");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                Log.d("db", "setupFinished, result good: " + result.isSuccess());
                if (result.isSuccess() && mHelper != null) {
                    queryInventory();
                }
                else{
                    Toast.makeText(context.getApplicationContext(), "ERROR!", Toast.LENGTH_LONG).show();
                }
            }
        });


        mBroadcastReceiver = new IabBroadcastReceiver(this);
        IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
        context.registerReceiver(mBroadcastReceiver, broadcastFilter);
    }

    @Override
    public void receivedBroadcast() {
        //received broadcast notification that inventory of items has changed
        try {
            mHelper.queryInventoryAsync(mGotInventoryListener);
        } catch (IabHelper.IabAsyncInProgressException e) {
            showAlert("Error querying inventory. Another async operation in progress");
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
            Log.i(TAG, "onActivityResult handled by IABUtil.");
        }
    }


    public void queryInventory() {
        try {
            mHelper.queryInventoryAsync(mGotInventoryListener);
        } catch (IabHelper.IabAsyncInProgressException e) {
            showAlert("Problem querying inventory.");
        }
    }


    public boolean purchaseUpgrade(Activity activity) {
        final String payloadString = "";
        final int RC_REQUEST = 10001; //arbitrary request code for purchase flow

        try {
            mHelper.launchPurchaseFlow(activity, UPGRADE_SKU, RC_REQUEST, new IabHelper.OnIabPurchaseFinishedListener() {
                @Override
                public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
                    if (mHelper != null) {
                        mHelper.flagEndAsync();
                        if (result.isSuccess()) {
                            if (purchase.getSku().equals(UPGRADE_SKU)) {
                                isUpgraded = true;
                            }
                        } else showAlert(result.getMessage());
                    }
                }
            }, payloadString);
        } catch (IabHelper.IabAsyncInProgressException e) {
            e.printStackTrace();
            showAlert("Problem Purchasing Item");
        }

        return isUpgraded;
    }

    private void showAlert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(context);
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        bld.create().show();
        mHelper.flagEndAsync();
    }

    public void dispose() {
        if (mBroadcastReceiver != null) {
            context.unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;

        }
        if (mHelper != null) {
            mHelper.disposeWhenFinished();
            mHelper = null;
        }
    }
}