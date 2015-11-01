package org.sergez.splayer.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.sergez.splayer.R;

public class PreferencesActivity extends AppCompatPreferenceActivity  {
	private final static String TAG = PreferencesActivity.class.getSimpleName();
	private ProgressDialog pDialog;
	private MediaIntentReceiver mediaIntentReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

		Preference rowAbout = findPreference("showAbout");   // TODO move to constants
		rowAbout.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				PackageInfo pInfo = null;
				String version = null;
				try {
					pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
					version = pInfo.versionName;
				} catch (NameNotFoundException e) {
					Log.e(TAG, e.getMessage(), e);
					version = "?";
				}

				String title = getResources().getString(R.string.about_title) + " " + getResources().getString(R.string.app_name);
				new AlertDialog.Builder(PreferencesActivity.this)
						.setIcon(R.drawable.ic_launcher)
						.setTitle(title)
						.setMessage(
								getString(R.string.version) + " " + version + "\n" + "\n"
										+ getString(R.string.author)
						)
						.setPositiveButton(R.string.ok, null).show();
				return true;
			}

		});
		Preference rowRefreshMedia = (Preference) findPreference("refreshMedia");
		if (android.os.Build.VERSION.SDK_INT > 18) {  // in Kitkat and newer intent.action.MEDIA_MOUNTED os blocked
			PreferenceScreen preferenceScreen = getPreferenceScreen();
			preferenceScreen.removePreference(rowRefreshMedia);
		} else {
			rowRefreshMedia.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				public boolean onPreferenceClick(Preference preference) {
					if (android.os.Environment.MEDIA_MOUNTED.equals(android.os.Environment.getExternalStorageState())) {
						IntentFilter mountedMediaFilter;
						mountedMediaFilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_FINISHED);
						mountedMediaFilter.addDataScheme("file");
						mediaIntentReceiver = new MediaIntentReceiver();
						registerReceiver(mediaIntentReceiver, mountedMediaFilter);
						sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
								Uri.parse("file://" + Environment.getExternalStorageDirectory())));
						pDialog = ProgressDialog.show(PreferencesActivity.this, getString(R.string.refreshing_media), getString(R.string.please_wait), true, false);
					} else {
						new AlertDialog.Builder(PreferencesActivity.this)
								.setTitle(R.string.app_name)
								.setMessage(R.string.media_cant_be_refreshed)
								.setPositiveButton(R.string.ok, null).show();
					}
					return true;
				}
			});
		}

	}

	// Toolbar workaround http://stackoverflow.com/questions/26564400/creating-a-preference-screen-with-support-v21-toolbar

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		Toolbar bar;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();
			bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.toolbar_layout, root, false);
			root.addView(bar, 0); // insert at top
		} else {
			ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
			ListView content = (ListView) root.getChildAt(0);
			root.removeAllViews();
			bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.toolbar_layout, root, false);

			int height;
			TypedValue tv = new TypedValue();
			if (getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
				height = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
			}else{
				height = bar.getHeight();
			}

			content.setPadding(0, height, 0, 0);

			root.addView(content);
			root.addView(bar);
		}
		setSupportActionBar(bar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		bar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	public class MediaIntentReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(intent.getAction())) {
				//refreshCountdown.cancel();
				unregisterReceiver(mediaIntentReceiver);
				if (pDialog != null) {
					pDialog.dismiss();
					pDialog = null;
				}
			}

		}
	}
}


	
	
	
	
	
