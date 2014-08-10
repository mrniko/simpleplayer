package org.sergez.splayer.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import org.sergez.splayer.R;
import org.sergez.splayer.inappbilling.IabHelper;
import org.sergez.splayer.inappbilling.IabResult;
import org.sergez.splayer.inappbilling.Inventory;
import org.sergez.splayer.inappbilling.Purchase;
import org.sergez.splayer.util.Constants;

import static org.sergez.splayer.util.Utils.makeToast;

/**
 * @author Sergii Zhuk
 *         Date: 20.07.2014
 *         Time: 13:30
 *
 *         Based on tutorial
 *         http://www.techotopia.com/index.php/Integrating_Google_Play_In-app_Billing_into_an_Android_Application_%E2%80%93_A_Tutorial
 */
public class DonationActivity extends Activity {
    private static final String TAG = DonationActivity.class.getName();
    static final String ITEM_S5 = "donate_s5";
    static final String ITEM_S10 = "donate_s10";
    static final String ITEM_S20 = "donate_s20";

    IabHelper mHelper;

    private Button buttonDonateS5;
    private Button buttonDonateS10;
    private Button buttonDonateS20;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donate);
        buttonDonateS5 = (Button) findViewById(R.id.button_donate_s5);
        buttonDonateS10 = (Button) findViewById(R.id.button_donate_s10);
        buttonDonateS20 = (Button) findViewById(R.id.button_donate_s20);
        buttonDonateS5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHelper.launchPurchaseFlow(DonationActivity.this, ITEM_S5, 10001, mPurchaseFinishedListener, "mypurchasetoken" + ITEM_S5);
            }
        });
        buttonDonateS10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHelper.launchPurchaseFlow(DonationActivity.this, ITEM_S10, 10002, mPurchaseFinishedListener, "mypurchasetoken" + ITEM_S10);
            }
        });
        buttonDonateS20.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHelper.launchPurchaseFlow(DonationActivity.this, ITEM_S20, 10003, mPurchaseFinishedListener, "mypurchasetoken" + ITEM_S20);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        mHelper = new IabHelper(this, Constants.DONATION_PUBLIC_KEY);

        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    Log.d(TAG, "In-app Billing setup failed: " + result);
                } else {
                    Log.d(TAG, "In-app Billing is set up OK");
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!mHelper.handleActivityResult(requestCode,
                resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            if (result.isFailure()) {
                Log.e(TAG, "Error" + result.getMessage());
                //TODO Handle error
                return;
            } else if (purchase.getSku().equals(ITEM_S5)) {
                consumeItem();
            }
        }
    };

    public void consumeItem() {
        mHelper.queryInventoryAsync(mReceivedInventoryListener);
    }

    IabHelper.QueryInventoryFinishedListener mReceivedInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            if (result.isFailure()) {
                // TODO Handle failure
                Log.e(TAG, "Error" + result.getMessage());
            } else {
                mHelper.consumeAsync(inventory.getPurchase(ITEM_S5), mConsumeFinishedListener);
            }
        }
    };

    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            if (result.isSuccess()) {
                makeToast(DonationActivity.this, "Thank you!");
            } else {
                // TODO handle error
                Log.e(TAG, "Error" + result.getMessage());
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHelper != null) {
            mHelper.dispose();
        }
        mHelper = null;
    }

}
