package org.sergez.splayer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import static org.sergez.splayer.util.PrefsConstants.*;

/**
 * @author Sergii Zhuk
 *         Date: 28.07.13
 *         Time: 16:21
 *
 *         Used to optimize calls to the Prefs as these prefs could be changed only
 *         from prefereces screen and it doesn't make sense to reload them every time
 */
public class UIStateController {

  private boolean showFileExtension;
  private boolean bigFont;
	private boolean readInternalMedia;

	private boolean playOnlyWhenButtonPressed;
	private boolean redLargeElapsedTime;

  public UIStateController(Context context) {;
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    showFileExtension = prefs.getBoolean(PREF_CHECKBOX_SHOW_EXTENSION, false);
    bigFont = prefs.getBoolean(PREF_CHECKBOX_BIG_FONT, false);
    redLargeElapsedTime = prefs.getBoolean(PREF_CHECKBOX_RED_ELAPSED, false);
	  readInternalMedia = prefs.getBoolean(PREF_CHECKBOX_READ_INTERNAL_MEMORY, true);
	  playOnlyWhenButtonPressed =  PrefsController.isPlayOnlyWhenButtonPressed(context);
  }

  public boolean isShowFileExtension() {
    return showFileExtension;
  }

	public boolean isReadInternalMedia(){
		return readInternalMedia;
	}

	public boolean isLargeRedElapsedTime() {
		return redLargeElapsedTime;
	}

	public boolean isBigFont() {
		return bigFont;
	}

	public boolean isPlayOnlyWhenButtonPressed() {
		return playOnlyWhenButtonPressed;
	}


}
