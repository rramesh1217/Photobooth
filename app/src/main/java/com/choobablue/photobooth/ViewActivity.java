package com.choobablue.photobooth;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import com.choobablue.photobooth.R;

import java.io.File;

public class ViewActivity extends ActionBarActivity {

    public static final String EXTRA_FILE_PATH = "filepath";

    private ImageView mImageView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);

        mImageView = (ImageView) findViewById(R.id.imageView);
        String path = getIntent().getStringExtra(EXTRA_FILE_PATH);
        if (path != null) {
            Bitmap image = BitmapFactory.decodeFile(path);
            mImageView.setImageBitmap(image);
        } // else show .... placeholder?
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
