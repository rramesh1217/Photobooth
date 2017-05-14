package com.choobablue.photobooth;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.Image;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ViewFlipper;

import com.choobablue.photobooth.R;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.Future;

public class GalleryActivity extends ActionBarActivity {

    private GridView mGridView;
    private ImageAdapter mImageAdapter;

    private ThumbnailLoader loader;

    // TODO: remove LRU thumbnails beyond x MB ?
    private final Map<File, Bitmap> cacheThumbs = new HashMap();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        loader = new ThumbnailLoader();

        this.mGridView = (GridView) findViewById(R.id.gridView);
        this.mGridView.setAdapter(this.mImageAdapter = new ImageAdapter());
        this.mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showItem(position);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();

        loader.shutdown();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        cacheThumbs.clear();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.gallery, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void showItem(final int position) {
        File file = mImageAdapter.getItem(position);
        final String filename = file.getName();
        if (filename.endsWith(PhotoboothActivity.PHOTO_SUFFIX)) {
            Intent viewIntent = new Intent();
            viewIntent.setClass(GalleryActivity.this, ViewActivity.class);
            viewIntent.putExtra(ViewActivity.EXTRA_FILE_PATH, file.getPath());
            startActivity(viewIntent);
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.fromFile(file));
            try {
                startActivity(intent);
            } catch (android.content.ActivityNotFoundException ex) {
                Log.e(PhotoboothActivity.TAG, "Couldn't view " + file.getName(), ex);
            }
        }
    }


    private static final int IMAGE_BACKGROUND = Color.argb(255, 32, 32, 32);
    private class ImageAdapter extends BaseAdapter {

        private File[] files;


        private ImageAdapter() {
            this.files = getSavedFiles();
        }


        @Override
        public int getCount() {
            return files.length;
        }

        @Override
        public File getItem(int position) {
            return files[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                imageView = new ImageView(GalleryActivity.this.getApplicationContext());
                imageView.setLayoutParams(new GridView.LayoutParams(320, 192));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setBackgroundColor(IMAGE_BACKGROUND);

                Log.d(PhotoboothActivity.TAG, "getView(" + position
                        + ") = ImageView#" + objectIndex(imageView));
            } else {
                Log.d(PhotoboothActivity.TAG, "getView(" + position
                        + ", convert ImageView#" + objectIndex(convertView) + ")");

                imageView = (ImageView) convertView;
                // remove any background loading for this view
                loader.removeAll((ImageView) convertView);
            }

            Bitmap thumbnail;
            File file = files[position];
            synchronized (cacheThumbs) {
                thumbnail = cacheThumbs.get(file);
            }
            if (thumbnail != null) {
                Log.d(PhotoboothActivity.TAG, "used cached thumbnail #" + position
                        + " (" + file.getName() + ") for ImageView#" + objectIndex(imageView));
                imageView.setImageBitmap(thumbnail);
            } else {
                // TODO: set to 'loading' image?
                imageView.setImageBitmap(thumbnail);

                Log.i(PhotoboothActivity.TAG, "submit task: " + file.getName()
                        + " for ImageView#" + objectIndex(imageView));
                loader.submit(imageView, file);
            }
            return imageView;
        }
    }

    private final Map<Object, Integer> objIdx = new HashMap();
    private synchronized int objectIndex(final Object obj) {
        Integer idx = objIdx.get(obj);
        if (idx == null) {
            objIdx.put(obj, idx = objIdx.size());
        }
        return idx;
    }

    private class ThumbnailLoader {

        private Thread worker;
        private boolean keepRunning;
        private final Queue<TaskImpl> queue = new LinkedList<TaskImpl>();

        TaskImpl curTask;

        public void shutdown() {
            synchronized (queue) {
                keepRunning = false;
                queue.clear();
                queue.notify();
                if (worker != null) {
                    worker = null;
                }
            }
        }

        public void submit(final ImageView view, final File file) {
            TaskImpl task = new TaskImpl(view, file);

            synchronized (queue) {
                if (worker == null) {
                    keepRunning = true;
                    worker = new Thread(workTask, "ThumbnailLoader");
                    worker.start();
                }
                queue.offer(task);
                queue.notify();
            }
            //return task.future;
        }

        public boolean removeAll(final ImageView view) {
            boolean result = false;
            synchronized (queue) {
                Iterator<TaskImpl> it = queue.iterator();
                while (it.hasNext()) {
                    TaskImpl task = it.next();
                    if (view.equals(task.view)) {
                        it.remove();
                        result = true;
                    }
                }
                if (curTask != null) {
                    if (view.equals(curTask.view)) {
                        curTask = null;
                        result = true;
                    }
                }
            }
            if (result) {
                Log.i(PhotoboothActivity.TAG, "remove ImageView#" + objectIndex(view));
            }
            return result;
        }

        private final Runnable workTask = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    // get a new task from the queue
                    final TaskImpl task;
                    synchronized (queue) {
                        if (!keepRunning) {
                            return; // shutdown, stop thread
                        }
                        task = queue.poll();
                        if (task == null) {
                            try { // no tasks, wait
                                queue.wait();
                            } catch (InterruptedException ex) {}
                            continue;
                        }
                        curTask = task;
                    }

                    // start work on the next task
                    try {
                        final Bitmap thumbnail;
                        Bitmap cacheValue;
                        synchronized (cacheThumbs) {
                            cacheValue = cacheThumbs.get(task.file);
                        }
                        if (cacheValue == null) {
                            Log.i(PhotoboothActivity.TAG, "generating thumbnail of "
                                    + task.file.getName() + " for ImageView#" + objectIndex(task.view));
                            thumbnail = generateThumbnail(task.file);
                            synchronized (cacheThumbs) {
                                cacheThumbs.put(task.file, thumbnail);
                            }
                        } else {
                            Log.i(PhotoboothActivity.TAG, "re-use thumbnail of "
                                    + task.file.getName() + " for ImageView#" + objectIndex(task.view));
                            thumbnail = cacheValue;
                        }
                        // make sure task isn't canceled (remove while in progress)
                        if (curTask != null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    task.view.setImageBitmap(thumbnail);
                                }
                            });
                        } else {
                            Log.i(PhotoboothActivity.TAG, "discarding removed thumbnail of "
                                    + task.file.getName() + " for ImageView#" + objectIndex(task.view));
                        }
                    } catch (Exception ex) {
                        Log.w(PhotoboothActivity.TAG,
                                "error loading thumbnail for file" + task.file.getName(), ex);
                    }
                }
            }
        };

        private class TaskImpl {
            private final ImageView view;
            private final File file;
            public TaskImpl(final ImageView view, final File file) {
                this.view = view;
                this.file = file;
            }
        }
    }


    private Bitmap generateThumbnail(final File file) {
        Bitmap thumbnail;
        final String filename = file.getName();
        if (filename.endsWith(PhotoboothActivity.PHOTO_SUFFIX)) {
            thumbnail = BitmapFactory.decodeFile(file.getPath());
            thumbnail = ThumbnailUtils.extractThumbnail(thumbnail, 185, 105);
        } else if (filename.endsWith(PhotoboothActivity.VIDEO_SUFFIX)) {
            // generate thumbnail with overlay video 'play' graphic
            thumbnail = ThumbnailUtils.createVideoThumbnail(file.getPath(),
                    MediaStore.Video.Thumbnails.MINI_KIND);
            thumbnail = overlayVideoIcon(thumbnail);
        } else {
            // TODO: load ? thumbnail
            thumbnail = null;
        }
        return thumbnail;
    }

    private Bitmap overlayVideoIcon(final Bitmap original) {
        Bitmap overlay = BitmapFactory.decodeResource(getResources(), R.drawable.video_overlay);
        Canvas cnvs = new Canvas(original);
        final int w = original.getWidth();
        final int h = original.getHeight();

        final int s = Math.min(w, h) * 1 / 2;
        final int l = (w - s) / 2;
        final int t = (h - s) / 2;

        Rect destRect = new Rect(l, t, l + s, t + s);
        cnvs.drawBitmap(overlay, null, destRect, null);
        return original;
    }


    public static File[] getSavedFiles() {
        File dir = PhotoboothActivity.getStorageDirectory();

        return dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(PhotoboothActivity.PHOTO_SUFFIX)
                        || filename.endsWith(PhotoboothActivity.VIDEO_SUFFIX);
            }
        });
    }

}
