package com.piusvelte.sonet.loader;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;

/**
 * Created by bemmanuel on 5/24/15.
 */
public class PhotoPathLoader extends BaseAsyncTaskLoader<String> {

    private Context mContext;
    private Uri mUri;

    public PhotoPathLoader(Context context, @NonNull Uri uri) {
        super(context);
        mContext = context.getApplicationContext();
        mUri = uri;
    }

    @Override
    public String loadInBackground() {
        String path;
        Cursor cursor = mContext.getContentResolver().query(mUri,
                new String[] { MediaStore.Images.Media.DATA },
                null,
                null,
                null);

        if (cursor.moveToFirst()) {
            path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        } else {
            // some file managers send the path in the uri
            path = mUri.getPath();
        }

        cursor.close();
        return path;
    }
}
