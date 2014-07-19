package org.sergez.splayer.activity;

import android.content.*;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.TranslateAnimation;
import android.widget.*;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import org.sergez.splayer.util.MediaFileUtil;
import org.sergez.splayer.util.MediaFileData;
import org.sergez.splayer.R;
import org.sergez.splayer.service.SimplePlayerService;
import org.sergez.splayer.util.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.sergez.splayer.util.Constants.ROOT_PATH;
import static org.sergez.splayer.util.Constants.SERVICE_RESPONSE_FALSE;
import static org.sergez.splayer.util.DialogUtils.showFileCantBePlayed;
import static org.sergez.splayer.util.DialogUtils.showFolderCantBeRead;
import static org.sergez.splayer.util.Utils.makeToast;


public class SimplePlayerActivity extends SherlockListActivity {
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

	private final Handler mHandler = new Handler();

	private boolean mIsBound;

	private PlayFileAdapter fileListAdapter;
	private PlayerServiceIntentReceiver playerServiceIntentReceiver;
	private MediaIntentReceiver mediaIntentReceiver;

	// accessed from PlayerFileAdapter:
	MemoryImagesCache memoryCache;
	ListData listData = null;
	UIStateController uiStateController;
	SimplePlayerService playerService;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!getResources().getBoolean(R.bool.tablet_layout)) { // for non-tablet only portrait orientation looks good
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		setContentView(R.layout.main);
		initComponents();
		startPlayerServiceAndReceiver();
	}

    @Override
    public void onNewIntent(Intent intent){
        clearNotifications();
    }

	public class PlayerServiceIntentReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {

            if(SimplePlayerService.ACTION_NOWPLAYING.equals(intent.getAction())) {
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
                    Log.e(TAG, PlayerServiceIntentReceiver.class.getSimpleName()+ " playerState: " + playerState);
                }
            } else if (SimplePlayerService.ACTION_FROM_NOTIF_NEXT.equals(intent.getAction())){
                buttonNextClick();
            } else if (SimplePlayerService.ACTION_FROM_NOTIF_PREV.equals(intent.getAction())){
                buttonPrevClick();
            } else if (SimplePlayerService.ACTION_FROM_NOTIF_PAUSE.equals(intent.getAction())){
                buttonPlayStopClick(true);
            } else if (SimplePlayerService.ACTION_FROM_NOTIF_PLAY.equals(intent.getAction())) {
                buttonPlayStopClick(true);
            }
		}
	}

	public class MediaIntentReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (Intent.ACTION_MEDIA_EJECT.equals(intent.getAction())) {
				onMediaEject();
			} else if (Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(intent.getAction())) {
				onMediaReady();
			} else {
				Log.e(TAG, "Unexpected mediaintent received");
			}
		}
	}

	@Override
	public void onStop() {
		memoryCache.clear();
		if ((playerService != null) && (playerService.playerState < 1)) { //if player isn't playing
			PlayerState.saveState(this, playerService);
			unregisterReceiver(playerServiceIntentReceiver);
			stopService(new Intent(getApplicationContext(), SimplePlayerService.class));
			doUnbindService();
			playerService = null;
		}

		super.onStop();
	}

	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(mediaIntentReceiver);
	}

	@Override
	public void onStart() {
		super.onStart();
		uiStateController = new UIStateController(this);
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
			Utils.moveToFolder(this, listData.getRoot(), null, false, listData, externalMediaMounted, readInternalMedia);
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
		registerReceiver(mediaIntentReceiver, ejectMediaFilter);
		checkIfMediaMounted();
		if (fileListAdapter != null) {
			fileListAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onRestart() {
		super.onRestart();
		if (playerService == null) {
			startPlayerServiceAndReceiver();
			//all other things for player init we have to do in onServiceConnected()
		}
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getSupportMenuInflater().inflate(R.menu.action_menu, menu);
		menuItemRepeat = menu.findItem(R.id.menu_repeat);
		menuItemShuffle = menu.findItem(R.id.menu_shuffle);
		setMenuRepeatShuffleTitlesFromService();
		return true;
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			playerService = ((SimplePlayerService.ServiceBinder) service)
					.getService();
			PlayerState playerState = PlayerState.loadState(SimplePlayerActivity.this);
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
						Utils.moveToFolder(this, parentFilePath, null, true, listData, externalMediaMounted, readInternalMedia);
					}
					playerService.setPathPlaying(listData.getCurrentPathPlayableList(), file.getAbsolutePath());
					if (playerService.moveToFile(file)) {
						playerService.seekTo(playerState.playerServiceProgress);
					} else {
						Log.e(TAG, "Unable move to file: " + file.getAbsolutePath());
						showFileCantBePlayed(this, file);
					}
				} else {
					Utils.moveToFolder(this, listData.getRoot(), null, false, listData, externalMediaMounted, readInternalMedia);
				}
			} else {
				//unable to find selected file
				Utils.moveToFolder(this, listData.getRoot(), null, false, listData, externalMediaMounted, readInternalMedia);
				hideTimesAndSeekInfo();
			}

		} else {//no defaults stored, start from root folder
			//unable to find selected file
			Utils.moveToFolder(this, listData.getRoot(), null, false, listData, externalMediaMounted, readInternalMedia);
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
		Intent intent = new Intent(this, SimplePlayerService.class);
		intent.setAction("Action.DOBIND");// TODO to const
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	void doUnbindService() {
		if (mIsBound) {
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return Utils.menuClick(item, this, playerService);
	}

	@Override
	public void onBackPressed() {
		// check if we are in root folder - do nothing
		if ((listData.getCurrentPathShowItemsSize() > 0)
				&& (listData.getCurrentPathShowKey(0).equals("../"))) {
			File file = new File(listData.getCurrentPathItemsFullpath(0));
			if (file.isDirectory()) {
				if (file.canRead()) {
					listData.setCurrentPath(listData.getCurrentPathItemsFullpath(0));
					Utils.moveToFolder(this, listData.getCurrentPath(), null, true, listData, externalMediaMounted, readInternalMedia);
					updateFileListAdapter(listData.getCurrentPathShowItems());
					textPath.setText(getString(R.string.location) + listData.getCurrentPath());
					moveToPrevPos(listData.getPrevListViewPosition());
				} else {
					showFolderCantBeRead(this, file);
				}
			}
			return;
		} else if (playerService.getPlayerState() > 0) {
			// if player is playing - just return to home screen
			Intent i = new Intent();
			i.setAction(Intent.ACTION_MAIN);
			i.addCategory(Intent.CATEGORY_HOME);
			this.startActivity(i);
		} else
		// back button - makes finish the app
		{
			super.onBackPressed();
		}
	}

	private void moveToPrevPos(List<Integer> prevListViewPos) {
		int prevPos = prevListViewPos.get(prevListViewPos.size() - 1);//gets the last element
		if (prevPos != -1) {
			getListView().smoothScrollToPosition(prevPos);
			prevListViewPos.remove(prevListViewPos.size() - 1);
		}
	}

	private void startPlayerServiceAndReceiver() {
		Intent music = new Intent();
		music.setAction(SimplePlayerService.ACTION_START);
		music.setClass(this, SimplePlayerService.class);
		startService(music);
		doBindService();
		registerIntentReceivers();
	}

	/**
	 * Have to be called only when playerService is binded
	 */
	private void registerIntentReceivers() {
		// intent to receive data from SimplePlayerService
		IntentFilter mpFileChangedFilter;
		mpFileChangedFilter = new IntentFilter(SimplePlayerService.ACTION_NOWPLAYING);
        mpFileChangedFilter.addAction(SimplePlayerService.ACTION_FROM_NOTIF_NEXT);
        mpFileChangedFilter.addAction(SimplePlayerService.ACTION_FROM_NOTIF_PAUSE);
        mpFileChangedFilter.addAction(SimplePlayerService.ACTION_FROM_NOTIF_PLAY);
        mpFileChangedFilter.addAction(SimplePlayerService.ACTION_FROM_NOTIF_PREV);
		playerServiceIntentReceiver = new PlayerServiceIntentReceiver();
		registerReceiver(playerServiceIntentReceiver, mpFileChangedFilter);
	}

	private void initComponents() {
		listData = new ListData();

		memoryCache = new MemoryImagesCache();
		textPath = (TextView) findViewById(R.id.text_path);
		buttonPlayStop = (Button) findViewById(R.id.button_playstop);
		buttonPlayStop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				buttonPlayStopClick(false);
			}
		});

		buttonNext = (Button) findViewById(R.id.button_next);
		buttonNext.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				buttonNextClick();
			}
		});

		buttonPrev = (Button) findViewById(R.id.button_prev);
		buttonPrev.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				buttonPrevClick();
			}
		});

		seekBar = (SeekBar) findViewById(R.id.seekbar_main);
		seekBar.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				seekChange(v);
				return false;
			}
		});

		textCurrentTime = (TextView) findViewById(R.id.text_current_tracktime);
		textLeftTime = (TextView) findViewById(R.id.text_left_tracktime);
		textTrackName = (TextView) findViewById(R.id.text_tracktime);
		textArtist = (TextView) findViewById(R.id.text_trackartist);

		trackInfoLayout = (LinearLayout) this.findViewById(R.id.layout_trackinfo);

		timesLayout = (LinearLayout) this.findViewById(R.id.layout_time);
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

	public void startPlayProgressUpdater() {
		if ((playerService != null) && (playerService.getPlayerState() >= 0)) {
			seekBar.setProgress(playerService.getCurrentPosition());
			String leftTimeMMSS = "-"
					+ Utils
					.timeMSSFormat(playerService.getDuration()
							- playerService.getCurrentPosition());
			String currentTimeMMSS = Utils
					.timeMSSFormat(playerService.getCurrentPosition());
			textCurrentTime.setText(currentTimeMMSS);
			textLeftTime.setText(leftTimeMMSS);
			if (playerService.isPlaying()) {
				Runnable notification = new Runnable() {
					public void run() {
						startPlayProgressUpdater();
					}
				};
				mHandler.postDelayed(notification, 1000);
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
			String timeMMSS = Utils.timeMSSFormat(playerService.getCurrentPosition());
			textCurrentTime.setText(timeMMSS);
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
				makeToast(this, getString(R.string.unable_start_playing));
			}
		} else if (playerService.getPlayerState() == SimplePlayerService.PLAYER_IS_PLAYING) {
			playerService.makePause();
            if (!calledFromNotification){
                clearNotifications();
            }
		}
	}

	private void buttonPrevClick() {
		String path = playerService.movePrev();
		if (path != null) {
			if (SERVICE_RESPONSE_FALSE.equals(path)) {
				showFileCantBePlayed(this, playerService.getCurrentlyPlayingFilePath());
				hideTimesAndSeekInfo();
			} else {
				playFileOperations(path);
				fileListAdapter.notifyDataSetChanged();
			}
		} else if (!TextUtils.isEmpty(playerService.getCurrentlyPlayingFilePath())) {
			makeToast(this, getString(R.string.play_first_file_in_list));
		} else {
			makeToast(this, getString(R.string.unable_move_prev));
		}
	}

	private void buttonNextClick() {
		String path = playerService.moveNext();
		if (path != null) {
			if (SERVICE_RESPONSE_FALSE.equals(path)) {
				showFileCantBePlayed(this, playerService.getCurrentlyPlayingFilePath());
				hideTimesAndSeekInfo();
			} else {
				playFileOperations(path);
				fileListAdapter.notifyDataSetChanged();
			}
		} else if (!TextUtils.isEmpty(playerService.getCurrentlyPlayingFilePath())) {
			makeToast(this, getString(R.string.playing_lastfile_inlist));
		} else {
			makeToast(this, getString(R.string.unable_mv_next));
		}
	}

	private void hideTimesAndSeekInfo() {
		timesLayout.setVisibility(View.GONE);
		trackInfoLayout.setVisibility(View.GONE);
		seekBar.setVisibility(View.GONE);
	}

	private Runnable mAnimatePathText = new Runnable() {
		public void run() {
			TranslateAnimation animation = new TranslateAnimation(0, 0, 0, -textPath.getHeight() * 0.82f);
			animation.setDuration(1); // animation disabled
			animation.setFillAfter(false);
			AnimationMakeGoneListener listener = new AnimationMakeGoneListener(textPath, SimplePlayerActivity.this);
			animation.setAnimationListener(listener);
			textPath.startAnimation(animation);
		}
	};

	private void updateFileListAdapter(Map<String, DurationAlbumID> listItems) {
		textPath.setVisibility(View.VISIBLE);

		fileListAdapter = new PlayFileAdapter(this, R.layout.row, listItems);
		ListView myList = (ListView) findViewById(android.R.id.list);
		myList.setAdapter(fileListAdapter);

		mHandler.removeCallbacks(mAnimatePathText);
		mHandler.postDelayed(mAnimatePathText, 4000);
	}

	@Override
	protected void onListItemClick(ListView listView, View v, int position, long id) {
		File file = new File(listData.getCurrentPathItemsFullpath(position));
		if (file.isDirectory()) {
			if (file.canRead()) {
				String prevPath = new String(listData.getCurrentPath());
				listData.setCurrentPath(listData.getCurrentPathItemsFullpath(position));
				Utils.moveToFolder(this, listData.getCurrentPath(), null, true, listData, externalMediaMounted, readInternalMedia);
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
				showFolderCantBeRead(this, file);
			}
		} else { // file was selected
			startPlayingFile(file);
		}
	}

	private void startPlayingFile(File file) {
		playerService.setPathPlaying(listData.getCurrentPathPlayableList(), file.getAbsolutePath());
		boolean waitUserCommandStatPlay = uiStateController.isPlayOnlyWhenButtonPressed();
		if (waitUserCommandStatPlay) {
			if (!playerService.isWaitUserCommandStartPlay(file)) {
				showFileCantBePlayed(this, file);
				hideTimesAndSeekInfo();
			}
		} else {
			if (!playerService.playFile(file)) {
				showFileCantBePlayed(this, file);
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
     *  Remove all notification if player is in pause mode or not ready
     */
    private void clearNotifications(){
        if((playerService!=null)&&(playerService.playerState<1)){
            SimplePlayerService.cancelAllNotifications(this);
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
					getListView().smoothScrollToPosition(moveToPos);
				}
			}
			if (playerService.isPlaying()) {
				buttonPlayStop.setText(R.string.button_pause);
			} else {
				buttonPlayStop.setText(R.string.button_play);
			}

			MediaFileData loadedResult = MediaFileUtil.loadMediaFileData(this, file);
			String timeLeft = loadedResult.duration;// metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
			if (TextUtils.isEmpty(timeLeft)) {
				timeLeft = String.valueOf(playerService.getDuration());
			}
			String timeLeftFormatted = "-" + Utils.timeMMSSformat(timeLeft);
			seekBar.setMax(Integer.valueOf(timeLeft));
			seekBar.setProgress(playerService.getCurrentPosition());
			textLeftTime.setText(timeLeftFormatted);
			if (loadedResult.artist != null) {
				textArtist.setText(loadedResult.artist);
			}
			if (!TextUtils.isEmpty(loadedResult.title)) {
				textTrackName.setText(loadedResult.title);
			} else {
				textTrackName.setText(file.getName());
			}
		} else {
			Log.e(TAG, "playerService == null in playFileOperations(File) for file " + file.getAbsolutePath());
			makeToast(this, getString(R.string.cant_get_info_for_file_or_service_err) + ": " + file.getAbsolutePath());
			buttonPlayStop.setText(R.string.button_pause);
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
		Utils.moveToFolder(this, listData.getRoot(), null, true, listData, externalMediaMounted, readInternalMedia);
		updateFileListAdapter(listData.getCurrentPathShowItems());
		textPath.setText(getString(R.string.location) + listData.getCurrentPath());
	}

	private void onMediaReady() {
		memoryCache.clear();
		externalMediaMounted = true;
		if (readInternalMedia) {
			listData.setRoot(ROOT_PATH);
		} else {
			listData.setRoot(Environment.getExternalStorageDirectory().getAbsolutePath());
		}
		//check if current directory path exists
		File file = new File(listData.getCurrentPath());
		if ((file.exists() && (listData.getRoot().length() > listData.getCurrentPath().length()))) {
			Utils.moveToFolder(this, listData.getRoot(), null, false, listData, externalMediaMounted, readInternalMedia);
			textPath.setText(getString(R.string.location) + listData.getCurrentPath());
			listData.clearPrevListViewPosition();
		} else {
			Utils.moveToFolder(this, listData.getCurrentPath(), null, true, listData, externalMediaMounted, readInternalMedia);
		}
		updateFileListAdapter(listData.getCurrentPathShowItems());

	}

}
