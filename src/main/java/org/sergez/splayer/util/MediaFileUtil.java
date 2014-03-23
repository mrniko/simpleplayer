package org.sergez.splayer.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;

/**
 * @author Sergii Zhuk
 *         Date: 13.10.13
 *         Time: 14:07
 */
public class MediaFileUtil {
  private final static String TAG = MediaFileUtil.class.getSimpleName();

  public static MediaFileData loadMediaFileData(Context context, File file) {
    MediaFileData result;

    // start of the file playing - get all track duration as time left
    String selection = MediaStore.Audio.Media.DATA + " like ?";
    // Some	audio may be explicitly marked as not being music !=  0
    String[] selectionArgs = {file.getAbsolutePath() + "%"};
    String[] projection = {
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.DURATION,
    };
    Uri uri = MediaStore.Audio.Media.getContentUriForPath(file.getAbsolutePath());
    Cursor cursor = context.getContentResolver().query(uri, projection,
        selection, selectionArgs, null);

    if ((cursor != null) && (cursor.moveToFirst())) {
      result = new MediaFileData(cursor.getString(0), cursor.getString(1), cursor.getString(2));
    } else {
      Log.e(TAG, "Cursor is null or empty for file" + file.getAbsolutePath());
      result = new MediaFileData("", "", "0");
    }
    cursor.close();
    return result;
  }

}
