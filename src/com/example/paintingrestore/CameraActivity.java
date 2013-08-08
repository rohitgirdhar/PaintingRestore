package com.example.paintingrestore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class CameraActivity extends Activity 
            implements SensorEventListener{

    private Camera mCamera;
    private CameraPreview mPreview;
    private String output_fname;
    public static final String OUTPUT_FNAME = "output";
    public static final int MEDIA_TYPE_IMAGE = 1;
    private static final String TAG = "CameraActivity";
    private ImageView overlay = null;
    private SensorManager mSensorManager = null;
    private Sensor mSensor = null;
    private TextView accView = null;
    private boolean stable = false;  // true if display stable
    private final double ACC_THRESH = 1.3; // max acc to say "stable"
    private int cycles = 0;
    private final int MIN_CYCLES = 8; // min num cycles to stay stable to click
    private boolean overlaying = false;
    
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
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        accView = (TextView) findViewById(R.id.accView);
        accView.setTextColor(Color.WHITE);
        overlay = new ImageView(this);
        RelativeLayout relLayout = (RelativeLayout) 
                findViewById(R.id.cameraLayoutMain);
        relLayout.addView(overlay);
        
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
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();              // release the camera immediately on pause event
        mSensorManager.unregisterListener(this);
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
        overlaying = true;
    	overlay.setVisibility(View.VISIBLE);
    	RelativeLayout.LayoutParams overlayParams = 
    			new RelativeLayout.LayoutParams(100, 100);
    	overlay.setBackgroundResource(R.drawable.anime_fire);
    	overlay.setLayoutParams(overlayParams);
    }
    
    public void removeOverlayImage() {
        overlaying = false;
        overlay.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float vals[] = new float[3];
        vals[0] = event.values[0];
        vals[1] = event.values[1];
        vals[2] = event.values[2];
        double net_acc = Math.pow(vals[0], 2) + 
                          Math.pow(vals[1], 2) + 
                          Math.pow(vals[2], 2);
        net_acc = Math.sqrt(net_acc);
        
        if (stable && net_acc < ACC_THRESH) {
            cycles ++;
            if (cycles >= MIN_CYCLES && !overlaying) {
                //mCamera.autoFocus(autoFocusCallback);
                stable = false;
                cycles = 0;
                overlayImage();
            }
        } else if (net_acc < ACC_THRESH) {
            stable = true;
        } else {
            cycles = 0;
            stable = false;
            removeOverlayImage();
        }
        accView.setText(Integer.toString(cycles));
    }
}
