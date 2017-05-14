package com.choobablue.photobooth;

import com.choobablue.photobooth.publisher.HttpPostPublisher;
import com.choobablue.photobooth.publisher.MediaPublisher;
import com.choobablue.photobooth.view.CameraPreview;
import com.choobablue.photobooth.util.SystemUiHider;
import com.choobablue.photobooth.view.CountdownView;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import org.apache.http.HttpHost;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class PhotoboothActivity extends Activity {

    public static final String PHOTO_SUFFIX = ".jpg";
    public static final String VIDEO_SUFFIX = ".mp4";

    public static final String TAG = "Photobooth";


    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    //private MediaPublisher mPublisher;

    private FrameLayout mPreviewContainer;
    private ImageView mPhotoReview;
    private VideoView mVideoReview;
    private View mHomeControlsView;
    private Button mVideoStartBtn;
    private Button mPhotoStartBtn;
    private View mCountdownContainer;
    private TextView mCountdownTitle;
    private CountdownView mCountdownView;
    private View mVideoOverlayContainer;
    private ImageView mRecordingIndicator;
    private ProgressBar mVideoProgress;
    private CountdownView mVideoCountdown;

    private Camera mCamera;
    private int mCameraId;
    private CameraPreview mPreview;
    private MediaRecorder mMediaRecorder;

    private PhotoTask mPhotoTask;
    private VideoTask mVideoTask;

    private Handler mHandlerMain = new Handler(Looper.getMainLooper());

    private PowerManager.WakeLock mWaitLock;

    private boolean hidden = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photobooth);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWaitLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");

        //getSharedPreferences()
        //final String laptopIpAddress = "10.136.44.162";
        //this.mPublisher = new HttpPostPublisher(this.getApplicationContext(), new HttpHost(laptopIpAddress, 8080));

        this.mPreviewContainer = (FrameLayout) findViewById(R.id.camera_preview);
        this.mPhotoReview = (ImageView) findViewById(R.id.photo_review);
        this.mVideoReview = (VideoView) findViewById(R.id.video_review);
        this.mHomeControlsView = findViewById(R.id.home_content_controls);
        this.mVideoStartBtn = (Button) findViewById(R.id.video_start_button);
        this.mPhotoStartBtn = (Button) findViewById(R.id.photo_start_button);
        this.mCountdownContainer = findViewById(R.id.countdown_container);
        this.mCountdownTitle = (TextView) findViewById(R.id.countdown_title);
        this.mCountdownView = (CountdownView) findViewById(R.id.countdown_view);
        this.mVideoOverlayContainer = findViewById(R.id.video_overlay_container);
        this.mRecordingIndicator = (ImageView) findViewById(R.id.record_indicator_view);
        this.mVideoProgress = (ProgressBar) findViewById(R.id.video_progress_bar);
        this.mVideoCountdown = (CountdownView) findViewById(R.id.video_countdown_view);

        // Set up an instance of SystemUiHider to control the system UI for this activity.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            View hideUiTarget = findViewById(R.id.hide_ui_target);
            mSystemUiHider = SystemUiHider.getInstance(this, hideUiTarget, HIDER_FLAGS);
            mSystemUiHider.setup();
            mSystemUiHider
                    .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                        // Cached values.
                        int mControlsHeight;
                        int mShortAnimTime;

                        @Override
                        @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                        public void onVisibilityChange(boolean visible) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                                // If the ViewPropertyAnimator API is available
                                // (Honeycomb MR2 and later), use it to animate the
                                // in-layout UI controls at the bottom of the
                                // screen.
                                //                            if (mControlsHeight == 0) {
                                //                                mControlsHeight = controlsView.getHeight();
                                //                            }
                                //                            if (mShortAnimTime == 0) {
                                //                                mShortAnimTime = getResources().getInteger(
                                //                                        android.R.integer.config_shortAnimTime);
                                //                            }
                                //                            controlsView.animate()
                                //                                    .translationY(visible ? 0 : mControlsHeight)
                                //                                    .setDuration(mShortAnimTime);
                            } else {
                                // If the ViewPropertyAnimator APIs aren't
                                // available, simply show or hide the in-layout UI
                                // controls.
                                //                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                            }

                            if (visible && AUTO_HIDE) {
                                // Schedule a hide().
                                delayedHide(AUTO_HIDE_DELAY_MILLIS);
                            }
                        }
                    });
        }

        // Set up the user interaction to manually show or hide the system UI.
//        this.mPreview.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                //if (TOGGLE_ON_CLICK) {
//                //    mSystemUiHider.toggle();
//                //} else {
//                //    mSystemUiHider.show();
//                //}
//            }
//        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        //findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

        this.mVideoStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!hidden) {
                    hideUi();
                    hidden = true;
                }
                mHomeControlsView.setVisibility(View.GONE);
                mVideoTask = new VideoTask();
                mVideoTask.start();
            }
        });
        this.mPhotoStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!hidden) {
                    hideUi();
                    hidden = true;
                }
                mHomeControlsView.setVisibility(View.GONE);
                mPhotoTask = new PhotoTask();
                mPhotoTask.start();
            }
        });

        hideUiPartial();
    }

    @Override
    protected void onStart() {
        super.onStart();
        prepareCamera();
        mWaitLock.acquire();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // TODO: stop any active task (taking photos/videos)
        releaseCamera();
        mWaitLock.release();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
    }


    private void hideUi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mPreviewContainer.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | Window.FEATURE_ACTION_BAR
                            | Window.FEATURE_ACTION_BAR_OVERLAY
            );
        }
    }

    private void hideUiPartial() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mPreviewContainer.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | Window.FEATURE_ACTION_BAR
                            | Window.FEATURE_ACTION_BAR_OVERLAY
            );
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            // Trigger the initial hide() shortly after the activity has been
            // created, to briefly hint to the user that UI controls
            // are available.
            delayedHide(100);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.photobooth, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_gallery) {
            Intent galleryIntent = new Intent();
            galleryIntent.setClass(PhotoboothActivity.this, GalleryActivity.class);
            startActivity(galleryIntent);
            return true;
        } else if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent();
            settingsIntent.setClass(PhotoboothActivity.this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            if (mSystemUiHider != null) {
                mSystemUiHider.hide();
            }
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }


    private void startCountdownTimer(final String title, final Runnable task, final long millis) {
        mHandlerMain.postDelayed(task, millis);

        mCountdownTitle.setText(title);
        mCountdownView.setEndMillis(SystemClock.elapsedRealtime() + millis);
        mCountdownContainer.setVisibility(View.VISIBLE);
        mCountdownView.start();
    }

    private class PhotoTask {
        private static final int TOTAL_PICTURES = 4;

        private int count, total;
        private Date date = new Date();
        private Bitmap composite;

        private File[] filesOrig = new File[TOTAL_PICTURES];
        private File fileComp;

        private PhotoTask() {
            this.count = 0;
            this.total = TOTAL_PICTURES;
        }

        public void start() {
            doLoop();
        }

        private boolean doLoop() {
            if (count++ < this.total) {
                startCountdownTimer("Photo " + count + " of " + total, new Runnable() {
                    @Override
                    public void run() {
                        mCountdownContainer.setVisibility(View.GONE);
                        mCamera.takePicture(null, null, mPictureCallback);
                    }
                }, 4000);
                return true;
            } else {
                // save result image and show to user
                reviewImage(composite);

                FileOutputStream fos = null;
                try {
                    fileComp = getCompositeImageFile();
                    fos = new FileOutputStream(fileComp);
                    composite.compress(Bitmap.CompressFormat.JPEG, 90, fos);

                    //mPublisher.publishMedia(fileComp, false);
                } catch (Exception ex) {
                    Log.e(TAG, "Error saving composite", ex);
                } finally {
                    try {
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException ex) {
                        Log.e(TAG, "Error closing file", ex);
                    }
                }

                mHandlerMain.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        end();
                    }
                }, 8000);
                return false;
            }
        }

        private void deleteFiles() {
            for (File file : this.filesOrig) {
                if (file != null) {
                    file.delete();
                }
            }
            if (fileComp != null) {
                fileComp.delete();
            }
        }

        private void end() {
            reviewImage(null);
            mCountdownContainer.setVisibility(View.GONE);
            mHomeControlsView.setVisibility(View.VISIBLE);
            mCamera.startPreview();
        }

        private void reviewImage(final Bitmap image) {
            mPhotoReview.setImageBitmap(image);
            mPhotoReview.setVisibility(image!=null ? View.VISIBLE : View.GONE);
            mPreview.setVisibility(image!=null ? View.GONE : View.VISIBLE);
        }

        private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                final int i = count - 1;
                final File imageFile = getOutputImageFile();
                if (imageFile == null) {
                    // TODO: show error message to user "Error creating media file, check storage permissions"
                    end();
                    return;
                }

                filesOrig[i] = imageFile;
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(imageFile);
                    fos.write(data);
                } catch (Exception ex) {
                    Log.e(TAG, "Error saving photo", ex);
                } finally {
                    try {
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException ex) {
                        Log.e(TAG, "Error closing file", ex);
                    }
                }
                Log.i(TAG, "saved photo: \"" + imageFile.getAbsolutePath() + "\" exists?" + imageFile.exists());

                final Bitmap img = BitmapFactory.decodeByteArray(data, 0, data.length);
                final int w = img.getWidth();
                final int h = img.getHeight();
                if (composite == null) {
                    composite = Bitmap.createBitmap(w*2, h*2, img.getConfig());
                }
                Canvas cnvs = new Canvas(composite);
                cnvs.drawBitmap(img, null, new Rect(
                        (i%2 == 0) ? 0 : w, // left
                        (i/2 == 0) ? 0 : h, // top
                        (i%2 == 0) ? w : w*2, // right
                        (i/2 == 0) ? h : h*2), // bottom
                        null);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Matrix m = new Matrix();
                        m.preScale(-1, 1);
                        Bitmap flipped = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), m, false);
                        flipped.setDensity(DisplayMetrics.DENSITY_DEFAULT);
                        reviewImage(flipped);
                    }
                });
                mHandlerMain.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        reviewImage(null);
                        mCamera.startPreview();
                        mCountdownView.setVisibility(View.VISIBLE);
                        doLoop();
                    }
                }, 2000);
            }
        };


        private File getOutputImageFile() {
            File dir = getStorageDirectory();
            if (dir == null) { return null; }

            File subdir = new File(dir, "orig");
            if (!subdir.exists()) {
                subdir.mkdirs();
            }

            // Create a media file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(date);
            return new File(subdir, "IMG_"+ timeStamp + "_" + count + PHOTO_SUFFIX);
        }

        private File getCompositeImageFile() {
            File dir = getStorageDirectory();
            if (dir == null) { return null; }

            // Create a media file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(date);
            return new File(dir.getPath() + File.separator +
                    "IMG_"+ timeStamp + PHOTO_SUFFIX);
        }
    }

    private class VideoTask {
        private File videoFile;
        private long durationMillis = 15000;

        private long startTimeMillis;
        private Timer progressTimer = new Timer();

        public void start() {
            try {
                this.prepareMediaRecorder();

                startCountdownTimer("Record Video", new Runnable() {
                    @Override
                    public void run() {
                        mCountdownContainer.setVisibility(View.GONE);
                        mVideoOverlayContainer.setVisibility(View.VISIBLE);

                        startTimeMillis = SystemClock.elapsedRealtime();
                        mVideoCountdown.setEndMillis(startTimeMillis + durationMillis);
                        mVideoProgress.setMax((int) durationMillis);
                        mVideoProgress.setProgress(0);
                        progressTimer.scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {
                                mHandlerMain.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        long elapsed = SystemClock.elapsedRealtime() - startTimeMillis;
                                        mVideoProgress.setProgress((int) elapsed);
                                        mRecordingIndicator.setVisibility((elapsed % 1000) < 500 ? View.VISIBLE : View.INVISIBLE);
                                    }
                                });
                            }
                        }, 50, 50);

                        // start recording video
                        mMediaRecorder.start();
                        mVideoCountdown.start();
                        mHandlerMain.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                stop();
                                //reviewVideo();
                                end();
                            }
                        }, durationMillis);
                    }
                }, 4000);
            } catch (Exception ex) {
                Log.e(TAG, "error preparing video", ex);
                this.end();
            }
        }

        private void stop() {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();

            mVideoCountdown.stop();
            mVideoOverlayContainer.setVisibility(View.GONE);
            progressTimer.cancel();

            Log.i(TAG, "saved video: \"" + videoFile.getAbsolutePath() + "\" exists?" + videoFile.exists());

            //mPublisher.publishMedia(videoFile, true);
        }

        private void reviewVideo() {
            try {
                mVideoReview.setVideoURI(Uri.fromFile(videoFile));
                mVideoReview.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mVideoReview.setVideoURI(null);
                        mVideoReview.setVisibility(View.GONE);
                        mPreview.setVisibility(View.VISIBLE);

                        end();
                    }
                });

                mVideoReview.setVisibility(View.VISIBLE);
                mPreview.setVisibility(View.GONE);
                mVideoReview.start();
            } catch (Exception ex) {
                Log.e(TAG, "error reviewing video", ex);
                end();
            }
        }

        private void end() {
            mCamera.lock();

            mHomeControlsView.setVisibility(View.VISIBLE);
        }

        private void prepareMediaRecorder() throws IOException {
            mMediaRecorder = new MediaRecorder();

            mCamera.unlock();
            mMediaRecorder.setCamera(mCamera);

            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

            CamcorderProfile profile;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                profile = CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_HIGH);
            } else {
                profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
            }
            mMediaRecorder.setProfile(profile);

            videoFile = getOutputVideoFile();
            mMediaRecorder.setOutputFile(videoFile.toString());

            mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

            mMediaRecorder.prepare();
        }

        private File getOutputVideoFile() {
            File dir = getStorageDirectory();
            if (dir == null) { return null; }

            // Create a media file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            return new File(dir.getPath() + File.separator +
                    "VID_"+ timeStamp + VIDEO_SUFFIX);
        }
    }



    private void prepareCamera() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                int cameraId = 0;
                final int count = Camera.getNumberOfCameras();
                for (int idx = 0; idx < count; idx++) {
                    Camera.CameraInfo info = new Camera.CameraInfo();
                    Camera.getCameraInfo(idx, info);
                    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        cameraId = idx;
                    }
                }
                mCamera = Camera.open(cameraId);
                mCameraId = cameraId;
            } else {
                mCamera = Camera.open();
                mCameraId = 0;
            }

            // Create our Preview view and set it as the content of our activity.
            mPreview = new CameraPreview(this, mCamera);
            mPreviewContainer.addView(mPreview);

            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mPreview.getLayoutParams();
            params.gravity = Gravity.CENTER;
            mPreview.setLayoutParams(params);
        } catch (Exception ex) {
            Log.e("PhotoboothActivity", "error opening camera", ex);
            // TODO: inform the user an error occurred
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
            } catch (Exception ex) { }

            if (mPreview != null) {
                mPreviewContainer.removeView(mPreview);
                mPreview = null;
            }

            mCamera.release();
            mCamera = null;
        }
    }



    public static File getStorageDirectory() {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Photobooth");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }
        return mediaStorageDir;
    }

}
