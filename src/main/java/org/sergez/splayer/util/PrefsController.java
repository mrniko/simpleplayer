package org.sergez.splayer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import static org.sergez.splayer.util.PrefsConstants.PREF_CHECKBOX_PLAY_ONLY_WHEN_BUTTON_PRESSED;
import static org.sergez.splayer.util.PrefsConstants.PREF_CHECKBOX_STOP_AFTER_EACH_FILE;


/**
 * @author Sergii Zhuk
 *         Date: 15.03.14
 *         Time: 21:18
 */
public final class PrefsController {

	private PrefsController(){

	}

	private static SharedPreferences getPrefs(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context);
	}

	public static boolean isPlayOnlyWhenButtonPressed(Context context){
		return getPrefs(context).getBoolean(PREF_CHECKBOX_PLAY_ONLY_WHEN_BUTTON_PRESSED, false);
	}

	public static boolean isStopAfterEachFile(Context context){
		return getPrefs(context).getBoolean(PREF_CHECKBOX_STOP_AFTER_EACH_FILE, false);
	}



}
