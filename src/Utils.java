/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

/**
 * Class containing some static utility methods.
 */
public class Utils {
    public static final int IO_BUFFER_SIZE = 8 * 1024;
    private static final String TAG = "Utils";

    private Utils() {};

    /**
     * Get the size in bytes of a bitmap.
     * @param bitmap
     * @return size in bytes
     */
    @SuppressLint("NewApi")
    public static int getBitmapSize(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            return bitmap.getByteCount();
        }
        // Pre HC-MR1
        return bitmap.getRowBytes() * bitmap.getHeight();
    }

    /**
     * Check if external storage is built-in or removable.
     *
     * @return True if external storage is removable (like an SD card), false
     *         otherwise.
     */
    @SuppressLint("NewApi")
    public static boolean isExternalStorageRemovable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return Environment.isExternalStorageRemovable();
        }
        return true;
    }

    /**
     * Check how much usable space is available at a given path.
     *
     * @param path The path to check
     * @return The space available in bytes
     */
    @SuppressLint("NewApi")
    public static long getUsableSpace(File path) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return path.getUsableSpace();
        }
        final StatFs stats = new StatFs(path.getPath());
        return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
    }

    /**
     * Get the memory class of this device (approx. per-app memory limit)
     *
     * @param context
     * @return
     */
    public static int getMemoryClass(Context context) {
        return ((ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE)).getMemoryClass();
    }
    
    /** Create a File for saving an image */
    public static Uri getOutputPhotoFileUri() {
    	// TODO: Check that the SDCard is mounted using Environment.getExternalStorageState().

    	File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
    				Environment.DIRECTORY_PICTURES), "PictureStory");
    	
    	// Create the storage directory if it does not exist
    	if (!mediaStorageDir.exists()) {
    		if (!mediaStorageDir.mkdirs()) { // Media storage directory made -here-
    			Log.e(TAG, "Failed to create directory for photos.");
    			return null;
    		}
    	}

    	// Create a media file name
    	String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
    	File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");

    	return Uri.fromFile(mediaFile);
    }
    
    public static String getOutputVideoFilePath() {
    	File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_MOVIES), "PictureStory");
    	
    	// Create the storage directory if it does not exist
    	if (!mediaStorageDir.exists()) {
    		if (!mediaStorageDir.mkdirs()) { // Media storage directory made -here-
    			Log.e(TAG, "Failed to create directory for videos.");
    			return null;
    		}
    	}
    	
    	// Create a media file name
    	String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
    	String filepath = mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".3gp";
    	
    	return filepath;
    }
    
    public static String getOutputAudioFilePath(Context cxt) {
    	String mediaStorageDir = cxt.getExternalFilesDir(null).getAbsolutePath();

    	// Create a media file name
    	String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
    	mediaStorageDir = mediaStorageDir + File.separator + "REC_" + timeStamp + ".3gp";

    	Log.d(TAG, "Uri calculated for audio recording: " + mediaStorageDir);
    	
    	return mediaStorageDir;
    }
}