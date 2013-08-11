package com.example.paintingrestore;

import android.os.Bundle;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class ImageDisplay extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_display);
        
        Bitmap myBitmap = BitmapFactory.decodeFile("/sdcard/PaintingRestore_final.jpg");
        ImageView overlay = (ImageView) findViewById(R.id.resultImage);
        //overlay.setImageBitmap(Bitmap.createScaledBitmap(myBitmap, overlay.getHeight(), overlay.getWidth(), false));
        //overlay.setBackgroundResource(R.drawable.original_image);
        overlay.setImageBitmap(myBitmap);
        overlay.setScaleType(ScaleType.FIT_XY);
        overlay.setVisibility(View.VISIBLE);
        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.image_display, menu);
        return true;
    }

}
