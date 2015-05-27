package com.piusvelte.sonet;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.piusvelte.sonet.provider.StatusLinks;
import com.piusvelte.sonet.provider.StatusesStyles;
import com.piusvelte.sonet.social.Client;
import com.piusvelte.sonet.util.CircleTransformation;
import com.squareup.picasso.Picasso;

@SuppressLint("NewApi")
public class SonetRemoteViewsFactory implements android.widget.RemoteViewsService.RemoteViewsFactory {
    private static final String TAG = "SonetRemoteViewsFactory";

    private static int sColumnIndexId;
    private static int sColumnIndexService;
    private static int sColumnIndexFriend;
    private static int sColumnIndexProfileUrl;
    private static int sColumnIndexMessage;
    private static int sColumnIndexCreatedText;
    private static int sColumnIndexImage;

    private Context mContext;
    private Cursor mCursor;
    private int mAppWidgetId;
    private Picasso mPicasso;
    private CircleTransformation mCircleTransformation;

    public SonetRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        mPicasso = Picasso.with(context);
        mCircleTransformation = new CircleTransformation();
    }

    @Override
    public int getCount() {
        if (mCursor != null) {
            return mCursor.getCount();
        } else {
            return 0;
        }
    }

    @Override
    public long getItemId(int position) {
        if ((mCursor != null) && !mCursor.isClosed() && mCursor.moveToPosition(position)) {
            return mCursor.getLong(mCursor.getColumnIndex(StatusesStyles._ID));
        }

        return position;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        // load the item
        final RemoteViews views;

        if (mCursor != null && !mCursor.isClosed() && mCursor.moveToPosition(position)) {
            views = new RemoteViews(mContext.getPackageName(), R.layout.widget_item);
            // set icons
            views.setImageViewResource(R.id.icon, Client.Network.get(mCursor.getInt(sColumnIndexService)).getIcon());

            views.setTextViewText(R.id.message, mCursor.getString(sColumnIndexMessage));

            // Set the click intent so that we can handle it and show a toast message
            final Intent fillInIntent = new Intent();
            final Bundle extras = new Bundle();
            extras.putString(StatusLinks.STATUS_ID, Long.toString(mCursor.getLong(sColumnIndexId)));
            fillInIntent.putExtras(extras);
            views.setOnClickFillInIntent(R.id.item, fillInIntent);

            views.setTextViewText(R.id.friend, mCursor.getString(sColumnIndexFriend));
            views.setTextViewText(R.id.created, mCursor.getString(sColumnIndexCreatedText));

            String imageUrl = mCursor.getString(sColumnIndexImage);

            if (!TextUtils.isEmpty(imageUrl)) {
                views.setViewVisibility(R.id.image, View.VISIBLE);
                mPicasso.load(imageUrl)
                        .into(views, R.id.image, new int[] { mAppWidgetId });
            } else {
                views.setViewVisibility(R.id.image, View.GONE);
            }

            String profileUrl = mCursor.getString(sColumnIndexProfileUrl);

            if (!TextUtils.isEmpty(profileUrl)) {
                mPicasso.load(profileUrl)
                        .transform(mCircleTransformation)
                        .error(R.drawable.ic_account_box_grey600_48dp)
                        .into(views, R.id.profile, new int[] { mAppWidgetId });
            } else {
                views.setImageViewBitmap(R.id.profile,
                        mCircleTransformation.transform(BitmapFactory.decodeResource(mContext.getResources(),
                                R.drawable.ic_account_box_grey600_48dp)));
            }
        } else {
            views = null;
        }

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

        // use INVALID_APPWIDGET_ID as accounts are no longer widget specific
        mCursor = mContext.getContentResolver().query(Uri.withAppendedPath(StatusesStyles.getContentUri(mContext),
                        Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID)),
                new String[] { StatusesStyles._ID,
                        StatusesStyles.SERVICE,
                        StatusesStyles.FRIEND,
                        StatusesStyles.PROFILE_URL,
                        StatusesStyles.MESSAGE,
                        StatusesStyles.CREATEDTEXT,
                        StatusesStyles.IMAGE_URL },
                null,
                null,
                StatusesStyles.CREATED + " DESC");

        if (mCursor != null) {
            sColumnIndexId = mCursor.getColumnIndexOrThrow(StatusesStyles._ID);
            sColumnIndexService = mCursor.getColumnIndexOrThrow(StatusesStyles.SERVICE);
            sColumnIndexFriend = mCursor.getColumnIndexOrThrow(StatusesStyles.FRIEND);
            sColumnIndexProfileUrl = mCursor.getColumnIndexOrThrow(StatusesStyles.PROFILE_URL);
            sColumnIndexMessage = mCursor.getColumnIndexOrThrow(StatusesStyles.MESSAGE);
            sColumnIndexCreatedText = mCursor.getColumnIndexOrThrow(StatusesStyles.CREATEDTEXT);
            sColumnIndexImage = mCursor.getColumnIndexOrThrow(StatusesStyles.IMAGE_URL);
        }
    }

    @Override
    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
        }
    }
}
