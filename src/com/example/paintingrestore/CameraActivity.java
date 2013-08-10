package com.example.paintingrestore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.highgui.Highgui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
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
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class CameraActivity extends Activity 
            implements SensorEventListener{

    private Camera mCamera;
    private CameraPreview mPreview;
    private String output_fname;
    private FrameLayout preview;
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
    private final int MIN_CYCLES = 12; // min num cycles to stay stable to click
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
        preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        accView = (TextView) findViewById(R.id.accView);
        accView.setTextColor(Color.WHITE);
        overlay = new ImageView(this);
        
        Matrix matrix=new Matrix();
        overlay.setScaleType(ScaleType.MATRIX);   //required
        matrix.postRotate((float) 90, 0, 0);
        overlay.setImageMatrix(matrix);
        
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
        if (mCamera != null) {
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
    
    public void overlayImage(double x, double y, double x2, double y2) {
        Log.v("got", Double.toString(x) + " " + Double.toString(y));
        overlaying = true;
    	overlay.setVisibility(View.VISIBLE);
    	RelativeLayout.LayoutParams overlayParams = 
    			new RelativeLayout.LayoutParams((int) Math.abs(x2-x), (int) Math.abs(y2-y));
    	overlay.setBackgroundResource(R.drawable.original_image);
        overlayParams.topMargin = (int) x;
        overlayParams.leftMargin = (int) y;
    	overlay.setLayoutParams(overlayParams);
    }
    
    public void removeOverlayImage() {
        //overlaying = false;
        //overlay.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        computeOverlayPosition();
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
                stable = false;
                mCamera.autoFocus(autoFocusCallback);
                //overlayImage();
            }
        } else if (net_acc < ACC_THRESH) {
            stable = true;
        } else {
            cycles = 0;
            stable = false;
            //removeOverlayImage();
        }
        accView.setText(Integer.toString(cycles));
    }
    
    AutoFocusCallback autoFocusCallback = new AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            mCamera.takePicture(null, null, mPicture);
        }
    };
    
    PictureCallback mPicture = new PictureCallback() {
      @Override
      public void onPictureTaken(byte[] data, Camera camera) {
          saveFile(data);
          Mat image = Highgui.imread("/sdcard/PaintingRestore_test_image.jpg");
          Mat orig = Highgui.imread("/sdcard/PaintingRestore_act_image.jpg");
          if (image.empty() || orig.empty()) {
              return;
          }
          
          Mat H = new Mat();
          computeHomography(orig.getNativeObjAddr(), image.getNativeObjAddr(), H.getNativeObjAddr());
          Point p = Util.getPointOnOrig(H, new Point(0,orig.rows()));
          Point p2 = Util.getPointOnOrig(H, new Point(orig.cols(),0));
          Log.v(TAG, Double.toString(p.x) + " " + Double.toString(p.y));
          overlayImage(p.x*preview.getHeight()/image.cols(), preview.getWidth() - (p.y)*preview.getWidth()/image.rows(), 
                  p2.x*preview.getHeight()/image.cols(), preview.getWidth() - (p2.y)*preview.getWidth()/image.rows());
          //camera.startPreview();
      }
    };
    
    private void saveFile(byte[] data) {
        File test_image_file = new File("/sdcard/PaintingRestore_test_image.jpg");
        File media_file = new File(test_image_file.getAbsolutePath());
        try {
            FileOutputStream fos = new FileOutputStream(media_file);
            fos.write(data);
            fos.close();
        } catch (FileNotFoundException e) {
            finish();
        } catch (IOException e) {
            finish();
        }
    }
    
    static {
        System.loadLibrary("vision");
        System.loadLibrary("opencv_java");
    }
    
    public native void computeOverlayPosition();
    public native void computeHomography(long addrOrig, long addrImage, long addrH);
}
