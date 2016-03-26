package com.SoftwareOverflow.CookingScheduler;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

public class UpgradeScreen extends AppCompatActivity {

    protected static boolean showAds = false;
    //TODO -- Add upgrade SKU & payload string
    //TODO -- implement InAppBilling


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upgrade_class);

        TextView statusTV = (TextView) findViewById(R.id.statusTV);

        if(showAds) {
            statusTV.setTextColor(ContextCompat.getColor(this, R.color.orange));
            statusTV.setText(" FREE");
        }else{
            statusTV.setTextColor(ContextCompat.getColor(this, R.color.darkgreen));
            statusTV.setText(" UPGRADED");
        }




        /*

        mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
            public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
                if (result.isFailure()) {
                    Log.d("iab", "Error purchasing: " + result);
                } else if (purchase.getSku().equals(AD_FREE_SKU)) {
                    if(purchase.getDeveloperPayload().equals(payload.replaceAll("a", "b").replaceAll("g","f"))){
                        showAds = false;
                    }
                }
            }
        };
        */
    }

    public void upgrade(View v) {
        /*
        HomeScreen.mHelper.launchPurchaseFlow(this, AD_FREE_SKU, 1001001,
                mPurchaseFinishedListener, payload.replaceAll("a", "b").replaceAll("g", "f"));
                */
    }

}
