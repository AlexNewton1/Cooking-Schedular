package com.SoftwareOverflow.CookingScheduler;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.SoftwareOverflow.CookingScheduler.util.BillingClass;

public class UpgradeScreen extends Activity {

    private TextView statusTV;
    private Button upgradeButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upgrade_class);

        upgradeButton = (Button) findViewById(R.id.upgradeButton);
        upgradeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (HomeScreen.billing.purchaseUpgrade(UpgradeScreen.this)){
                    upgradePurchased();
                }
            }
        });

        statusTV = (TextView) findViewById(R.id.statusTV);
        if (!BillingClass.isUpgraded) {
            statusTV.setTextColor(ContextCompat.getColor(this, R.color.orange));
            statusTV.setText(getResources().getString(R.string.free));
        } else {
            upgradePurchased();
        }
    }

    protected void upgradePurchased(){
        statusTV.setTextColor(ContextCompat.getColor(this, R.color.dark_green));
        statusTV.setText(getResources().getString(R.string.upgraded));
        upgradeButton.setEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        HomeScreen.billing.queryInventory();
        if(BillingClass.isUpgraded) upgradePurchased();
    }
}
