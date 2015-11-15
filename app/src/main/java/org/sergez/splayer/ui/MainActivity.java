package org.sergez.splayer.ui;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import org.sergez.splayer.R;
import org.sergez.splayer.service.SimplePlayerService;

import static org.sergez.splayer.ui.ActivityHelper.getMainActivityContainer;

/**
 * @author Sergii Zhuk
 *         Date: 31.10.2015
 *         Time: 21:13
 */

public class MainActivity extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!getResources().getBoolean(R.bool.tablet_layout)) { // for non-tablet only portrait orientation looks good
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		setContentView(R.layout.activity_root);
		Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
		mToolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
		setSupportActionBar(mToolbar);
		ActivityHelper.navigateTo(this, new PlayerFragment(), getMainActivityContainer());
	}

	@Override
	public void onNewIntent(Intent intent) {
		clearNotifications();
	}

	public void clearNotifications(){
		PlayerFragment myFragment = (PlayerFragment)getSupportFragmentManager().findFragmentByTag(PlayerFragment.class.getName());
		if (myFragment != null && myFragment.isVisible()) {
			myFragment.clearNotifications();
		}
	}

	@Override
	public void onBackPressed() {
		PlayerFragment myFragment = (PlayerFragment)getSupportFragmentManager().findFragmentByTag(PlayerFragment.class.getName());
		if (myFragment != null && myFragment.isVisible()) {
			if(!myFragment.onBackPressed()){
				super.onBackPressed();
			}
		}
	}
}
