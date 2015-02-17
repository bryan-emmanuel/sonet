package com.piusvelte.sonet;

import static com.piusvelte.sonet.Sonet.getBlob;
import static com.piusvelte.sonet.Sonet.sBFOptions;

import com.piusvelte.sonet.Sonet.Statuses_styles;
import com.piusvelte.sonet.Sonet.Widgets;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

@SuppressLint("NewApi")
public class SonetRemoteViewsFactory implements android.widget.RemoteViewsService.RemoteViewsFactory {
    private static final String TAG = "SonetRemoteViewsFactory";
    private Context mContext;
    private Cursor mCursor;
    private int mAppWidgetId;
    private boolean mDisplay_profile;

    public SonetRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        mDisplay_profile = intent.getBooleanExtra(Widgets.DISPLAY_PROFILE, true);
    }

    @Override
    public int getCount() {
        if (mCursor != null)
            return mCursor.getCount();
        else
            return 0;
    }

    @Override
    public long getItemId(int position) {
        if ((mCursor != null) && !mCursor.isClosed() && mCursor.moveToPosition(position))
            return mCursor.getLong(mCursor.getColumnIndex(Statuses_styles._ID));
        return position;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    private static boolean setImageViewBitmap(RemoteViews remoteViews, int viewId, byte[] blob) {
        if (blob != null) {
            Bitmap bmp = BitmapFactory.decodeByteArray(blob, 0, blob.length, sBFOptions);

            if (bmp != null) {
                remoteViews.setImageViewBitmap(viewId, bmp);
                return true;
            }
        }

        return false;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        // load the item
        RemoteViews views;

        if ((mCursor != null) && !mCursor.isClosed() && mCursor.moveToPosition(position)) {
            int friend_color = mCursor.getInt(6),
                    created_color = mCursor.getInt(7),
                    friend_textsize = mCursor.getInt(9),
                    created_textsize = mCursor.getInt(10),
                    messages_color = mCursor.getInt(5),
                    messages_textsize = mCursor.getInt(8);
            views = new RemoteViews(mContext.getPackageName(), mDisplay_profile ? R.layout.widget_item : R.layout.widget_item_noprofile);
            // set icons
            byte[] icon = mCursor.getBlob(12);
            setImageViewBitmap(views, R.id.icon, icon);

            views.setTextViewText(R.id.friend_bg_clear, mCursor.getString(1));
            views.setFloat(R.id.friend_bg_clear, "setTextSize", friend_textsize);
            views.setTextViewText(R.id.message_bg_clear, mCursor.getString(3));
            views.setFloat(R.id.message_bg_clear, "setTextSize", messages_textsize);
            // set messages background
            byte[] status_bg = mCursor.getBlob(11);
            setImageViewBitmap(views, R.id.status_bg, status_bg);

            views.setTextViewText(R.id.message, mCursor.getString(3));
            views.setTextColor(R.id.message, messages_color);
            views.setFloat(R.id.message, "setTextSize", messages_textsize);

            // Set the click intent so that we can handle it and show a toast message
            final Intent fillInIntent = new Intent();
            final Bundle extras = new Bundle();
            extras.putString(Sonet.Status_links.STATUS_ID, Long.toString(mCursor.getLong(0)));
            fillInIntent.putExtras(extras);
            views.setOnClickFillInIntent(R.id.item, fillInIntent);

            byte[] friend_bg = mCursor.getBlob(14);
            setImageViewBitmap(views, R.id.friend_bg, friend_bg);

            views.setTextViewText(R.id.friend, mCursor.getString(1));
            views.setTextColor(R.id.friend, friend_color);
            views.setFloat(R.id.friend, "setTextSize", friend_textsize);
            views.setTextViewText(R.id.created, mCursor.getString(4));
            views.setTextColor(R.id.created, created_color);
            views.setFloat(R.id.created, "setTextSize", created_textsize);
            byte[] image_bg = mCursor.getBlob(15);

            if (setImageViewBitmap(views, R.id.image_clear, image_bg)) {
                byte[] image = mCursor.getBlob(16);
                setImageViewBitmap(views, R.id.image, image);
            }

            byte[] profile_bg = mCursor.getBlob(13);

            if (mDisplay_profile) {
                setImageViewBitmap(views, R.id.profile_bg, profile_bg);
            }

            byte[] profile = mCursor.getBlob(2);

            if (mDisplay_profile) {
                if (profile == null) {
                    profile = getBlob(mContext.getResources(), R.drawable.ic_contact_picture);
                }

                setImageViewBitmap(views, R.id.profile, profile);
            }
        } else
            views = null;

        return views;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDataSetChanged() {
        // Refresh the cursor, warning: the resulting cursor could be null
        if (mCursor != null) {
            mCursor.close();
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onDataSetChanged: " + mAppWidgetId);
        }

        mCursor = mContext.getContentResolver().query(Uri.withAppendedPath(Statuses_styles.getContentUri(mContext), Integer.toString(mAppWidgetId)), new String[]{Statuses_styles._ID, Statuses_styles.FRIEND, Statuses_styles.PROFILE, Statuses_styles.MESSAGE, Statuses_styles.CREATEDTEXT, Statuses_styles.MESSAGES_COLOR, Statuses_styles.FRIEND_COLOR, Statuses_styles.CREATED_COLOR, Statuses_styles.MESSAGES_TEXTSIZE, Statuses_styles.FRIEND_TEXTSIZE, Statuses_styles.CREATED_TEXTSIZE, Statuses_styles.STATUS_BG, Statuses_styles.ICON, Statuses_styles.PROFILE_BG, Statuses_styles.FRIEND_BG, Statuses_styles.IMAGE_BG, Statuses_styles.IMAGE}, null, null, Statuses_styles.CREATED + " DESC");
    }

    @Override
    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
        }
    }

}
