package org.sergez.splayer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import static org.sergez.splayer.util.PrefsConstants.*;


/**
 * @author Sergii Zhuk
 *         Date: 15.03.14
 *         Time: 21:18
 */
public final class PrefsController {

    private PrefsController() {

    }

    private static SharedPreferences getPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static boolean isPlayOnlyWhenButtonPressed(Context context) {
        return getPrefs(context).getBoolean(PREF_CHECKBOX_PLAY_ONLY_WHEN_BUTTON_PRESSED, false);
    }

    public static boolean isStopAfterEachFile(Context context) {
        return getPrefs(context).getBoolean(PREF_CHECKBOX_STOP_AFTER_EACH_FILE, false);
    }

    public static boolean isDonationStoppedToShow(Context context) {
        boolean result = getPrefs(context).getBoolean(PREF_STOP_SHOW_DONATION, false);
        return result;
    }

    public static void setDonationStoppedToShow(Context context, boolean stopToShow) {
        getPrefs(context).edit().putBoolean(PREF_STOP_SHOW_DONATION, stopToShow).commit();
    }

    public static boolean isTimeToShowDonationRequest(Context context) {
        long now = System.currentTimeMillis();
        long lastShow = getPrefs(context).getLong(PREF_LAST_DONATION_SHOW, now);
        if (now - lastShow >= Constants.INTERVAL_DAYS_SHOW_DONATE) {
            getPrefs(context).edit().putLong(PREF_LAST_DONATION_SHOW, now).commit();
            return true;
        } else {
            return false;
        }
    }

}
