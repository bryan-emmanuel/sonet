package com.piusvelte.sonet.core;

import static com.piusvelte.sonet.core.Sonet.sBFOptions;

import java.lang.ref.WeakReference;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;

public class BitmapDownloadTask extends AsyncTask<String, Void, Bitmap> {
	
	private final WeakReference<ImageView> mImageViewReference;
	private final HttpClient mHttpClient;
	
	public BitmapDownloadTask(ImageView imageView, HttpClient httpClient) {
		mImageViewReference = new WeakReference<ImageView>(imageView);
		mHttpClient = httpClient;
	}

	@Override
	protected Bitmap doInBackground(String... params) {
		byte[] blob = SonetHttpClient.httpBlobResponse(mHttpClient, new HttpGet(params[0]));
		return BitmapFactory.decodeByteArray(blob, 0, blob.length, sBFOptions);
	}

	@Override
	protected void onPostExecute(Bitmap bitmap) {
        if (mImageViewReference != null) {
            ImageView imageView = mImageViewReference.get();
            BitmapDownloadTask bitmapDownloadTask = getBitmapDownloadTask(imageView);
            if (this == bitmapDownloadTask)
                imageView.setImageBitmap(bitmap);
        }
	}
	
    private static BitmapDownloadTask getBitmapDownloadTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof DownloadedDrawable) {
                DownloadedDrawable downloadedDrawable = (DownloadedDrawable)drawable;
                return downloadedDrawable.getBitmapDownloadTask();
            }
        }
        return null;
    }
	
	static class DownloadedDrawable extends ColorDrawable {
        private final WeakReference<BitmapDownloadTask> bitmapDownloadTaskReference;

        public DownloadedDrawable(BitmapDownloadTask bitmapDownloadTask) {
            super(Color.BLACK);
            bitmapDownloadTaskReference = new WeakReference<BitmapDownloadTask>(bitmapDownloadTask);
        }

        public BitmapDownloadTask getBitmapDownloadTask() {
            return bitmapDownloadTaskReference.get();
        }
    }

}
