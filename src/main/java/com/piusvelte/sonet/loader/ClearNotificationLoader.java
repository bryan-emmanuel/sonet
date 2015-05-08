package com.piusvelte.sonet.loader;

import android.content.ContentValues;
import android.content.Context;

import com.piusvelte.sonet.provider.Notifications;

/**
 * Created by bemmanuel on 5/7/15.
 */
public class ClearNotificationLoader extends BaseAsyncTaskLoader {

    private static final long CLEAR_ALL = -1;

    private Context mContext;
    private long mClearId = CLEAR_ALL;

    public ClearNotificationLoader(Context context) {
        super(context);
        mContext = context.getApplicationContext();
    }

    public ClearNotificationLoader(Context context, long clearId) {
        this(context);
        mClearId = clearId;
    }

    @Override
    public Object loadInBackground() {
        ContentValues values = new ContentValues();
        values.put(Notifications.CLEARED, 1);
        mContext.getContentResolver()
                .update(Notifications.getContentUri(mContext),
                        values,
                        mClearId > CLEAR_ALL ? Notifications._ID + "=?" : null,
                        mClearId > CLEAR_ALL ? new String[] { Long.toString(mClearId) } : null);
        return null;
    }
}
