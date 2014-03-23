package org.sergez.splayer.util;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

//Thanks to https://github.com/thest1/LazyList/tree/master/src/com/fedorvlasov/lazylist

public class MemoryImagesCache {

	private static final String TAG = MemoryImagesCache.class.getSimpleName();
	private Map<String, Bitmap> cache = Collections.synchronizedMap(
			new LinkedHashMap<String, Bitmap>(10, 1.5f, true));//Last argument true for LRU ordering
	private long size = 0;//current allocated size
	private long limit = 1000000;//max memory in bytes

	public MemoryImagesCache() {
		//use 10% of available heap size
		setLimit(Runtime.getRuntime().maxMemory() / 10);
	}

	public void setLimit(long new_limit) {
		limit = new_limit;
		Log.d(TAG, "MemoryImagesCache will use up to " + limit / 1024. / 1024. + "MB");
	}

	public Bitmap get(String id) {
		try {
			if (!cache.containsKey(id)) {
				return null;
			}
			return cache.get(id);
		} catch (NullPointerException ex) {
			return null;
		}
	}

	public void put(String id, Bitmap bitmap) {
		try {
			if (cache.containsKey(id)) {
				size -= getSizeInBytes(cache.get(id));
			}
			cache.put(id, bitmap);
			size += getSizeInBytes(bitmap);
			checkSize();
		} catch (Throwable th) {
			Log.e(TAG, th.getMessage(), th);
			th.printStackTrace();
		}
	}

	public boolean containsKey(String id) {
		if (cache.containsKey(id)) {
			return true;
		} else {
			return false;
		}
	}

	private void checkSize() {
		Log.d(TAG, "cache size=" + size + " length=" + cache.size());
		if (size > limit) {
			Iterator<Entry<String, Bitmap>> iter = cache.entrySet().iterator();//least recently accessed item will be the first one iterated
			while (iter.hasNext()) {
				Entry<String, Bitmap> entry = iter.next();
				size -= getSizeInBytes(entry.getValue());
				iter.remove();
				if (size <= limit) {
					break;
				}
			}
			Log.d(TAG, "Clean cache. New size " + cache.size());
		}
	}

	public void clear() {
		cache.clear();
	}

	long getSizeInBytes(Bitmap bitmap) {
		if (bitmap == null) {
			return 0;
		}
		return bitmap.getRowBytes() * bitmap.getHeight();
	}
}
