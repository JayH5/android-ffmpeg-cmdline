package za.jamie.androidffmpegcmdline.ffmpeg;
import java.util.LinkedList;
import java.util.List;

import android.text.TextUtils;


public class FfmpegJob {

	public String inputPath;
	
	public String inputFormat;
	public String inputVideoCodec;
	public String inputAudioCodec;
	
	public String videoCodec;
	public int videoBitrate;
	public int videoWidth = -1;
	public int videoHeight = -1;
	public float videoFramerate = -1;	
	public String videoBitStreamFilter;
	
	public String audioCodec;
	public int audioBitrate;
	public int audioSampleRate;
	public int audioChannels;
	public int audioVolume;
	public String audioBitStreamFilter;
	
	public String audioFilter;
	public String videoFilter;
	
	public long startTime = -1;
	public long duration = -1;
	
	public String outputPath;
	public String format;
	
	public boolean disableVideo;
	public boolean disableAudio;
	
	private final String mFfmpegPath;
	
	public FfmpegJob(String ffmpegPath) {
		mFfmpegPath = ffmpegPath;
	}
	
	public ProcessRunnable create() {
		if (inputPath == null || outputPath == null) {
			throw new IllegalStateException("Need an input and output filepath!");
		}	
		
		final List<String> cmd = new LinkedList<String>();
		
		cmd.add(mFfmpegPath);
		cmd.add("-y");
		
		if (!TextUtils.isEmpty(inputFormat)) {
			cmd.add(FFMPEGArg.ARG_FORMAT);
			cmd.add(inputFormat);
		}
		if (!TextUtils.isEmpty(inputVideoCodec)) {
			cmd.add(FFMPEGArg.ARG_VIDEOCODEC);
			cmd.add(inputVideoCodec);
		}
		if (!TextUtils.isEmpty(inputAudioCodec)) {
			cmd.add(FFMPEGArg.ARG_AUDIOCODEC);
			cmd.add(inputAudioCodec);
		}
		
		cmd.add("-i");
		cmd.add(inputPath);
		
		if (disableVideo) {
			cmd.add(FFMPEGArg.ARG_DISABLE_VIDEO);
		} else {
			if (videoBitrate > 0) {
				cmd.add(FFMPEGArg.ARG_BITRATE_VIDEO);
				cmd.add(videoBitrate + "k");
			}
			if (videoWidth > 0 && videoHeight > 0) {
				cmd.add(FFMPEGArg.ARG_SIZE);
				cmd.add(videoWidth + "x" + videoHeight);
			}
			if (videoFramerate > -1) {
				cmd.add(FFMPEGArg.ARG_FRAMERATE);
				cmd.add(String.valueOf(videoFramerate));
			}
			if (!TextUtils.isEmpty(videoCodec)) {
				cmd.add(FFMPEGArg.ARG_VIDEOCODEC);
				cmd.add(videoCodec);
			}
			if (!TextUtils.isEmpty(videoBitStreamFilter)) {
				cmd.add(FFMPEGArg.ARG_VIDEOBITSTREAMFILTER);
				cmd.add(videoBitStreamFilter);
			}		
			if (!TextUtils.isEmpty(videoFilter)) {
				cmd.add(FFMPEGArg.ARG_VIDEOFILTER);
				cmd.add(videoFilter);
			}
		}
		if (disableAudio) {
			cmd.add(FFMPEGArg.ARG_DISABLE_AUDIO);
		} else {
			if (!TextUtils.isEmpty(audioCodec)) {
				cmd.add(FFMPEGArg.ARG_AUDIOCODEC);
				cmd.add(audioCodec);
			}
			if (!TextUtils.isEmpty(audioBitStreamFilter)) {
				cmd.add(FFMPEGArg.ARG_AUDIOBITSTREAMFILTER);
				cmd.add(audioBitStreamFilter);
			}
			if (!TextUtils.isEmpty(audioFilter)) {
				cmd.add(FFMPEGArg.ARG_AUDIOFILTER);
				cmd.add(audioFilter);
			}
			if (audioChannels > 0) {
				cmd.add(FFMPEGArg.ARG_CHANNELS_AUDIO);
				cmd.add(String.valueOf(audioChannels));
			}
			if (audioVolume > 0) {
				cmd.add(FFMPEGArg.ARG_VOLUME_AUDIO);
				cmd.add(String.valueOf(audioVolume));
			}
			if (audioBitrate > 0) {
				cmd.add(FFMPEGArg.ARG_BITRATE_AUDIO);
				cmd.add(audioBitrate + "k");
			}
		}
			
		if (!TextUtils.isEmpty(format)) {
			cmd.add("-f");
			cmd.add(format);
		}
		
		cmd.add(outputPath);
		
		final ProcessBuilder pb = new ProcessBuilder(cmd);
		return new ProcessRunnable(pb);
	}
	
	public class FFMPEGArg {
		String key;
		String value;
		
		public static final String ARG_VIDEOCODEC = "-vcodec";
		public static final String ARG_AUDIOCODEC = "-acodec";
		
		public static final String ARG_VIDEOBITSTREAMFILTER = "-vbsf";
		public static final String ARG_AUDIOBITSTREAMFILTER = "-absf";
		
		public static final String ARG_VIDEOFILTER = "-vf";
		public static final String ARG_AUDIOFILTER = "-af";
		
		public static final String ARG_VERBOSITY = "-v";
		public static final String ARG_FILE_INPUT = "-i";
		public static final String ARG_SIZE = "-s";
		public static final String ARG_FRAMERATE = "-r";
		public static final String ARG_FORMAT = "-f";
		public static final String ARG_BITRATE_VIDEO = "-b:v";
		
		public static final String ARG_BITRATE_AUDIO = "-b:a";
		public static final String ARG_CHANNELS_AUDIO = "-ac";
		public static final String ARG_FREQ_AUDIO = "-ar";
		public static final String ARG_VOLUME_AUDIO = "-vol";
		
		public static final String ARG_STARTTIME = "-ss";
		public static final String ARG_DURATION = "-t";
		
		public static final String ARG_DISABLE_AUDIO = "-an";
		public static final String ARG_DISABLE_VIDEO = "-vn";
	}
}
