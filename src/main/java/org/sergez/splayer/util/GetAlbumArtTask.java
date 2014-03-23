package org.sergez.splayer.util;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.widget.ImageView;
import org.sergez.splayer.R;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;

/**
 * @author Sergii Zhuk
 *         Date: 15.03.14
 *         Time: 23:12
 */
public class GetAlbumArtTask implements Runnable {
	private final long album_id;
	private final MemoryImagesCache memoryCache;
	private final ImageView imageView;
	private Context context;

	public GetAlbumArtTask(Context context, Long album_id, MemoryImagesCache memoryCache, ImageView imageView) {
		this.album_id = album_id;
		this.memoryCache = memoryCache;
		this.imageView = imageView;
		this.context = context;
	}

	public void run() {
		try {
			final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
			Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);
			ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
			if (pfd != null) {
				FileDescriptor fd = pfd.getFileDescriptor();
				final Bitmap bm = BitmapFactory.decodeFileDescriptor(fd);
				memoryCache.put(String.valueOf(album_id), bm);
				if (bm != null) {
					imageView.post(new Runnable() {
						public void run() {
							imageView.setImageBitmap(bm);
						}
					});
				} else { //set default value and mark nullvalue for this id in memoryCache
					imageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_launcher));
					memoryCache.put(String.valueOf(album_id), null);
				}
			}
		} catch (FileNotFoundException e) {
			// it's OK, just no album art for current file
			//set default value
			imageView.post(new Runnable() {
				public void run() {
					imageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_launcher));
					memoryCache.put(String.valueOf(album_id), null);
				}
			});
		}
	}
}
