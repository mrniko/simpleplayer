package org.sergez.splayer.ui;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;

import org.sergez.splayer.R;

/**
 * @author Sergii Zhuk
 *         Date: 31.10.2015
 *         Time: 21:15
 */

public class PreferencesFragment extends PreferenceFragmentCompat {
	private final static String TAG = PreferencesActivity.class.getSimpleName();
	private ProgressDialog pDialog;
	private MediaIntentReceiver mediaIntentReceiver;

	@Override
	public void onCreatePreferences(Bundle bundle, String s) {
		addPreferencesFromResource(R.xml.preferences);

		Preference rowAbout = findPreference("showAbout");   // TODO move to constants
		rowAbout.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				PackageInfo pInfo = null;
				String version = null;
				try {
					pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
					version = pInfo.versionName;
				} catch (PackageManager.NameNotFoundException e) {
					Log.e(TAG, e.getMessage(), e);
					version = "?";
				}

				String title = getResources().getString(R.string.about_title) + " " + getResources().getString(R.string.app_name);
				new AlertDialog.Builder(getActivity())
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
			rowRefreshMedia.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				public boolean onPreferenceClick(Preference preference) {
					if (android.os.Environment.MEDIA_MOUNTED.equals(android.os.Environment.getExternalStorageState())) {
						IntentFilter mountedMediaFilter;
						mountedMediaFilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_FINISHED);
						mountedMediaFilter.addDataScheme("file");
						mediaIntentReceiver = new MediaIntentReceiver();
						getActivity().registerReceiver(mediaIntentReceiver, mountedMediaFilter);
						getActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
								Uri.parse("file://" + Environment.getExternalStorageDirectory())));
						pDialog = ProgressDialog.show(getActivity(), getString(R.string.refreshing_media), getString(R.string.please_wait), true, false);
					} else {
						new AlertDialog.Builder(getActivity())
								.setTitle(R.string.app_name)
								.setMessage(R.string.media_cant_be_refreshed)
								.setPositiveButton(R.string.ok, null).show();
					}
					return true;
				}
			});
		}
	}

	public class MediaIntentReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(intent.getAction())) {
				//refreshCountdown.cancel();
				getActivity().unregisterReceiver(mediaIntentReceiver);
				if (pDialog != null) {
					pDialog.dismiss();
					pDialog = null;
				}
			}

		}
	}
}

