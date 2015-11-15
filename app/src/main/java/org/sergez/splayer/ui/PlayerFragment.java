package org.sergez.splayer.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.sergez.splayer.R;
import org.sergez.splayer.service.SimplePlayerService;
import org.sergez.splayer.util.DurationAlbumID;
import org.sergez.splayer.util.MediaFileData;
import org.sergez.splayer.util.MediaFileUtil;
import org.sergez.splayer.util.PlayerState;
import org.sergez.splayer.util.UIStateController;
import org.sergez.splayer.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.sergez.splayer.util.Constants.ROOT_PATH;
import static org.sergez.splayer.util.Constants.SERVICE_RESPONSE_FALSE;
import static org.sergez.splayer.util.DialogUtils.showFileCantBePlayed;
import static org.sergez.splayer.util.DialogUtils.showFolderCantBeRead;
import static org.sergez.splayer.util.Utils.makeToast;

/**
 * @author Sergii Zhuk
 *         Date: 31.10.2015
 *         Time: 21:14
 */

public class PlayerFragment extends android.support.v4.app.Fragment {

	private static final String TAG = SimplePlayerActivity.class.getSimpleName();
	private Button buttonPlayStop;
	private Button buttonNext;
	private Button buttonPrev;
	private MenuItem menuItemRepeat;
	private MenuItem menuItemShuffle;
	private SeekBar seekBar;
	private TextView textPath;
	private TextView textCurrentTime;
	private TextView textLeftTime;
	private TextView textTrackName;
	private TextView textArtist;
	private LinearLayout trackInfoLayout;
	private LinearLayout timesLayout;
	private boolean externalMediaMounted;
	private boolean readInternalMedia;
	private ListView listView;

	private final Handler mHandler = new Handler();

	private boolean mIsBound;

	private PlayFileAdapter fileListAdapter;
	private PlayerServiceIntentReceiver playerServiceIntentReceiver;
	private MediaIntentReceiver mediaIntentReceiver;

	ListData listData;
	UIStateController uiStateController;
	SimplePlayerService playerService;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.main, container, false);
		setHasOptionsMenu(true);
		initComponents(rootView);
		startPlayerService();
		return rootView;
	}


	public class PlayerServiceIntentReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {

			if (SimplePlayerService.ACTION_NOWPLAYING.equals(intent.getAction())) {
				int playerState = intent.getIntExtra(
						SimplePlayerService.NOWPLAYING_PLAYER_STATE, -100);
				if (playerState > 0) {// playing
					boolean playCurrent = intent.getBooleanExtra(SimplePlayerService.NOWPLAYING_PLAY_CURRENT, false);
					if (playCurrent == true) {
						playCurrentFileOperations();
						startPlayProgressUpdater();
					} else {
						String filePath = intent.getStringExtra(SimplePlayerService.NOWPLAYING_FILEPATH);
						playFileOperations(filePath);
						startPlayProgressUpdater();
						fileListAdapter.notifyDataSetChanged();
					}
				} else if (playerState == 0) {// pause or being ready to play
					String filePath = intent
							.getStringExtra(SimplePlayerService.NOWPLAYING_FILEPATH);
					playFileOperations(filePath);
					startPlayProgressUpdater();// need to set progress to 0 e.g.
					// when moves to next file in pause state
					fileListAdapter.notifyDataSetChanged();
				} else if (playerState < 0) {// player error
					// TODO: process player state in playing mode
					Log.e(TAG, PlayerServiceIntentReceiver.class.getSimpleName() + " playerState: " + playerState);
				}
			}
		}
	}

	public class MediaIntentReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (Intent.ACTION_MEDIA_EJECT.equals(intent.getAction())) {
				PlayerFragment.this.onMediaEject();
			} else if (Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(intent.getAction())) {
				onMediaReady();
			} else {
				Log.e(TAG, "Unexpected mediaintent received");
			}
		}
	}

	@Override
	public void onStop() {
		if ((playerService != null) && (playerService.playerState < 1)) { //if player isn't playing
			PlayerState.saveState(playerService);
			getActivity().stopService(new Intent(getActivity().getApplicationContext(), SimplePlayerService.class));
			doUnbindService();
			playerService = null;
		}
		mHandler.removeCallbacksAndMessages(null);
		super.onStop();
	}

	@Override
	public void onPause() {
		super.onPause();
		getActivity().unregisterReceiver(mediaIntentReceiver);
		getActivity().unregisterReceiver(playerServiceIntentReceiver);
	}

	@Override
	public void onStart() {
		super.onStart();
		uiStateController = new UIStateController(getActivity());
		applyPrefs();
	}


	private void applyPrefs() {
		boolean oldReadInternalMedia = readInternalMedia;
		readInternalMedia = uiStateController.isReadInternalMedia();
		if (oldReadInternalMedia != readInternalMedia) {
			if (readInternalMedia) {
				listData.setRoot(ROOT_PATH);
			} else {
				listData.setRoot(Environment.getExternalStorageDirectory().getAbsolutePath());
			}
			Utils.moveToFolder(getActivity(), listData.getRoot(), null, false, listData, externalMediaMounted, readInternalMedia);
			updateFileListAdapter(listData.getCurrentPathShowItems());
			// list of items was taken from moveToFirstAvailableFoldersHashSet
			textPath.setText(getString(R.string.location) + listData.getCurrentPath());
		}

		if (uiStateController.isLargeRedElapsedTime()) {
			textCurrentTime.setTextColor(getResources().getColor(R.color.elapsed_time_red));
			float dimenValueBig = getResources().getDimensionPixelSize(R.dimen.elapsed_time_big);
			textCurrentTime.setTextSize(TypedValue.COMPLEX_UNIT_PX, dimenValueBig);
		} else {
			textCurrentTime.setTextColor(getResources().getColor(R.color.elapsed_time_normal));
			float dimenValueStd = getResources().getDimensionPixelSize(R.dimen.elapsed_time_std);
			textCurrentTime.setTextSize(TypedValue.COMPLEX_UNIT_PX, dimenValueStd);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		IntentFilter ejectMediaFilter = new IntentFilter(Intent.ACTION_MEDIA_EJECT);
		ejectMediaFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
		ejectMediaFilter.addDataScheme("file");
		mediaIntentReceiver = new MediaIntentReceiver();
		getActivity().registerReceiver(mediaIntentReceiver, ejectMediaFilter);
		checkIfMediaMounted();
		if (fileListAdapter != null) {
			fileListAdapter.notifyDataSetChanged();
		}
		// intent to receive data from SimplePlayerService
		IntentFilter mpFileChangedFilter;
		mpFileChangedFilter = new IntentFilter(SimplePlayerService.ACTION_NOWPLAYING);
		playerServiceIntentReceiver = new PlayerServiceIntentReceiver();
		getActivity().registerReceiver(playerServiceIntentReceiver, mpFileChangedFilter);
	}

//	@Override
//	public void onRestart() { //TODO
//		super.onRestart();
//		if (playerService == null) {
//			startPlayerService();
//			//all other things for player init we have to do in onServiceConnected()
//		} else {
//			playFileOperations(playerService.getCurrentlyPlayingFilePath());
//			updateTrackTime();
//		}
//	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// TODO Add your menu entries here
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.action_menu, menu);
		menuItemRepeat = menu.findItem(R.id.menu_repeat);
		menuItemShuffle = menu.findItem(R.id.menu_shuffle);
		setMenuRepeatShuffleTitlesFromService();
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			playerService = ((SimplePlayerService.ServiceBinder) service)
					.getService();
			PlayerState playerState = PlayerState.loadState(getActivity());
			restoreLastPlayerState(playerState);
		}

		public void onServiceDisconnected(ComponentName className) {
			playerService = null;
			mIsBound = false;
		}
	};

	/**
	 * Restores last played track position, folder, etc
	 */
	private void restoreLastPlayerState(PlayerState playerState) {
		//if we're taking stored file after onStop()
		if ((playerState.playerServiceFile != null) && ((playerState.playerServiceFile.length() > 0))) {
			File file = new File(playerState.playerServiceFile);
			if (file.exists()) {
				String parentFilePath = file.getParent();
				if (parentFilePath != null) {
					//loads last played file to player, and loads filelist of current folder
					if (!parentFilePath.equals(listData.getCurrentPath())) {
						Utils.moveToFolder(getContext(), parentFilePath, null, true, listData, externalMediaMounted, readInternalMedia);
					}
					playerService.setPathPlaying(listData.getCurrentPathPlayableList(), file.getAbsolutePath());
					if (playerService.moveToFile(file, true)) {
						playerService.seekTo(playerState.playerServiceProgress);
					} else {
						Log.e(TAG, "Unable move to file: " + file.getAbsolutePath());
						showFileCantBePlayed(getActivity(), file);
					}
				} else {
					Utils.moveToFolder(getContext(), listData.getRoot(), null, false, listData, externalMediaMounted, readInternalMedia);
				}
			} else {
				//unable to find selected file
				Utils.moveToFolder(getContext(), listData.getRoot(), null, false, listData, externalMediaMounted, readInternalMedia);
				hideTimesAndSeekInfo();
			}

		} else {//no defaults stored, start from root folder
			//unable to find selected file
			Utils.moveToFolder(getContext(), listData.getRoot(), null, false, listData, externalMediaMounted, readInternalMedia);
			hideTimesAndSeekInfo();
		}
		playerService.setRepeatState(playerState.repeatState);
		playerService.setShuffleState(playerState.shuffleState);
		updateFileListAdapter(listData.getCurrentPathShowItems());
		textPath.setText(getString(R.string.location) + listData.getCurrentPath());
		setMenuRepeatShuffleTitlesFromService();
	}

	/**
	 * Could be called from init of menu and restore last player state
	 */
	private void setMenuRepeatShuffleTitlesFromService() {
		if ((menuItemRepeat != null) && (menuItemShuffle != null) && (playerService != null)) {
			menuItemShuffle.setTitle(getString(R.string.button_and_toast_shuffle_first_part) + " " + playerService.getShuffleState().getLabel());
			menuItemRepeat.setTitle(getString(R.string.button_and_toast_repeat_first_part) + " " + playerService.getRepeatState().getLabel());
		}
	}

	void doBindService() {
		Intent intent = new Intent(getActivity(), SimplePlayerService.class);
		intent.setAction("Action.DOBIND");// TODO to const
		getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	void doUnbindService() {
		if (mIsBound) {
			// Detach our existing connection.
			getActivity().unbindService(mConnection);
			mIsBound = false;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return Utils.menuClick(item, this, playerService);
	}


	/**
	 *
	 * @return true if event was consumed by fragment
	 */
	public boolean onBackPressed() {
		// check if we are in root folder - do nothing
		if ((listData.getCurrentPathShowItemsSize() > 0)
				&& (listData.getCurrentPathShowKey(0).equals("../"))) {
			File file = new File(listData.getCurrentPathItemsFullpath(0));
			if (file.isDirectory()) {
				if (file.canRead()) {
					listData.setCurrentPath(listData.getCurrentPathItemsFullpath(0));
					Utils.moveToFolder(getActivity(), listData.getCurrentPath(), null, true, listData, externalMediaMounted, readInternalMedia);
					updateFileListAdapter(listData.getCurrentPathShowItems());
					textPath.setText(getString(R.string.location) + listData.getCurrentPath());
					moveToPrevPos(listData.getPrevListViewPosition());
				} else {
					showFolderCantBeRead(getActivity(), file);
				}
			}
			return true;
		} else if (playerService.getPlayerState() > 0) {
			// if player is playing - just return to home screen
			Intent i = new Intent();
			i.setAction(Intent.ACTION_MAIN);
			i.addCategory(Intent.CATEGORY_HOME);
			this.startActivity(i);
			return true;
		} else {
			return false;
		}
	}

	private void moveToPrevPos(List<Integer> prevListViewPos) {
		int prevPos = prevListViewPos.get(prevListViewPos.size() - 1);//gets the last element
		if (prevPos != -1) {
			listView.smoothScrollToPosition(prevPos);
			prevListViewPos.remove(prevListViewPos.size() - 1);
		}
	}

	private void startPlayerService() {
		Intent music = new Intent();
		music.setAction(SimplePlayerService.ACTION_START);
		music.setClass(getActivity(), SimplePlayerService.class);
		getActivity().startService(music);
		doBindService();
	}


	private void initComponents(View rootView) {
		listView = (ListView) rootView.findViewById(R.id.list_main);
		listView.setOnItemClickListener(listClickListener);
		listData = new ListData();
		textPath = (TextView) rootView.findViewById(R.id.text_path);
		buttonPlayStop = (Button) rootView.findViewById(R.id.button_playstop);
		buttonPlayStop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				buttonPlayStopClick(false);
			}
		});

		buttonNext = (Button) rootView.findViewById(R.id.button_next);
		buttonNext.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				buttonNextClick();
			}
		});

		buttonPrev = (Button) rootView.findViewById(R.id.button_prev);
		buttonPrev.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				buttonPrevClick();
			}
		});

		seekBar = (SeekBar) rootView.findViewById(R.id.seekbar_main);
		seekBar.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				seekChange(v);
				return false;
			}
		});

		textCurrentTime = (TextView) rootView.findViewById(R.id.text_current_tracktime);
		textLeftTime = (TextView) rootView.findViewById(R.id.text_left_tracktime);
		textTrackName = (TextView) rootView.findViewById(R.id.text_tracktime);
		textArtist = (TextView) rootView.findViewById(R.id.text_trackartist);

		trackInfoLayout = (LinearLayout) rootView.findViewById(R.id.layout_trackinfo);

		timesLayout = (LinearLayout) rootView.findViewById(R.id.layout_time);
		hideTimesAndSeekInfo();

		listData.clearCurrentPathShowItems();
		listData.clearCurrentPathItemsFullPath();

		checkIfMediaMounted();

		if (externalMediaMounted == true) {
			if (!readInternalMedia) {
				listData.setRoot(Environment.getExternalStorageDirectory().getAbsolutePath());
			} else {
				listData.setRoot(ROOT_PATH);
			}
		} else {
			listData.setRoot(ROOT_PATH);
		}

	}

	private void checkIfMediaMounted() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)
				|| Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			externalMediaMounted = true;
		} else {
			externalMediaMounted = false;
		}
	}

	private void updateTrackTime() {
		String leftTimeMMSS = "-"
				+ Utils.timeMSSFormat(playerService.getDuration() - playerService.getCurrentPosition());
		String currentTimeMMSS = Utils
				.timeMSSFormat(playerService.getCurrentPosition());
		textCurrentTime.setText(currentTimeMMSS);
		textLeftTime.setText(leftTimeMMSS);
	}

	public void startPlayProgressUpdater() {
		if ((playerService != null) && (playerService.getPlayerState() >= 0)) {
			seekBar.setProgress(playerService.getCurrentPosition());
			updateTrackTime();
			if (playerService.isPlaying()) {
				Runnable notification = new Runnable() {
					public void run() {
						startPlayProgressUpdater();
					}
				};
				mHandler.postDelayed(notification, 500);
			} else {
				seekBar.setProgress(playerService.getCurrentPosition());
			}
		}
	}

	// This is event mHandler thumb moving event
	private void seekChange(View v) {
		if (playerService.getPlayerState() >= 0) {
			SeekBar sb = (SeekBar) v;
			playerService.seekTo(sb.getProgress());
			updateTrackTime();
		}
	}

	private void buttonPlayStopClick(boolean calledFromNotification) {
		if (playerService.getPlayerState() == SimplePlayerService.PLAYER_INITIALIZED_PAUSED) {
			try {
				playerService.playCurrentFile();
				buttonPlayStop.setText(getString(R.string.button_pause));
				startPlayProgressUpdater();
			} catch (IllegalStateException e) {
				playerService.makePause();
				Log.e(TAG, "", e);
			}
		} else if (playerService.getPlayerState() == SimplePlayerService.PLAYER_NOT_READY) {
			// try to play first music file in current folder
			if (listData.getCurrentPathPlayableListSize() > 0) {
				File file = new File(listData.getCurrentPathPlayableListItem(0));
				startPlayingFile(file);
			} else {
				makeToast(getActivity(), getString(R.string.unable_start_playing));
			}
		} else if (playerService.getPlayerState() == SimplePlayerService.PLAYER_IS_PLAYING) {
			playerService.makePause();
			if (!calledFromNotification) {
				 clearNotifications();
			}
		}
	}

	private void buttonPrevClick() {
		String path = playerService.movePrev(true);
		if (path != null) {
			if (SERVICE_RESPONSE_FALSE.equals(path)) {
				showFileCantBePlayed(getActivity(), playerService.getCurrentlyPlayingFilePath());
				hideTimesAndSeekInfo();
			} else {
				playFileOperations(path);
				fileListAdapter.notifyDataSetChanged();
			}
		} else if (!TextUtils.isEmpty(playerService.getCurrentlyPlayingFilePath())) {
			makeToast(getActivity(), getString(R.string.play_first_file_in_list));
		} else {
			makeToast(getActivity(), getString(R.string.unable_move_prev));
		}
	}

	private void buttonNextClick() {
		String path = playerService.moveNext(true);
		if (path != null) {
			if (SERVICE_RESPONSE_FALSE.equals(path)) {
				showFileCantBePlayed(getActivity(), playerService.getCurrentlyPlayingFilePath());
				hideTimesAndSeekInfo();
			} else {
				playFileOperations(path);
				fileListAdapter.notifyDataSetChanged();
			}
		} else if (!TextUtils.isEmpty(playerService.getCurrentlyPlayingFilePath())) {
			makeToast(getActivity(), getString(R.string.playing_lastfile_inlist));
		} else {
			makeToast(getActivity(), getString(R.string.unable_mv_next));
		}
	}

	private void hideTimesAndSeekInfo() {
		timesLayout.setVisibility(View.GONE);
		trackInfoLayout.setVisibility(View.GONE);
		seekBar.setVisibility(View.GONE);
	}

	private Runnable mHidePathText = new Runnable() {
		public void run() {
			textPath.setVisibility(View.GONE);
		}
	};

	private void updateFileListAdapter(Map<String, DurationAlbumID> listItems) {
		textPath.setVisibility(View.VISIBLE);

		fileListAdapter = new PlayFileAdapter(this, R.layout.row, listItems);

		listView.setAdapter(fileListAdapter);

		mHandler.removeCallbacks(mHidePathText);
		mHandler.postDelayed(mHidePathText, 4000);
	}


	private AdapterView.OnItemClickListener listClickListener = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> listView, View v, int position, long id) {
			File file = new File(listData.getCurrentPathItemsFullpath(position));
			if (file.isDirectory()) {
				if (file.canRead()) {
					String prevPath = new String(listData.getCurrentPath());
					listData.setCurrentPath(listData.getCurrentPathItemsFullpath(position));
					Utils.moveToFolder(getActivity(), listData.getCurrentPath(), null, true, listData, externalMediaMounted, readInternalMedia);
					updateFileListAdapter(listData.getCurrentPathShowItems());
					textPath.setText(getString(R.string.location) + listData.getCurrentPath());
					if (listData.getCurrentPath().length() >= prevPath.length()) {
						//if move to subfolder
						listData.getPrevListViewPosition().add(position);
					} else {
						//if move to parent folder
						moveToPrevPos(listData.getPrevListViewPosition());
					}
				} else {
					showFolderCantBeRead(getActivity(), file);
				}
			} else { // file was selected
				startPlayingFile(file);
			}
		}
	};

	private void startPlayingFile(File file) {
		playerService.setPathPlaying(listData.getCurrentPathPlayableList(), file.getAbsolutePath());
		boolean waitUserCommandStartPlay = uiStateController.isPlayOnlyWhenButtonPressed();
		if (waitUserCommandStartPlay) {
			if (!playerService.isWaitUserCommandStartPlay(file)) {
				showFileCantBePlayed(getActivity(), file);
				hideTimesAndSeekInfo();
			}
		} else {
			if (!playerService.playFile(file)) {
				showFileCantBePlayed(getActivity(), file);
				hideTimesAndSeekInfo();
			}
		}
	}

	private void playFileOperations(String filePath) {
		playFileOperations(new File(filePath));
	}

	private void playCurrentFileOperations() {
		startPlayProgressUpdater();
		buttonPlayStop.setText(R.string.button_pause);
	}


	/**
	 * Remove all notification if player is in pause mode or not ready
	 */
	void clearNotifications() {
		if ((playerService != null) && (playerService.playerState < 1)) {
			SimplePlayerService.cancelAllNotifications(getActivity());
		}
	}

	private void playFileOperations(File file) {
		if (playerService != null) {
			if (timesLayout.getVisibility() == LinearLayout.GONE) {
				// make controls visible and make sure  selected item is visible after it
				timesLayout.setVisibility(View.VISIBLE);
				trackInfoLayout.setVisibility(View.VISIBLE);
				seekBar.setVisibility(View.VISIBLE);
				List<String> filesList = new ArrayList<String>(listData.getCurrentPathShowItems().keySet());
				int moveToPos = filesList.indexOf(file.getName());
				if (moveToPos >= 0) {
					listView.smoothScrollToPosition(moveToPos);
				}
			}
			if (playerService.isPlaying()) {
				buttonPlayStop.setText(R.string.button_pause);
			} else {
				buttonPlayStop.setText(R.string.button_play);
			}

			MediaFileData loadedResult = MediaFileUtil.loadMediaFileData(getActivity(), file);
			// it is better to always take track time from the file and not its metadata
			String timeLeft = String.valueOf(playerService.getDuration());
			String timeLeftFormatted = "-" + Utils.timeMMSSformat(timeLeft);
			seekBar.setMax(Integer.valueOf(timeLeft));
			seekBar.setProgress(playerService.getCurrentPosition());
			textLeftTime.setText(timeLeftFormatted);
			if (!TextUtils.isEmpty(loadedResult.artist)) {
				textArtist.setText(loadedResult.artist);
			} else {
				textArtist.setText("");
			}
			if (!TextUtils.isEmpty(loadedResult.title)) {
				textTrackName.setText(loadedResult.title);
			} else {
				textTrackName.setText(file.getName());
			}
		} else {
//			Log.e(TAG, "playerService == null in playFileOperations(File) for file " + file.getAbsolutePath());
//			makeToast(this, getString(R.string.cant_get_info_for_file_or_service_err) + ": " + file.getAbsolutePath());
			buttonPlayStop.setText(R.string.button_play);
		}
	}

	private void onMediaEject() {
		externalMediaMounted = false;
		String extStoragePath = Environment.getExternalStorageDirectory()
				.getAbsolutePath();
		if (playerService.getCurrentlyPlayingFilePath().startsWith(
				extStoragePath)) {
			playerService.unmountPathPlaying();
			playerService.initService();
		}
		listData.setRoot(ROOT_PATH);
		listData.setCurrentPath(listData.getRoot());
		Utils.moveToFolder(getActivity(), listData.getRoot(), null, true, listData, externalMediaMounted, readInternalMedia);
		updateFileListAdapter(listData.getCurrentPathShowItems());
		textPath.setText(getString(R.string.location) + listData.getCurrentPath());
	}

	private void onMediaReady() {
		externalMediaMounted = true;
		if (readInternalMedia) {
			listData.setRoot(ROOT_PATH);
		} else {
			listData.setRoot(Environment.getExternalStorageDirectory().getAbsolutePath());
		}
		//check if current directory path exists
		File file = new File(listData.getCurrentPath());
		if ((file.exists() && (listData.getRoot().length() > listData.getCurrentPath().length()))) {
			Utils.moveToFolder(getActivity(), listData.getRoot(), null, false, listData, externalMediaMounted, readInternalMedia);
			textPath.setText(getString(R.string.location) + listData.getCurrentPath());
			listData.clearPrevListViewPosition();
		} else {
			Utils.moveToFolder(getActivity(), listData.getCurrentPath(), null, true, listData, externalMediaMounted, readInternalMedia);
		}
		updateFileListAdapter(listData.getCurrentPathShowItems());

	}


}
