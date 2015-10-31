package org.sergez.splayer.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import org.sergez.splayer.R;
import org.sergez.splayer.ui.SimplePlayerActivity;
import org.sergez.splayer.enums.RepeatState;
import org.sergez.splayer.enums.ShuffleState;
import org.sergez.splayer.util.FileFormat;
import org.sergez.splayer.util.PlayerState;
import org.sergez.splayer.util.PrefsController;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.sergez.splayer.util.Constants.SERVICE_RESPONSE_FALSE;
import static org.sergez.splayer.util.Utils.makeToast;


public class SimplePlayerService extends Service implements OnErrorListener, OnCompletionListener {
	public static final String ACTION_START = "action.START";
	private static final String TAG = SimplePlayerService.class.getSimpleName();

	private static final int NOTIFY_PLAYING = 1;

	//to send information to Activity via intent
	public static final String ACTION_NOWPLAYING = "ACTION_NOWPLAYING";
    public static final String ACTION_FROM_NOTIF_NEXT = "ACTION_FROM_NOTIF_NEXT";
    public static final String ACTION_FROM_NOTIF_PLAY = "ACTION_FROM_NOTIF_PLAY";
    public static final String ACTION_FROM_NOTIF_PREV = "ACTION_FROM_NOTIF_PREV";
    public static final String ACTION_FROM_NOTIF_PAUSE = "ACTION_FROM_NOTIF_PAUSE";
	public static final String NOWPLAYING_FILEPATH = "NOWPLAYING_FILEPATH";
	public static final String NOWPLAYING_PLAY_CURRENT = "NOWPLAYING_PLAY_CURRENT";
	public static final String NOWPLAYING_PLAYER_STATE = "NOWPLAYING_PLAYER_STATE";


	private final IBinder binder = new ServiceBinder();
	private MediaPlayer mediaPlayer;

	//currently playing filelist
	private List<String> pathPlayingList = null;
	private List<String> copyPathPlayingList = null;

	//position in itemlist for the current and the last files in nowplaying folder
	private int currentFilePos;
	private int lastFilePos;

	public static final int PLAYER_IS_NULL = -2;
	public static final int PLAYER_NOT_READY = -1;
	public static final int PLAYER_INITIALIZED_PAUSED = 0;
	public static final int PLAYER_IS_PLAYING = 1;
	public static final int PLAYER_PAUSED_BY_AFCHANGE = 2;
	public static final int PLAYER_QUIET_CAN_DUCK = 3;
	/**
	 * player state :
	 * -2 = player is null
	 * -1 = not initialized,
	 * 0 = initialized, pause;
	 * 1 = playing;
	 * 2 = short pause caused by sound interruption(e.g. phone call)
	 */
	public int playerState = PLAYER_IS_NULL;
	public RepeatState repeatState;
	public ShuffleState shuffleState;
	private MusicNoisyIntentReceiver musicNoisyIntentReceiver;
    private NotificationActionIntentReceiver notificationActionIntentReceiver;

	public class ServiceBinder extends Binder {
		public SimplePlayerService getService() {
			return SimplePlayerService.this;
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return binder;
	}

	public void onCreate() {
		super.onCreate();
        // helps to remove all notifications when app process was killed by system and then service was restored
        cancelAllNotifications(this);

        //--- for notification buttons intent:
        IntentFilter actionFromNotifFilter;
        actionFromNotifFilter = new IntentFilter(SimplePlayerService.ACTION_FROM_NOTIF_NEXT);
        actionFromNotifFilter.addAction(SimplePlayerService.ACTION_FROM_NOTIF_PAUSE);
        actionFromNotifFilter.addAction(SimplePlayerService.ACTION_FROM_NOTIF_PLAY);
        actionFromNotifFilter.addAction(SimplePlayerService.ACTION_FROM_NOTIF_PREV);
        notificationActionIntentReceiver = new NotificationActionIntentReceiver();
        registerReceiver(notificationActionIntentReceiver, actionFromNotifFilter);
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		if (ACTION_START.equals(intent.getAction())) {
			initService();
		}
		return START_NOT_STICKY;
	}

	public boolean onError(MediaPlayer mp, int what, int extra) {
		Log.e(TAG, "MP error :: what: " + what + ", extra: " + extra);
		makeToast(this, "Simple Player MP error");
		stopForeground(true);
		if (mediaPlayer != null) {
			releasePlayer();
		}
		initMediaPlayer();
		//TODO: send intent to activity
		return false;
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		unregisterAudioNoisyFilter();
		stopForeground(true);
		String result = moveNext(true); // need to update notification here
	}

	@Override
	public void onDestroy() {
		unregisterAudioNoisyFilter();
        if (notificationActionIntentReceiver != null){
            unregisterReceiver(notificationActionIntentReceiver);
        }
		releasePlayer();
        cancelAllNotifications(this);
		super.onDestroy();
	}

	OnAudioFocusChangeListener afChangeListener = new OnAudioFocusChangeListener() {
		public void onAudioFocusChange(int focusChange) {
			if (playerState >= 0) {
				switch (focusChange) {
					case AudioManager.AUDIOFOCUS_GAIN:
						// resume playback
						if (mediaPlayer != null) {
							if (playerState == PLAYER_PAUSED_BY_AFCHANGE) {
								playCurrentFile();
								mediaPlayer.setVolume(1.0f, 1.0f);
							} else if (playerState == PLAYER_QUIET_CAN_DUCK) {
								mediaPlayer.setVolume(1.0f, 1.0f);
								playerState = PLAYER_IS_PLAYING;
							}
						}
						break;
					case AudioManager.AUDIOFOCUS_LOSS:
						// Lost focus for an unbounded amount of time: stop playback and release media player
						if (mediaPlayer.isPlaying()) {
							makePause();
						}
						break;
					case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
						// Lost focus for a short time, but we have to stop
						// playback. We don't release the media player because playback
						// is likely to resume
						if (mediaPlayer.isPlaying()) {
							makeAFChangedPause();
						}
						break;

					case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
						// Lost focus for a short time, but it's ok to keep playing
						// at an attenuated level
						if (mediaPlayer.isPlaying()) {
							mediaPlayer.setVolume(0.1f, 0.1f);
							playerState = PLAYER_QUIET_CAN_DUCK;
						}
						break;
				}
			}
		}
	};

	public void initService() {
		initMediaPlayer();
		lastFilePos = 0;
		currentFilePos = 0;
		pathPlayingList = new ArrayList<String>();
		playerState = PLAYER_NOT_READY;
		repeatState = RepeatState.NO_REPEAT;
		shuffleState = ShuffleState.SHUFFLE_OFF;
	}

	public void initMediaPlayer() {
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnErrorListener(this);
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);

		AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audioManager.requestAudioFocus(afChangeListener,
				// Use the music stream.
				AudioManager.STREAM_MUSIC,
				// Request permanent focus.
				AudioManager.AUDIOFOCUS_GAIN);

	}

	public int getCurrentPosition() {
		if (mediaPlayer != null) {
			if (mediaPlayer.isPlaying() || (playerState >= 0)) {
				return mediaPlayer.getCurrentPosition();
			}
		}
		return 0;
	}

	public void seekTo(int progress) {
		if (playerState >= 0) {
			mediaPlayer.seekTo(progress);
		}
	}

	public int getPlayerState() {
		return playerState;
	}

	public RepeatState getRepeatState() {
		return repeatState;
	}

	public ShuffleState getShuffleState() {
		return shuffleState;
	}

	public void setRepeatState(RepeatState repeatState) {
		this.repeatState = repeatState;
	}

	public void setShuffleState(ShuffleState shuffleState) {
		this.shuffleState = shuffleState;
		processSetShuffleState();
	}

	/**
	 * If user selects file to play in the shuffle mode - all other files have to be reshuffled and current have to be first in the list
	 */
	private void processSetPathPlayingShuffleFolder(String selectedFilepath) { //to process shuffle if moving to next file
		if ((pathPlayingList != null) && (pathPlayingList.size() > 0)) { //creates new shuffled list
			if ((this.shuffleState == ShuffleState.SHUFFLE_ON)) {
				copyPathPlayingList = new ArrayList<String>();
				copyPathPlayingList.addAll(pathPlayingList);
				//after shuffle is enabled - we have to play all files, but start one have to be currently playing
				pathPlayingList.remove(selectedFilepath);
				Collections.shuffle(pathPlayingList);
				pathPlayingList.add(0, selectedFilepath);
				currentFilePos = 0;
			}
		}
	}

	private void processSetShuffleState() {
		if ((pathPlayingList != null) && (pathPlayingList.size() > 0)) { //creates new shuffled list
			if ((this.shuffleState == ShuffleState.SHUFFLE_ON) && (copyPathPlayingList == null)) {
				copyPathPlayingList = new ArrayList<String>();
				copyPathPlayingList.addAll(pathPlayingList);
				String nowPlaying = pathPlayingList.get(currentFilePos);//after shuffle is enabled - we have to play all files, but start one have to be currently playing
				pathPlayingList.remove(currentFilePos);
				Collections.shuffle(pathPlayingList);
				pathPlayingList.add(0, nowPlaying);
				currentFilePos = 0;
			} else {
				if (copyPathPlayingList != null) {
					String nowPlaying = pathPlayingList.get(currentFilePos);//after shuffle is disabled - continue to play from those file in list which was playing
					pathPlayingList = new ArrayList<String>();
					pathPlayingList.addAll(copyPathPlayingList);
					currentFilePos = pathPlayingList.lastIndexOf(nowPlaying);
					copyPathPlayingList = null;
				}
			}
		}
	}


	public String movePrev(boolean fromNotification) {
		if (playerState >= 0) {
			if ((currentFilePos == 0) || (mediaPlayer.getCurrentPosition() > 4001)) { //first file in the folder or rewind to the beginning of current song if more then 1 sec
				File file = new File(pathPlayingList.get(currentFilePos));
				if (!file.isDirectory()) {
					boolean result;
					boolean stopAfterEachFile = PrefsController.isStopAfterEachFile(this);

					if ((playerState == PLAYER_IS_PLAYING) && (!stopAfterEachFile)) {
						result = playFile(file);
					} else if (stopAfterEachFile) {
						result = isWaitUserCommandStartPlay(file);
					} else {
						result = moveToFile(file, fromNotification);
					}
					if (result) {
						return file.getPath();
					} else {
						return SERVICE_RESPONSE_FALSE; //failed to play file
					}
				}
			} else {
				while (currentFilePos > 0) {
					currentFilePos -= 1;
					File file = new File(pathPlayingList.get(currentFilePos));
					if (!file.isDirectory()) {
						boolean result;
						boolean stopAfterEachFile = PrefsController.isStopAfterEachFile(this);
						if ((playerState == PLAYER_IS_PLAYING) && (!stopAfterEachFile)) {
							result = playFile(file);
						} else if (stopAfterEachFile) {
							result = isWaitUserCommandStartPlay(file);
						} else {
							result = moveToFile(file, fromNotification);
						}
						if (result) {
							return file.getPath();
						} else {
							return SERVICE_RESPONSE_FALSE;
						}
					}
				}
			}
		}
		return null;
	}

	public String moveNext(boolean fromNotification) {
		if ((playerState >= 0) && pathPlayingList.size() > 0) { //ifPathPlaying is empty - nothing to play
			if (repeatState == RepeatState.NO_REPEAT) {
				while ((currentFilePos < lastFilePos)) {
					currentFilePos += 1;
					File file = new File(pathPlayingList.get(currentFilePos));
					if (!file.isDirectory()) {
						boolean result;
						boolean stopAfterEachFile = PrefsController.isStopAfterEachFile(this);
						if ((playerState == PLAYER_IS_PLAYING) && (!stopAfterEachFile)) {
							result = playFile(file);
						} else if (stopAfterEachFile) {
							result = isWaitUserCommandStartPlay(file);
						} else {
							result = moveToFile(file, fromNotification);
						}
						if (result) {
							return file.getPath();
						} else {
							return SERVICE_RESPONSE_FALSE;
						}
					}
				}
				//probably it was the last file - move to it and make pause, but keep do it only if player was paused
				if ((currentFilePos >= lastFilePos) && (pathPlayingList.size() > 0) && (!mediaPlayer.isPlaying())) {
					File file = new File(pathPlayingList.get(currentFilePos));
					boolean result;
					result = moveToFile(file, fromNotification);
					if (result) {
						foregroundNotification("", getString(R.string.end_of_the_playlist_reached));
						return file.getPath();
					} else {
						return SERVICE_RESPONSE_FALSE;
					}
				}
			} else if (repeatState == RepeatState.REPEAT_CURRENT_TRACK) {
				File file = new File(pathPlayingList.get(currentFilePos));
				if (!file.isDirectory()) {
					boolean result;
					boolean stopAfterEachFile = PrefsController.isStopAfterEachFile(this);
					if ((playerState == PLAYER_IS_PLAYING) && (!stopAfterEachFile)) {
						result = playFile(file);
					} else if (stopAfterEachFile) {
						result = isWaitUserCommandStartPlay(file);
					} else {
						result = moveToFile(file, fromNotification);
					}
					if (result) {
						return file.getPath();
					} else {
						return SERVICE_RESPONSE_FALSE;
					}
				}
			} else if (repeatState == RepeatState.REPEAT_ALL_FILES) {
				while (true) {
					currentFilePos += 1;
					if (currentFilePos > lastFilePos) {
						currentFilePos = 0;
					}
					File file = new File(pathPlayingList.get(currentFilePos));
					if (!file.isDirectory()) {
						boolean result;
						boolean stopAfterEachFile = PrefsController.isStopAfterEachFile(this);

						if ((playerState == PLAYER_IS_PLAYING) && (!stopAfterEachFile)) {
							result = playFile(file);
						} else if (stopAfterEachFile) {
							result = isWaitUserCommandStartPlay(file);
						} else {
							result = moveToFile(file, fromNotification);
						}
						if (result) {
							return file.getPath();
						} else {
							return SERVICE_RESPONSE_FALSE;
						}
					}
				}
			}
		}
		return null;
	}

	public boolean playFile(int filePos) { //filePos - position of file in internal SimplePlayerService memory
		File file = new File(pathPlayingList.get(filePos));
		if (!file.isDirectory()) {
			playFile(file);
			currentFilePos = filePos;
			return true;
		}
		return false;
	}

	public void playCurrentFile() {
		if (mediaPlayer != null) {
			File file = new File(pathPlayingList.get(currentFilePos));
			if (mediaPlayer.getCurrentPosition() != 0) {
				unregisterAudioNoisyFilter();//to ensure that if was previously unregistred
				registerAudioNoisyFilter();
				mediaPlayer.start();
				playerState = 1;
				foregroundNotification("", file.getName());
				sendIntentToSPActivity(file.getAbsolutePath(), true, playerState); //nowplaying haven't been changed, but this action makes activity to refresh scrollbar and song time

			} else {
				if (!file.isDirectory() && FileFormat.acceptableFormat(file.getName())) {
					playFile(file);
				}
			}
		}
	}

	/**
	 * Prepares file to playback
	 *
	 * @param file
	 * @return
	 */
	public boolean moveToFile(File file, boolean fromNotification) {
		if (mediaPlayer == null) {
			return false;
		}
		if (FileFormat.acceptableFormat(file.getName())) {
			try {
				mediaPlayer.reset();
				mediaPlayer.setDataSource(this, Uri.parse(file.getAbsolutePath()));
				mediaPlayer.prepare();
				currentFilePos = pathPlayingList.lastIndexOf(file.getPath());
				playerState = PLAYER_INITIALIZED_PAUSED;
                if(fromNotification) {
                    foregroundNotification("", file.getName());
                }
				sendIntentToSPActivity(file.getAbsolutePath(), false, playerState);
				return true;
			} catch (IOException e) {
				Log.e(TAG, e.getMessage(), e);
				playerState = PLAYER_NOT_READY;
				return false;
			}
		}
		return false;
	}

	/**
	 * To be called if 'checkboxStopAfterEachFile' mode is true
	 */
	public boolean isWaitUserCommandStartPlay(File file) {
		if (moveToFile(file, false)) { // set 'false' here because it has its own notification
			unregisterAudioNoisyFilter();//to ensure that it was previously unregistered
			playerState = PLAYER_INITIALIZED_PAUSED;
			foregroundNotification("Going to play: ", file.getName());  // TODO to res
			return true;
		} else {
			return false;
		}
	}


	public boolean playFile(File file) {
		if (moveToFile(file, false)) {
			unregisterAudioNoisyFilter();//to ensure that it was previously unregistred
			registerAudioNoisyFilter();
			mediaPlayer.start();
			playerState = PLAYER_IS_PLAYING;
			foregroundNotification("", file.getName());
			sendIntentToSPActivity(file.getAbsolutePath(), false, playerState);
			currentFilePos = pathPlayingList.lastIndexOf(file.getPath());
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Axiom here: if musicNoisyIntentReceiver!=null - it's registered as receiver
	 */
	private void registerAudioNoisyFilter() {
		IntentFilter audioNoisyFilter;
		audioNoisyFilter = new IntentFilter(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY);
		musicNoisyIntentReceiver = new MusicNoisyIntentReceiver();
		registerReceiver(musicNoisyIntentReceiver, audioNoisyFilter);
	}

	private void unregisterAudioNoisyFilter() {
		if (musicNoisyIntentReceiver != null) {
			unregisterReceiver(musicNoisyIntentReceiver);
			musicNoisyIntentReceiver = null;
		}
	}

	public void makePause() {
		unregisterAudioNoisyFilter();
		mediaPlayer.pause();
		playerState = PLAYER_INITIALIZED_PAUSED;
		sendIntentToSPActivity(pathPlayingList.get(currentFilePos), true, playerState);
        // needed to show 'play' button in notification:
        foregroundNotification("", new File(pathPlayingList.get(currentFilePos)).getName());
		PlayerState.saveState(this);
	}

	public void makeAFChangedPause() { //pause when audiofocus has been changed
		//unregisterAudioNoisyFilter(); don't need to - because after call receive need to play again
		mediaPlayer.pause();
		playerState = PLAYER_PAUSED_BY_AFCHANGE;
		sendIntentToSPActivity(pathPlayingList.get(currentFilePos), true, playerState);
	}

	public void releasePlayer() {
		unregisterAudioNoisyFilter();
		try {
			if (mediaPlayer != null) {
				mediaPlayer.release();
			}
		} finally {
			mediaPlayer = null;
		}
		playerState = PLAYER_IS_NULL;
	}

	public boolean isPlaying() {
		if (mediaPlayer != null) {
			return mediaPlayer.isPlaying();
		} else {
			return false;
		}
	}

	/**
	 * @param pathPlaying
	 * @param selectedFilepath have to be set for proper shuffling if shufflemode is ON
	 */
	public void setPathPlaying(List<String> pathPlaying, String selectedFilepath) {
		this.pathPlayingList = new ArrayList<String>();
		this.pathPlayingList.addAll(pathPlaying);
		lastFilePos = this.pathPlayingList.size() - 1;
		processSetPathPlayingShuffleFolder(selectedFilepath);
	}

	public int getDuration() {
		int duration = 0;
		try {
			duration = mediaPlayer.getDuration();
		} catch (Exception e) {
			Log.e(TAG, "Get Duration: " + e.getMessage(), e);
		}
		return duration;
	}

	public String getCurrentlyPlayingFilePath() {
		String result = "";
		if (playerState >= 0) {
			if ((pathPlayingList.size() > currentFilePos) && (currentFilePos >= 0)) {
				result = pathPlayingList.get(currentFilePos);
			} else {
				Log.e(TAG, "Attempt to getCurrentlyPlayingFilePath() for pathPlayingList.size="
						+ pathPlayingList.size() + " with currentFilePos=" + currentFilePos);
				makeToast(this, "Error on locating currently playing file"); // TODO res
			}
		}
		return result;
	}

	private void sendIntentToSPActivity(String filePath, boolean playCurrent, int state) {
		Intent intent = new Intent(ACTION_NOWPLAYING);
		intent.putExtra(NOWPLAYING_FILEPATH, filePath);
		intent.putExtra(NOWPLAYING_PLAY_CURRENT, playCurrent);
		intent.putExtra(NOWPLAYING_PLAYER_STATE, state);
		sendBroadcast(intent);
	}

	public void foregroundNotification(String textStatus, String textFilename) {

        PendingIntent pIntent =  PendingIntent.getActivity(this, 0,
                new Intent(this, SimplePlayerActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_start_simpleplayer)
                        // Large icon won't be available on API<11 devices
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(textStatus + textFilename)
                .setTicker(textStatus + textFilename)
                        // Set PendingIntent into Notification
                .setContentIntent(pIntent);

        // TODO Set RemoteViews into Notification and image

        Intent movePrev = new Intent(ACTION_FROM_NOTIF_PREV);
        PendingIntent pendingPrevIntent = PendingIntent.getBroadcast(this, 0, movePrev, 0);
        builder.addAction(R.drawable.ic_media_previous, "", pendingPrevIntent);

        if (playerState > 0) {
            Intent pause = new Intent(ACTION_FROM_NOTIF_PAUSE);
            PendingIntent pendingPauseIntent = PendingIntent.getBroadcast(this, 0, pause, 0);
            builder.addAction(R.drawable.ic_media_pause, "", pendingPauseIntent);
        } else {
            Intent play = new Intent(ACTION_FROM_NOTIF_PLAY);
            PendingIntent pendingPlayIntent = PendingIntent.getBroadcast(this, 0, play, 0);
            builder.addAction(R.drawable.ic_media_play, "", pendingPlayIntent);
        }
        Intent moveNext = new Intent(ACTION_FROM_NOTIF_NEXT);
        PendingIntent pendingNextIntent = PendingIntent.getBroadcast(this, 0, moveNext, 0);
        builder.addAction(R.drawable.ic_media_next, "", pendingNextIntent);

		startForeground(NOTIFY_PLAYING, builder.build());
	}

	public class MusicNoisyIntentReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context ctx, Intent intent) {
			if (android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
				if ((mediaPlayer != null) & (mediaPlayer.isPlaying())) {
					makePause();
				}
			}
		}
	}

	/**
	 * Call when SDCard is going to be unmounted (React on intent ACTION_MEDIA_EJECT)
	 */
	public void unmountPathPlaying() {
		releasePlayer();
		lastFilePos = 0;
		currentFilePos = 0;
		pathPlayingList = new ArrayList<String>();
		playerState = PLAYER_NOT_READY;
	}

    public static void cancelAllNotifications(Context ctx) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) ctx.getSystemService(ns);
        nMgr.cancelAll();
    }


    public class NotificationActionIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SimplePlayerService.ACTION_FROM_NOTIF_NEXT.equals(intent.getAction())){
                moveNext(true);
            } else if (SimplePlayerService.ACTION_FROM_NOTIF_PREV.equals(intent.getAction())){
                movePrev(true);
            } else if (SimplePlayerService.ACTION_FROM_NOTIF_PAUSE.equals(intent.getAction())){
                makePause();
            } else if (SimplePlayerService.ACTION_FROM_NOTIF_PLAY.equals(intent.getAction())) {
                try {
                    playCurrentFile();
                } catch (IllegalStateException e) {
                    makePause();
                    Log.e(TAG, "", e);
                }
            }
        }
    }
}
