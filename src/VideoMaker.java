/**
 * I wanted to implement a basic disk cache structure to minimise the number
 * of reads to the disk and resizing of images. This class is mostly a guide
 * as to how I tried to implement command-line ffmpeg in my PictureStory app.
 * The file makes references to parts of that app and so won't build unless
 * integrated with that. Most importantly, the image processing/caching
 * framework is missing (ImageWorker, etc)
 * @author Jamie Hewland
 */

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.util.Log;

import com.example.android_ffmpeg_cmdline.BuildConfig;

public class VideoMaker {
	
	private static final String TAG = "VideoMaker";
    private static final String CACHE_FILENAME_PREFIX = "cache_";
    private static final int MAX_REMOVALS = 4;
    private static final int INITIAL_CAPACITY = 32;
    private static final float LOAD_FACTOR = 0.75f;
    
    private static final int WIDTH = 352;
    private static final int HEIGHT = 288;
    private static final String FORMAT = "3gp";
    private static final String VIDEO_CODEC = "h263";
    private static final String FRAME_RATE = "12";

    private final File mCacheDir;
    private int cacheSize = 0;
    private int cacheByteSize = 0;
    private final int maxCacheItemSize = 64; // 64 item default
    private long maxCacheByteSize = 1024 * 1024 * 5; // 5MB default
    private CompressFormat mCompressFormat = CompressFormat.JPEG;
    private int mCompressQuality = 70;
    
    private final Context mContext;

    private final Map<String, String> mLinkedHashMap =
            Collections.synchronizedMap(new LinkedHashMap<String, String>(
                    INITIAL_CAPACITY, LOAD_FACTOR, true));
	
    public static VideoMaker init(Context context, long maxByteSize) {
        File cacheDir = getDiskCacheDir(context, "cache");
    	if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }

        if (cacheDir.isDirectory() && cacheDir.canWrite()
                && Utils.getUsableSpace(cacheDir) > maxByteSize) {
            return new VideoMaker(context, cacheDir, maxByteSize);
        }

        return null;
    }
    
    private VideoMaker(Context context, File cacheDir, long maxByteSize) {
        mCacheDir = cacheDir;
        maxCacheByteSize = maxByteSize;
        mContext = context;
    }
    
    public void createVideo(ImageWorker imageWorker, ArrayList<Uri> uris, 
    		ArrayList<StoryTransition> storyTransitions, String audioFilePath) {
    	
    	FfmpegController ffmpeg = null;
    	try {
    		ffmpeg = new FfmpegController(mContext);
    	} catch (IOException ioe) {
    		Log.e(TAG, "Error loading ffmpeg. " + ioe.getMessage());
    	}
    	
    	MediaDesc in = new MediaDesc();
    	//in.format = FORMAT;
    	in.height = HEIGHT;
    	in.width = WIDTH;
    	in.videoFps = FRAME_RATE;
    	//in.videoCodec = VIDEO_CODEC;
    	
    	ShellDummy shelly = new ShellDummy();
    	
    	int numTransitions = storyTransitions.size();
    	ArrayList<MediaDesc> splits = new ArrayList<MediaDesc>(numTransitions);
    	
    	for (int i = 0; i < storyTransitions.size() - 1; i++) {
    		int position = storyTransitions.get(i).getPosition();
    		long time = storyTransitions.get(i + 1).getTime();
    		String key = uris.get(position).toString();
    		
    		if (!containsKey(key)) {
    			Bitmap bitmap = imageWorker.getBitmapSynchronous(key);
    			put(key, resizeBitmap(bitmap));
    		}
    		
    		String filepath = get(key);
    		in.path = filepath;
    		
    		MediaDesc out = new MediaDesc();
    		out.path = Utils.getOutputVideoFilePath();
    		splits.add(out);
    		
    		ffmpeg.execChmod(filepath, "666");
    		ffmpeg.testCommands("ls -l " + filepath, shelly);
    		ffmpeg.convertImageToVideo(in, (int)(time / 1000), out.path, shelly);
    	}
    	
    	MediaDesc finalOut = new MediaDesc();
    	finalOut.path = Utils.getOutputVideoFilePath();
    	try {
    		ffmpeg.concatAndTrimFilesMPEG(splits, finalOut, true, shelly);
    	} catch (Exception e) {
    		Log.e(TAG, "Error concatenating videos!", e);
    	}
    	
    	MediaDesc audio = new MediaDesc();
    	audio.path = audioFilePath;
    	try {
    		ffmpeg.combineAudioAndVideo(finalOut, audio, Utils.getOutputAudioFilePath(mContext), shelly);
    	} catch (Exception e) {
    		Log.e(TAG, "Error adding audio to video!", e);
    	}
    }
    
    private Bitmap resizeBitmap(Bitmap bitmap) {
    	final float reqAspectRatio = WIDTH / HEIGHT;
    	
    	final int height = bitmap.getHeight();
        final int width = bitmap.getWidth();
        
        int longestSide = height;
        if (width > height) {
        	longestSide = width;
        }
        
        int reqHeight;
        int reqWidth;
        
        if (longestSide == height) {
        	reqHeight = height;
        	reqWidth = (int)(height * reqAspectRatio);
        } else {
        	reqWidth = width;
        	reqHeight = (int)(width / reqAspectRatio);
        }
        
        float paddingX = (reqWidth - width) / 2.0f;
        float paddingY = (reqHeight - height) / 2.0f;
        
        Bitmap paddedBitmap = Bitmap.createBitmap(reqWidth, reqHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(paddedBitmap);
        canvas.drawARGB(0xFF, 0xFF, 0xFF, 0xFF);
        canvas.drawBitmap(bitmap, paddingX, paddingY, new Paint(Paint.FILTER_BITMAP_FLAG));
        
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(paddedBitmap, WIDTH, HEIGHT, false);
        
        return scaledBitmap;
    }
    
    /**
     * Add a bitmap to the disk cache.
     *
     * @param key A unique identifier for the bitmap.
     * @param data The bitmap to store.
     */
    public void put(String key, Bitmap data) {
        synchronized (mLinkedHashMap) {
            if (mLinkedHashMap.get(key) == null) {
                try {
                    final String file = createFilePath(mCacheDir, key);
                    if (writeBitmapToFile(data, file)) {
                        put(key, file);
                        flushCache();
                    }
                } catch (final FileNotFoundException e) {
                    Log.e(TAG, "Error in put: " + e.getMessage());
                } catch (final IOException e) {
                    Log.e(TAG, "Error in put: " + e.getMessage());
                }
            }
        }
    }

    private void put(String key, String file) {
        mLinkedHashMap.put(key, file);
        cacheSize = mLinkedHashMap.size();
        cacheByteSize += new File(file).length();
    }

    /**
     * Flush the cache, removing oldest entries if the total size is over the specified cache size.
     * Note that this isn't keeping track of stale files in the cache directory that aren't in the
     * HashMap. If the images and keys in the disk cache change often then they probably won't ever
     * be removed.
     */
    private void flushCache() {
        Entry<String, String> eldestEntry;
        File eldestFile;
        long eldestFileSize;
        int count = 0;

        while (count < MAX_REMOVALS &&
                (cacheSize > maxCacheItemSize || cacheByteSize > maxCacheByteSize)) {
            eldestEntry = mLinkedHashMap.entrySet().iterator().next();
            eldestFile = new File(eldestEntry.getValue());
            eldestFileSize = eldestFile.length();
            mLinkedHashMap.remove(eldestEntry.getKey());
            eldestFile.delete();
            cacheSize = mLinkedHashMap.size();
            cacheByteSize -= eldestFileSize;
            count++;
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "flushCache - Removed cache file, " + eldestFile + ", "
                        + eldestFileSize);
            }
        }
    }

    /**
     * Get an image from the disk cache.
     *
     * @param key The unique identifier for the bitmap
     * @return The bitmap or null if not found
     */
    public String get(String key) {
        synchronized (mLinkedHashMap) {
            final String file = mLinkedHashMap.get(key);
            if (file != null) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Disk cache hit");
                }
                return file;
            } else {
                final String existingFile = createFilePath(mCacheDir, key);
                if (new File(existingFile).exists()) {
                    put(key, existingFile);
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Disk cache hit (existing file)");
                    }
                    return existingFile;
                }
            }
            return null;
        }
    }

    /**
     * Checks if a specific key exist in the cache.
     *
     * @param key The unique identifier for the bitmap
     * @return true if found, false otherwise
     */
    public boolean containsKey(String key) {
        // See if the key is in our HashMap
        if (mLinkedHashMap.containsKey(key)) {
            return true;
        }

        // Now check if there's an actual file that exists based on the key
        final String existingFile = createFilePath(mCacheDir, key);
        if (new File(existingFile).exists()) {
            // File found, add it to the HashMap for future use
            put(key, existingFile);
            return true;
        }
        return false;
    }

    /**
     * Removes all disk cache entries from this instance cache dir
     */
    public void clearCache() {
        VideoMaker.clearCache(mCacheDir);
    }

    /**
     * Removes all disk cache entries from the application cache directory in the uniqueName
     * sub-directory.
     *
     * @param context The context to use
     * @param uniqueName A unique cache directory name to append to the app cache directory
     */
    public static void clearCache(Context context, String uniqueName) {
        File cacheDir = getDiskCacheDir(context, uniqueName);
        clearCache(cacheDir);
    }

    /**
     * Removes all disk cache entries from the given directory. This should not be called directly,
     * call {@link DiskLruCache#clearCache(Context, String)} or {@link DiskLruCache#clearCache()}
     * instead.
     *
     * @param cacheDir The directory to remove the cache files from
     */
    private static void clearCache(File cacheDir) {
        final File[] files = cacheDir.listFiles(cacheFileFilter);
        for (int i=0; i<files.length; i++) {
            files[i].delete();
        }
    }

    /**
     * Get a usable cache directory (external if available, internal otherwise).
     *
     * @param context The context to use
     * @param uniqueName A unique directory name to append to the cache dir
     * @return The cache dir
     */
    public static File getDiskCacheDir(Context context, String uniqueName) {

        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
        // otherwise use internal cache dir
        /*final String cachePath =
                Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED ||
                        !Utils.isExternalStorageRemovable() ?
                        context.getExternalCacheDir().getPath() :
                        context.getCacheDir().getPath();*/
    	final String cachePath = context.getCacheDir().getPath();

        return new File(cachePath + File.separator + uniqueName);
    }

    /**
     * Creates a constant cache file path given a target cache directory and an image key.
     *
     * @param cacheDir
     * @param key
     * @return
     */
    public static String createFilePath(File cacheDir, String key) {
        try {
            // Use URLEncoder to ensure we have a valid filename, a tad hacky but it will do for
            // this example
            return cacheDir.getAbsolutePath() + File.separator +
                    CACHE_FILENAME_PREFIX + URLEncoder.encode(key.replace("*", "") + ".jpeg", "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            Log.e(TAG, "createFilePath - " + e);
        }

        return null;
    }

    /**
     * Create a constant cache file path using the current cache directory and an image key.
     *
     * @param key
     * @return
     */
    public String createFilePath(String key) {
        return createFilePath(mCacheDir, key);
    }

    /**
     * Sets the target compression format and quality for images written to the disk cache.
     *
     * @param compressFormat
     * @param quality
     */
    public void setCompressParams(CompressFormat compressFormat, int quality) {
        mCompressFormat = compressFormat;
        mCompressQuality = quality;
    }

    /**
     * Writes a bitmap to a file. Call {@link DiskLruCache#setCompressParams(CompressFormat, int)}
     * first to set the target bitmap compression and format.
     *
     * @param bitmap
     * @param file
     * @return
     */
    private boolean writeBitmapToFile(Bitmap bitmap, String file)
            throws IOException, FileNotFoundException {

        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(file), Utils.IO_BUFFER_SIZE);
            return bitmap.compress(mCompressFormat, mCompressQuality, out);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
    
    /**
     * A filename filter to use to identify the cache filenames which have CACHE_FILENAME_PREFIX
     * prepended.
     */
    private static final FilenameFilter cacheFileFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            return filename.startsWith(CACHE_FILENAME_PREFIX);
        }
    };
    
    private class ShellDummy implements ShellUtils.ShellCallback {

		@Override
		public void shellOut(String shellLine) {
			
			Log.d(TAG, "FFmpeg cmd message: " + shellLine);
		}

		@Override
		public void processComplete(int exitValue) {
			Log.e(TAG, "FFmpeg process exit value: " + exitValue);
		}
    	
    }
}
