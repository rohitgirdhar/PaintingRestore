package com.example.paintingrestore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class CameraActivity extends Activity {

    private Camera mCamera;
    private CameraPreview mPreview;
    private String output_fname;
    public static final String OUTPUT_FNAME = "output";
    public static final int MEDIA_TYPE_IMAGE = 1;
    private static final String TAG = "CameraActivity";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_app);
        
        // Get the output filename
        Intent intent = getIntent();
        output_fname = intent.getStringExtra(OUTPUT_FNAME);
        output_fname = "/sdcard/temp.jpg";
        
        // Create an instance of Camera
        mCamera = getCameraInstance();
        
        if(mCamera == null) {
        	Log.d(TAG, "Couldnot get Camera instance");
        }

        // Create our Preview view and set it as the content of our activity.
        Camera.Parameters cp = mCamera.getParameters();
        cp.setPictureSize(1280, 960);
        mCamera.setParameters(cp);
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        overlayImage();
        
        final PictureCallback mPicture = new PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                if (pictureFile == null) {
                    return;
                }

                try {
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    fos.write(data);
                    fos.close();
                } catch (FileNotFoundException e) {
                    Log.d(TAG, "File not found: " + e.getMessage());
                } catch (IOException e) {
                    Log.d(TAG, "Error accessing file: " + e.getMessage());
                }
                finish();
            }
        };
        
    }
    
    public static Camera getCameraInstance(){
	    Camera c = null;
	    try {
	        c = Camera.open(); // attempt to get a Camera instance
	    }
	    catch (Exception e){
	        // Camera is not available (in use or does not exist)
	    	e.printStackTrace();
	    }
	    return c; // returns null if camera is unavailable
	}
    
    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();              // release the camera immediately on pause event
    }



    private void releaseCamera() {
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    /** Create a File for saving an image or video */
    private  File getOutputMediaFile(int type) {
    	File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(output_fname);
        } else {
            return null;
        }
        return mediaFile;
    }
    
    public void overlayImage() {
    	ImageView overlay = new ImageView(this);
    	overlay.setVisibility(View.VISIBLE);
    	RelativeLayout.LayoutParams overlayParams = 
    			new RelativeLayout.LayoutParams(100, 100);
    	overlay.setBackgroundResource(R.drawable.anime_fire);
    	overlay.setLayoutParams(overlayParams);
    	RelativeLayout relLayout = (RelativeLayout) findViewById(R.id.cameraLayoutMain);
    	relLayout.addView(overlay);
    }
}
