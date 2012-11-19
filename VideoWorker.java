package com.jamie.picturestory;

import static com.googlecode.javacv.cpp.avcodec.AV_CODEC_ID_MPEG4;
import static com.googlecode.javacv.cpp.avcodec.AV_CODEC_ID_AMR_NB;
import static com.googlecode.javacv.cpp.avcodec.AV_CODEC_ID_AAC;
import static com.googlecode.javacv.cpp.avutil.AV_SAMPLE_FMT_FLT;
import static com.googlecode.javacv.cpp.avutil.PIX_FMT_YUV420P;
import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.googlecode.javacv.FFmpegFrameGrabber;
import com.googlecode.javacv.FFmpegFrameRecorder;
import com.googlecode.javacv.Frame;
import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.FrameRecorder;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class VideoWorker {
	private ArrayList<Uri> mUris;
	private ArrayList<StoryTransition> mStoryTransitions;
	private ImageWorker mImageWorker;
	
	private String mSaveFilePath;
	
	// Keep a RAM cache for the IplImages as they are relatively costly to create
	private LruCache<String, IplImage> mMemoryCache;
	
	private static final String TAG = "VideoWorker";
	
	// Default memory cache size
    private static final int DEFAULT_MEM_CACHE_SIZE = 1024 * 1024 * 5; // 5MB
	
	private static final String FORMAT = "3gp";
	private static final double FRAME_RATE = 12.0d;
	private static final int VIDEO_CODEC = AV_CODEC_ID_MPEG4; // This is less buggy than h.263 in ffmpeg
	private static final int AUDIO_CODEC = AV_CODEC_ID_AAC;
	private static final int PIXEL_FORMAT = PIX_FMT_YUV420P;
	private static final int SAMPLE_FORMAT = AV_SAMPLE_FMT_FLT;
	private static final int COLOUR_DEPTH = IPL_DEPTH_8U;
	private static final int COLOUR_CHANNELS = 4;
	private static final int AUDIO_CHANNELS = 1;
	private static final int SAMPLE_RATE = 8000;
	private static final int WIDTH = 352;
	private static final int HEIGHT = 288;
	
	VideoWorker(ImageWorker iw, ArrayList<Uri> u, ArrayList<StoryTransition> st) {
		mImageWorker = iw;
		mUris = u;
		mStoryTransitions = st;
		mMemoryCache = new LruCache<String, IplImage>(DEFAULT_MEM_CACHE_SIZE) {
			@Override
            protected int sizeOf(String key, IplImage image) {
                return (image.width() * image.height() * COLOUR_CHANNELS);
            }
		};
		mSaveFilePath = Utils.getOutputVideoFilePath();
	}
	
	public void createVideoWithAudio(String audioFilePath) {
		createVideo();
		
		FrameGrabber grabber1 = new FFmpegFrameGrabber(mSaveFilePath);
		grabber1.setFormat(FORMAT);
	    FrameGrabber grabber2 = new FFmpegFrameGrabber(audioFilePath);
	    grabber2.setFormat(FORMAT);
	    try {
			grabber1.start();
			grabber2.start();
		} catch (com.googlecode.javacv.FrameGrabber.Exception e) {
			Log.e(TAG, "Error starting FFmpegFrameGrabber for audio merge!", e);
		} 
	    
	    String newFilePath = Utils.getOutputVideoFilePath();
	    FrameRecorder recorder = new FFmpegFrameRecorder(newFilePath, WIDTH, HEIGHT); 
	    
	    recorder.setVideoCodec(VIDEO_CODEC);
        recorder.setFormat(FORMAT);
        recorder.setPixelFormat(PIXEL_FORMAT);
        recorder.setFrameRate(FRAME_RATE);
	    
        Log.d(TAG, "Grabber channels: " + grabber2.getAudioChannels());
        Log.d(TAG, "Grabber sample format: " + grabber2.getSampleFormat());
        Log.d(TAG, "Grabber sample rate: " + grabber2.getSampleRate());
        
        Log.d(TAG, "Recorder audio codec: " + recorder.getAudioCodec());
        Log.d(TAG, "Recorder audio channels: " + recorder.getAudioChannels());
        Log.d(TAG, "Recorder sample format: " + recorder.getSampleFormat());
        Log.d(TAG, "Recorder sample rate: " + recorder.getSampleRate());
        
	    recorder.setAudioCodec(AUDIO_CODEC);
	    recorder.setAudioChannels(AUDIO_CHANNELS);
	    recorder.setSampleFormat(SAMPLE_FORMAT);
	    recorder.setSampleRate(SAMPLE_RATE);
	    
	    Log.d(TAG, "Recorder audio codec: " + recorder.getAudioCodec());
        Log.d(TAG, "Recorder audio channels: " + recorder.getAudioChannels());
        Log.d(TAG, "Recorder sample format: " + recorder.getSampleFormat());
        Log.d(TAG, "Recorder sample rate: " + recorder.getSampleRate());
	    
	    try {
			recorder.start();
		} catch (com.googlecode.javacv.FrameRecorder.Exception e) {
			Log.e(TAG, "Error starting FFmpegFrameRecorder for audio merge!", e);
		}
	    
	    Frame frame1, frame2; 
	    try {
	    	while ((frame1 = grabber1.grabFrame()) != null && 
	    			(frame2 = grabber2.grabFrame()) != null) { 
	    		recorder.record(frame1); 
	    		recorder.record(frame2);
	    	}
	    } catch (Exception e) {
	    	Log.e(TAG, "Error capturing frame for audio merge!", e);
	    }
	    
	    try {
	    	recorder.stop(); 
	    	grabber1.stop(); 
	    	grabber2.stop();
	    } catch (Exception e) {
	    	Log.e(TAG, "Error stopping grabbers/recorder for audio merge!", e);
	    }
	}
	
	public void createVideo() {
		FrameRecorder recorder = new FFmpegFrameRecorder(mSaveFilePath, WIDTH, HEIGHT);
		recorder.setVideoCodec(VIDEO_CODEC);
        recorder.setFormat(FORMAT);
        recorder.setPixelFormat(PIXEL_FORMAT);
        recorder.setFrameRate(FRAME_RATE);

        try {
			recorder.start();
		} catch (com.googlecode.javacv.FrameRecorder.Exception e) {
			Log.e(TAG, "Failed to start FFmpegFrameRecorder!", e);
		}
        for (int i = 0; i < mStoryTransitions.size() - 1; i++) {
			Uri uri = mUris.get(mStoryTransitions.get(i).getPosition());
        	IplImage image = mMemoryCache.get(uri.toString());
        	
        	if (image == null) {
        		ImageWorkerTask task = new ImageWorkerTask();
        		task.execute(uri.toString());
        		try {
					image = task.get();
				} catch (Exception e) {
					Log.e(TAG, "Critical error during image processing task!", e);
					e.printStackTrace();
				}
        	}
        	long time = mStoryTransitions.get(i + 1).getTime();
        	int frames = (int) (time * FRAME_RATE / 1000);
        	try {
        		recorder.record(image);
        		recorder.setFrameNumber(frames);
        	} catch (com.googlecode.javacv.FrameRecorder.Exception e) {
				Log.e(TAG, "Failed to record frame.", e);
			}
		}
        try {
			recorder.stop();
		} catch (com.googlecode.javacv.FrameRecorder.Exception e) {
			Log.e(TAG, "Failed to stop FFmpegFrameRecorder!", e);
		}
        
        // Clear the cache when our work is done.
        mMemoryCache.evictAll();
	}
	
	private class ImageWorkerTask extends AsyncTask<String, Void, IplImage> {
		@Override
	    protected IplImage doInBackground(String...strings) {			
			return fetchImage(strings[0]);
		}
		
		private IplImage fetchImage(String address) {
			// Get the bitmap from the image worker
	        Bitmap bitmap = mImageWorker.getBitmapSynchronous(address);
	        
	        // Resize the bitmap to fit the video
	        bitmap = resizeBitmap(bitmap);
	        
	        // Convert bitmap to IplImage
	        IplImage image = bitmapToIplImage(bitmap);
	        
	        mMemoryCache.put(address, image);
	        
	        return image;
		}
		
		private IplImage bitmapToIplImage(Bitmap bitmap) {
	    	//IplImage image = IplImage.create(WIDTH, HEIGHT, COLOUR_DEPTH, COLOUR_CHANNELS);
	    	
	    	IplImage image = IplImage.create(bitmap.getWidth(), bitmap.getHeight(), COLOUR_DEPTH, COLOUR_CHANNELS);    	
	        bitmap.copyPixelsToBuffer(image.getByteBuffer());
	    	return image;
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
	        
	        return paddedBitmap;
	    }
	}
}
