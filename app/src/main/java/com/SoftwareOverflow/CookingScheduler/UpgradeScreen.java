package com.SoftwareOverflow.CookingScheduler;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.widget.TextView;

public class UpgradeScreen extends Activity {

    protected static boolean showAds = false;
    //TODO -- Add upgrade SKU & payload string
    //TODO -- implement InAppBilling


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upgrade_class);

        TextView statusTV = (TextView) findViewById(R.id.statusTV);

        if (showAds) {
            statusTV.setTextColor(ContextCompat.getColor(this, R.color.orange));
            statusTV.setText(getResources().getString(R.string.free));
        } else {
            statusTV.setTextColor(ContextCompat.getColor(this, R.color.darkgreen));
            statusTV.setText(getResources().getString(R.string.upgraded));
        }

    }

}
