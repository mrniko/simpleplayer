package org.sergez.splayer.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
import com.actionbarsherlock.view.MenuItem;
import org.sergez.splayer.R;
import org.sergez.splayer.activity.ListData;
import org.sergez.splayer.activity.PreferencesActivity;
import org.sergez.splayer.activity.SimplePlayerActivity;
import org.sergez.splayer.service.SimplePlayerService;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

public final class Utils {

	private Utils() {

	}

	private static final String TAG = Utils.class.getSimpleName();

	public static String timeMSSFormat(int millis) {
		long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
		long minutes = seconds / 60;
		seconds = seconds - minutes * 60;
		return String.format("%01d:%02d", minutes, seconds);
	}

	public static String timeMMSSformat(String millis) {
		int value = 0;
		try {
			value = Integer.valueOf(millis);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, e.toString());
		}
		return timeMSSFormat(value);
	}

	public static boolean menuClick(MenuItem menuItem, SimplePlayerActivity spActivity, SimplePlayerService playerService) {
		switch (menuItem.getItemId()) {
			case R.id.menu_settings:
				Intent preferencesActivity = new Intent(spActivity, PreferencesActivity.class);
				spActivity.startActivity(preferencesActivity);
				return true;
			case R.id.menu_file_props:
				if ((playerService.playerState >= 0)) {
					String selection = MediaStore.Audio.Media.DATA + " like ?";
					String[] selectionArgs = {playerService
							.getCurrentlyPlayingFilePath() + "%"};
					String[] projection = {MediaStore.Audio.Media.ARTIST,
							MediaStore.Audio.Media.TITLE,
							MediaStore.Audio.Media.DURATION,
							MediaStore.Audio.Media.YEAR,
							MediaStore.Audio.Media.ALBUM,};
					Cursor cursor = spActivity.getContentResolver().query(
							MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
							projection, selection, selectionArgs, null);

					String duration = "";
					String title = "";
					String artist = "";
					String year = "";
					String album = "";
					// TODO: replace moveToNext with more appropriate method
					while (cursor.moveToNext()) {
						artist = spActivity.getString(R.string.trackinfo_artist) + " " + cursor.getString(0);
						title = spActivity.getString(R.string.trackinfo_title) + " " + cursor.getString(1);
						duration = spActivity.getString(R.string.trackinfo_duration) + " " + timeMMSSformat(cursor.getString(2));// cursor.getString(2);
						year = spActivity.getString(R.string.trackinfo_year) + " " + cursor.getString(3);
						album = spActivity.getString(R.string.trackinfo_album) + " " + cursor.getString(4);
					}
					cursor.close();
					File file = new File(
							playerService.getCurrentlyPlayingFilePath());
					String filePath = spActivity.getString(R.string.filepath) + " " + file.getAbsolutePath();

					final CharSequence[] items = {title, artist, album, year, duration,
							filePath};

					AlertDialog.Builder builder = new AlertDialog.Builder(spActivity);

					builder.setTitle(R.string.trackinfo_header);
					builder.setItems(items, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							// do nothing
						}
					});
					builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							// do nothing
						}
					});
					AlertDialog alert = builder.create();
					alert.show();
					return true;
				} else {
					new AlertDialog.Builder(spActivity).setIcon(R.drawable.ic_launcher)
							.setTitle(R.string.properties)
							.setMessage(R.string.media_wasnt_sel)
							.setPositiveButton(R.string.ok, null).show();
				}
			case R.id.menu_exit:
				spActivity.finish();
				return true;
			case R.id.menu_repeat:
				DialogUtils.showDialogRepeatChoice(spActivity, playerService, menuItem);
				return true;
			case R.id.menu_send_feedback:
				Intent sendIntent = new Intent(Intent.ACTION_SEND);
				PackageInfo pInfo = null;
				String version = null;
				try {
					pInfo = spActivity.getPackageManager().getPackageInfo(spActivity.getPackageName(), 0);
					version = pInfo.versionName;
				} catch (PackageManager.NameNotFoundException e) {
					Log.e(TAG, e.getMessage(), e);
					version = "?";
				}
				sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Simple Player " + version + " - Feedback");
				sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"sergii.dev@gmail.com"});
				sendIntent.setType("plain/text");
				spActivity.startActivity(Intent.createChooser(sendIntent, "Send feedback"));
				return true;
			case R.id.menu_shuffle:
				DialogUtils.showDialogShuffleChoice(spActivity, playerService, menuItem);
				return true;
		}
		return false;
	}


	/**
	 * Moves from root dir to first list of directories in the tree of music
	 * files, count of which >1 - if processOnlyCurrentFolder==false
	 * <p/>
	 * Black magic here... optimization needed
	 */
	public static void moveToFolder(Context context, String dirPath, Set<String> allMusicFiles, boolean processOnlyCurrentFolder, ListData listData, boolean extMediaMnt, boolean readIntMedia) {
		long currentTime = new Date().getTime();
		listData.clearCurrentPathItemsFullPath();
		listData.clearCurrentPathPlayableList();
		listData.clearCurrentPathShowItems();
		Map<String, DurationAlbumID> itemLocal = new LinkedHashMap<String, DurationAlbumID>(); //stores pairs <SongName, (SongLength, AlbumID>

		Set<String> folders = new LinkedHashSet<String>();

		if ((!dirPath.equals(listData.getRoot())) && (processOnlyCurrentFolder)) {
			listData.addCurrentPathShowFolder("../");
			int slashPos = dirPath.lastIndexOf('/');
			if (slashPos >= 0) {
				String tmpDirPath = dirPath.substring(0, slashPos);
				if (tmpDirPath.length() > 0) {
					listData.addCurrentPathItemsFullPath(tmpDirPath);// set parent
				} else {
					// root folder
					listData.addCurrentPathItemsFullPath("/");
				}
			} else {
				// root folder
				listData.addCurrentPathItemsFullPath("/");
			}
		}

		if ((allMusicFiles == null) || (processOnlyCurrentFolder)) { // if first step
			allMusicFiles = new HashSet<String>();
			String selection = MediaStore.Audio.Media.DATA + " like ?";
			String[] projection = {
					MediaStore.Audio.Media.DATA,
					MediaStore.Audio.Media.DURATION,
					MediaStore.Audio.Media.ALBUM_ID
			};

			String[] selectionArgs = {dirPath + "%"};
			// to check  if we are working only  with  int/ext  storage
			String externalStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();

			if ((readIntMedia) && (!dirPath.startsWith(externalStoragePath))) {
				Cursor cursorInternal = context.getContentResolver().query(
						MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
						projection, selection, selectionArgs,
						MediaStore.Audio.Media.DATA + " COLLATE NOCASE ASC");
				if (cursorInternal != null) {
					if (processOnlyCurrentFolder) { //if we dont have to look deeper then current folder
						while (cursorInternal.moveToNext()) {
							String s = cursorInternal.getString(0);
							int endPos = s.indexOf('/', dirPath.length() + 1);
							if (endPos > 0) { // folder, add to sorted set
								folders.add(s.substring(0, endPos));
							} else { // file, add to list
								String trackName = s.substring(s.lastIndexOf('/') + 1);
								String trackDuration = Utils.timeMMSSformat(cursorInternal.getString(1));
								int AlbumID = Integer.valueOf(cursorInternal.getString(2));
								DurationAlbumID durationAlbumID = new DurationAlbumID(trackDuration, AlbumID);
								itemLocal.put(trackName, durationAlbumID);
								listData.addCurrentPathPlayableList(s);
							}
						}
					} else {
						while (cursorInternal.moveToNext()) {
							String s = cursorInternal.getString(0);
							allMusicFiles.add(s);
							int endPos = s.indexOf('/', dirPath.length() + 1);
							if (endPos > 0) // folder, add to sorted set
							{
								folders.add(s.substring(0, endPos));
							} else { // file, add to list
								String trackName = s.substring(s.lastIndexOf('/') + 1);
								String trackDuration = Utils.timeMMSSformat(cursorInternal.getString(1));
								int AlbumID = Integer.valueOf(cursorInternal.getString(2));
								DurationAlbumID durationAlbumID = new DurationAlbumID(trackDuration, AlbumID);
								itemLocal.put(trackName, durationAlbumID);
								listData.addCurrentPathPlayableList(s);
							}
						}
					}
					cursorInternal.close();
				} else {
					Log.e(TAG, "CursorInternal == null error");
					makeToast(context, "Can't get media files. Please check memory card or internal memory");
				}
			}
			if (extMediaMnt) {
				Cursor cursor = context.getContentResolver().query(
						MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
						projection, selection, selectionArgs,
						MediaStore.Audio.Media.DATA + " COLLATE NOCASE ASC");
				if (cursor != null) {
					if (processOnlyCurrentFolder) {
						while (cursor.moveToNext()) {
							String s = cursor.getString(0);
							int endPos = s.indexOf('/', dirPath.length() + 1);
							if (endPos > 0) // folder, add to sorted set
							{
								folders.add(s.substring(0, endPos));
							} else { // file, add to list
								String trackName = s.substring(s.lastIndexOf('/') + 1);
								String trackDuration = Utils.timeMMSSformat(cursor.getString(1));
								int AlbumID = Integer.valueOf(cursor.getString(2));
								DurationAlbumID durationAlbumID = new DurationAlbumID(trackDuration, AlbumID);
								itemLocal.put(trackName, durationAlbumID);
								listData.addCurrentPathPlayableList(s);
							}
						}
					} else {
						while (cursor.moveToNext()) {
							String s = cursor.getString(0);
							allMusicFiles.add(s);
							int endPos = s.indexOf('/', dirPath.length() + 1);
							if (endPos > 0) // folder, add to sorted set
							{
								folders.add(s.substring(0, endPos));
							} else { // file, add to list
								String trackName = s.substring(s.lastIndexOf('/') + 1);
								String trackDuration = Utils.timeMMSSformat(cursor.getString(1));
								int AlbumID = Integer.valueOf(cursor.getString(2));
								DurationAlbumID durationAlbumID = new DurationAlbumID(trackDuration, AlbumID);
								itemLocal.put(trackName, durationAlbumID);
								listData.addCurrentPathPlayableList(s);
							}
						}
					}
					cursor.close();
				} else {
					Log.e(TAG, "Cursor == null error");
					makeToast(context, "Can't get media files. Please check memory card");
				}
			}
		} else {// if we've got data from recursion
			for (String s : allMusicFiles) {
				int endPos = s.indexOf('/', dirPath.length() + 1);
				if (endPos > 0) { // folder, add to sorted set
					folders.add(s.substring(0, endPos));
				} else { // file, add to list
					//we have to query information about duration of current file song from MediaStore
					//TODO: make select query to all tracks ?
					//TODO: optimize algorithm
					String[] selectionArgs = {s + "%"};
					String selection = MediaStore.Audio.Media.DATA + " like ?";
					String[] projection = {MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM_ID};
					Cursor cursor = context.getContentResolver().query(
							MediaStore.Audio.Media.getContentUriForPath(s),
							projection, selection, selectionArgs,
							MediaStore.Audio.Media.DATA + " COLLATE NOCASE ASC");
					String trackName = s.substring(s.lastIndexOf('/') + 1);
					String trackDuration = "";
					int AlbumID = 0;
					if ((cursor != null) && (cursor.moveToFirst())) {
						trackDuration = Utils.timeMMSSformat(cursor.getString(0));
						AlbumID = Integer.valueOf(cursor.getString(1));
					} else {
						trackDuration = Utils.timeMMSSformat("0");
						AlbumID = 0;
					}
					DurationAlbumID durationAlbumID = new DurationAlbumID(trackDuration, AlbumID);
					itemLocal.put(trackName, durationAlbumID);
					listData.addCurrentPathPlayableList(s);
				}
			}
		}
		if ((folders.size() == 1) && (allMusicFiles.size() > 1)) {
			// only one folder, move down to hierarchy level
			moveToFolder(context, folders.iterator().next(),
					allMusicFiles, false, listData, extMediaMnt, readIntMedia);
			return;
		}

		// this part of void can be reached only by last iteration of recursion
		listData.setCurrentPath(dirPath);
		if (!processOnlyCurrentFolder) {
			listData.setRoot(dirPath);//put pointer to new root path only if we were in root-search mode
		}
		for (String folderPath : folders) {
			listData.addCurrentPathShowFolder(folderPath.substring(folderPath
					.lastIndexOf('/') + 1));
			listData.addCurrentPathItemsFullPath(folderPath);
		}
		if (itemLocal.size() > 0) {// playable items
			listData.addAllCurrentPathShowItems(itemLocal);
			listData.addAllCurrentPathItemsFullPath(listData.getCurrentPathPlayableList());
		}
		long finalTime = new Date().getTime() - currentTime;
		//Log.e(TAG, "GetDirHash time: "+String.valueOf(finalTime));

	}

	public final static void makeToast(Context context, CharSequence text) {
		Toast.makeText(context, text, Toast.LENGTH_LONG).show();
	}

}
