package com.SoftwareOverflow.CookingScheduler;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.SoftwareOverflow.CookingScheduler.util.BillingClass;

public class UpgradeScreen extends Activity {

    //TODO -- Add upgrade SKU & payload string
    //TODO -- implement InAppBilling

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upgrade_class);

        TextView statusTV = (TextView) findViewById(R.id.statusTV);
        if (!BillingClass.isUpgraded) {
            statusTV.setTextColor(ContextCompat.getColor(this, R.color.orange));
            statusTV.setText(getResources().getString(R.string.free));
        } else {
            statusTV.setTextColor(ContextCompat.getColor(this, R.color.darkgreen));
            statusTV.setText(getResources().getString(R.string.upgraded));
        }

        Button upgradeButton = (Button) findViewById(R.id.upgradeButton);
        upgradeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeScreen.billing.setContext(UpgradeScreen.this);
                HomeScreen.billing.purchaseUpgrade(UpgradeScreen.this);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        HomeScreen.billing.queryInventory();
    }
}
