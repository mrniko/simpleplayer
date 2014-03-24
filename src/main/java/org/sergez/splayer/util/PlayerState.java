package org.sergez.splayer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.sergez.splayer.enums.RepeatState;
import org.sergez.splayer.enums.ShuffleState;
import org.sergez.splayer.service.SimplePlayerService;

/**
 * @author Sergii Zhuk
 *         Date: 13.10.13
 *         Time: 12:21
 */
public class PlayerState {
	public static final String PREF_CURRENT_FILE = "playerServiceFile";
	public static final String PREF_CURRENT_FILE_PROGRESS = "playerServiceProgress";
	public static final String PREF_SHUFFLE = "playerShuffleState";
	public static final String PREF_REPEAT = "playerRepeatState";

	public int playerServiceProgress;
	public String playerServiceFile;
	public RepeatState repeatState;
	public ShuffleState shuffleState;

	public static PlayerState loadState(Context context) {
		PlayerState state = new PlayerState();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		state.playerServiceProgress = prefs.getInt(PREF_CURRENT_FILE_PROGRESS, 0);
		state.playerServiceFile = prefs.getString(PREF_CURRENT_FILE, "");
		state.repeatState = RepeatState.values()[prefs.getInt(PREF_REPEAT, RepeatState.NO_REPEAT.ordinal())];
		state.shuffleState = ShuffleState.values()[prefs.getInt(PREF_SHUFFLE, ShuffleState.SHUFFLE_OFF.ordinal())];
		return state;

	}

	public static void saveState(Context context, PlayerState playerState) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PREF_CURRENT_FILE, playerState.playerServiceFile);
		editor.putInt(PREF_CURRENT_FILE_PROGRESS, playerState.playerServiceProgress);
		editor.putInt(PREF_SHUFFLE, playerState.shuffleState.ordinal());
		editor.putInt(PREF_REPEAT, playerState.repeatState.ordinal());
		editor.commit();
	}

	public static void saveState(Context context, SimplePlayerService playerServiceWithState) {
		PlayerState playerState = new PlayerState();
		playerState.repeatState = playerServiceWithState.repeatState;
		playerState.shuffleState = playerServiceWithState.shuffleState;
		playerState.playerServiceFile = playerServiceWithState.getCurrentlyPlayingFilePath();
		playerState.playerServiceProgress = playerServiceWithState.getCurrentPosition();
		saveState(context, playerState);
	}

	private PlayerState() {

	}
}
