package com.choobablue.photobooth.publisher;

import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;

import com.choobablue.photobooth.PhotoboothActivity;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Future;

/**
 * Created by Matt McHenry on 8/29/2014.
 */
public class HttpPostPublisher implements MediaPublisher {

    final Context context;
    final HttpClient client;

    final HttpHost host;


    public HttpPostPublisher(final Context context, final HttpHost host) {
        this.context = context;
        this.client = AndroidHttpClient.newInstance("com.choobablue.Photobooth", context);
        this.host = host;
    }


    public Future<?> publishMedia(final File file, boolean isVideo) {
        PostTask task = new PostTask(file, isVideo);
        task.execute();
        return task;
    }


    private class PostTask extends AsyncTask implements Future {
        final File file;
        final boolean isVideo;

        public PostTask(final File file, final boolean isVideo) {
            this.file = file;
            this.isVideo = isVideo;
        }

        @Override
        public boolean isDone() {
            return getStatus() == Status.FINISHED;
        }

        @Override
        protected Object doInBackground(Object[] params) {
            HttpPost post = new HttpPost("/" + file.getName());
            post.setEntity(new FileEntity(file, isVideo ? "video/mp4" : "image/jpeg"));

            try {
                HttpResponse resp = client.execute(host, post);

                int statusCode = resp.getStatusLine().getStatusCode();
                if (statusCode < 200 || statusCode >= 300) {
                    Log.e(PhotoboothActivity.TAG, "error posting file: " +file.getName()
                        + ": " + resp.getStatusLine());
                } else {
                    Log.i(PhotoboothActivity.TAG, "successfully uploaded file: " + file.getName());
                }
            } catch (IOException ex) {
                Log.e(PhotoboothActivity.TAG, "error posting file: " + file.getName(), ex);
            }

            return null;
        }
    }

}
