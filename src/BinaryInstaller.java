

/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.Context;
import android.util.Log;

import com.example.android_ffmpeg_cmdline.R;


public class BinaryInstaller  {

	
	private File mInstallFolder;
	private Context mContext;
	
    private static int isARMv6 = -1;
    private static String CHMOD_EXEC = "700";
    
    private final static int FILE_WRITE_BUFFER_SIZE = 32256;
    
	public BinaryInstaller (Context context, File installFolder) {
		mContext = context;
		mInstallFolder = installFolder;
	}
	
	
	public boolean installFromRaw () throws IOException, FileNotFoundException {
		InputStream is;
        File outFile;
        
		is = mContext.getResources().openRawResource(R.raw.ffmpeg);
		outFile = new File(mInstallFolder, "ffmpeg");
		streamToFile(is, outFile, false, false, CHMOD_EXEC);
		
	
		return true;
	}
	
	
	private static void copyAssetFile(Context ctx, String asset, File file) 
	        throws IOException, InterruptedException {
    	
		DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
		InputStream is = new GZIPInputStream(ctx.getAssets().open(asset));
		
		byte buf[] = new byte[8172];
		int len;
		while ((len = is.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		out.close();
		is.close();
	}
	
	/*
	 * Write the inputstream contents to the file
	 */
    private static boolean streamToFile(InputStream stm, File outFile, boolean append, 
            boolean zip, String mode) throws IOException {
        
        byte[] buffer = new byte[FILE_WRITE_BUFFER_SIZE];

        int bytecount;
    	OutputStream stmOut = new FileOutputStream(outFile, append);
    	
    	if (zip) {
    		ZipInputStream zis = new ZipInputStream(stm);    		
    		ZipEntry ze = zis.getNextEntry();
    		stm = zis;
    	}
    	
        while ((bytecount = stm.read(buffer)) > 0) {
            stmOut.write(buffer, 0, bytecount);
        }
        stmOut.close();
        stm.close();
        
		doChmod(outFile, mode);

        return true;
    }
	
    //copy the file from inputstream to File output - alternative impl
	public void copyFile (InputStream is, File outputFile) {
		try {
			outputFile.createNewFile();
			DataOutputStream out = new DataOutputStream(new FileOutputStream(outputFile));
			DataInputStream in = new DataInputStream(is);
			
			int b = -1;
			byte[] data = new byte[1024];
			
			while ((b = in.read(data)) != -1) {
				out.write(data);
			}
			
			if (b == -1); //rejoice
			
			//
			out.flush();
			out.close();
			in.close();
			// chmod?
			
		} catch (IOException ex) {
			Log.e("ffmpeg", "Error copying binary", ex);
		}
	}
	
	

    /**
	 * Check if this is an ARMv6 device
	 * @return true if this is ARMv6
	 */
	private static boolean isARMv6() {
		if (isARMv6 == -1) {
			BufferedReader r = null;
			try {
				isARMv6 = 0;
				r = new BufferedReader(new FileReader("/proc/cpuinfo"));
				for (String line = r.readLine(); line != null; line = r.readLine()) {
					if (line.startsWith("Processor") && line.contains("ARMv6")) {
						isARMv6 = 1;
						break;
					} else if (line.startsWith("CPU architecture") && (line.contains("6TE") 
					        || line.contains("5TE"))) {
						isARMv6 = 1;
						break;
					}
				}
			} catch (Exception ex) {
			    // TODO: Log the exception
			} finally {
				if (r != null) { 
				    try {
				        r.close();
				    } catch (IOException ex) {
				        // TODO: Log the exception
				    }
				}
			}
		}
		return (isARMv6 == 1);
	}
	
	/**
	 * Copies a raw resource file, given its ID to the given location
	 * @param ctx context
	 * @param resid resource id
	 * @param file destination file
	 * @param mode file permissions (E.g.: "755")
	 * @throws IOException on error
	 * @throws InterruptedException when interrupted
	 */
	private static void copyRawFile(Context ctx, int resid, File file, String mode, 
	        boolean isZipd) throws IOException, InterruptedException {
	        
		final FileOutputStream out = new FileOutputStream(file);
		InputStream is = ctx.getResources().openRawResource(resid);
		
		if (isZipd) {
    		ZipInputStream zis = new ZipInputStream(is);    		
    		ZipEntry ze = zis.getNextEntry();
    		is = zis;
    	}
		
		byte buf[] = new byte[1024];
		int len;
		while ((len = is.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		out.close();
		is.close();
		// Change the permissions
		doChmod(file, mode).waitFor();
	}
	
	public static Process doChmod(File file, String mode) throws IOException {
	    String filepath = file.getAbsolutePath();
	    return Runtime.getRuntime().exec("chmod " + mode + " " + filepath);
	}
}
