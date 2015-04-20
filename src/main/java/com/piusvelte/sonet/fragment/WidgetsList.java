package com.piusvelte.sonet.fragment;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.piusvelte.sonet.R;
import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.SonetService;
import com.piusvelte.sonet.StatusDialog;
import com.piusvelte.sonet.provider.StatusesStyles;

import mobi.intuitit.android.content.LauncherIntent;

import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;
import static com.piusvelte.sonet.Sonet.sBFOptions;


/**
 * Created by bemmanuel on 3/21/15.
 */
public class WidgetsList extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_WIDGETS = 0;

    private SimpleCursorAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.widgets, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.widget_item,
                null,
                new String[]{StatusesStyles.FRIEND,
                        StatusesStyles.FRIEND + "2",
                        StatusesStyles.MESSAGE,
                        StatusesStyles.MESSAGE + "2",
                        StatusesStyles.STATUS_BG,
                        StatusesStyles.CREATEDTEXT,
                        StatusesStyles.PROFILE,
                        StatusesStyles.ICON,
                        StatusesStyles.PROFILE_BG,
                        StatusesStyles.FRIEND_BG,
                        StatusesStyles.IMAGE_BG,
                        StatusesStyles.IMAGE},
                new int[]{R.id.friend_bg_clear,
                        R.id.friend,
                        R.id.message_bg_clear,
                        R.id.message,
                        R.id.status_bg,
                        R.id.created,
                        R.id.profile,
                        R.id.icon,
                        R.id.profile_bg,
                        R.id.friend_bg,
                        R.id.image_clear,
                        R.id.image},
                0);
        mAdapter.setViewBinder(new WidgetsViewBinder());
        setListAdapter(mAdapter);
        getLoaderManager().initLoader(LOADER_WIDGETS, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_WIDGETS:
                return new CursorLoader(getActivity(),
                        StatusesStyles.getContentUri(getActivity()),
                        new String[]{StatusesStyles._ID,
                                StatusesStyles.FRIEND,
                                StatusesStyles.FRIEND + " as " + StatusesStyles.FRIEND + "2",
                                StatusesStyles.PROFILE,
                                StatusesStyles.MESSAGE,
                                StatusesStyles.MESSAGE + " as " + StatusesStyles.MESSAGE + "2",
                                StatusesStyles.CREATEDTEXT,
                                StatusesStyles.MESSAGES_COLOR,
                                StatusesStyles.FRIEND_COLOR,
                                StatusesStyles.CREATED_COLOR,
                                StatusesStyles.MESSAGES_TEXTSIZE,
                                StatusesStyles.FRIEND_TEXTSIZE,
                                StatusesStyles.CREATED_TEXTSIZE,
                                StatusesStyles.STATUS_BG,
                                StatusesStyles.ICON,
                                StatusesStyles.PROFILE_BG,
                                StatusesStyles.FRIEND_BG,
                                StatusesStyles.IMAGE_BG,
                                StatusesStyles.IMAGE},
                        StatusesStyles.WIDGET + "=?",
                        new String[]{Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID)},
                        StatusesStyles.CREATED + " desc");

            default:
                return null;
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Rect r = new Rect();
        v.getHitRect(r);
        startActivity(Sonet.getPackageIntent(getActivity(), StatusDialog.class)
                .setData(Uri.withAppendedPath(StatusesStyles.getContentUri(getActivity()), Long.toString(id)))
                .putExtra(LauncherIntent.Extra.Scroll.EXTRA_SOURCE_BOUNDS, r)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case LOADER_WIDGETS:
                mAdapter.changeCursor(cursor);
                // if no statuses, trigger a refresh
                if (cursor.getCount() == 0 && isAdded() && isResumed()) {
                    getActivity().startService(Sonet.getPackageIntent(getActivity(),
                            SonetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                            AppWidgetManager.INVALID_APPWIDGET_ID)
                            .setAction(ACTION_REFRESH));
                }
                break;
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_WIDGETS:
                mAdapter.changeCursor(null);
                break;
        }
    }

    private static class WidgetsViewBinder implements SimpleCursorAdapter.ViewBinder {

        private static void setImageBitmap(View view, byte[] data) {
            Bitmap bmp;

            if (data != null) {
                bmp = BitmapFactory.decodeByteArray(data, 0, data.length, sBFOptions);

                if (bmp != null) {
                    ((ImageView) view).setImageBitmap(bmp);
                }
            }
        }

        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            if (columnIndex == cursor.getColumnIndex(StatusesStyles.FRIEND)) {
                ((TextView) view).setText(cursor.getString(columnIndex));
                ((TextView) view).setTextSize(cursor.getLong(cursor.getColumnIndex(StatusesStyles.FRIEND_TEXTSIZE)));
                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.MESSAGE)) {
                ((TextView) view).setText(cursor.getString(columnIndex));
                ((TextView) view).setTextSize(cursor.getLong(cursor.getColumnIndex(StatusesStyles.MESSAGES_TEXTSIZE)));
                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.STATUS_BG)) {
                setImageBitmap(view, cursor.getBlob(columnIndex));
                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.PROFILE)) {
                setImageBitmap(view, cursor.getBlob(columnIndex));
                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.FRIEND + "2")) {
                ((TextView) view).setText(cursor.getString(columnIndex));
                ((TextView) view).setTextSize(cursor.getLong(cursor.getColumnIndex(StatusesStyles.FRIEND_TEXTSIZE)));
                ((TextView) view).setTextColor(cursor.getInt(cursor.getColumnIndex(StatusesStyles.FRIEND_COLOR)));
                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.CREATEDTEXT)) {
                ((TextView) view).setText(cursor.getString(columnIndex));
                ((TextView) view).setTextSize(cursor.getLong(cursor.getColumnIndex(StatusesStyles.CREATED_TEXTSIZE)));
                ((TextView) view).setTextColor(cursor.getInt(cursor.getColumnIndex(StatusesStyles.CREATED_COLOR)));
                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.MESSAGE + "2")) {
                ((TextView) view).setText(cursor.getString(columnIndex));
                ((TextView) view).setTextSize(cursor.getLong(cursor.getColumnIndex(StatusesStyles.MESSAGES_TEXTSIZE)));
                ((TextView) view).setTextColor(cursor.getInt(cursor.getColumnIndex(StatusesStyles.MESSAGES_COLOR)));
                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.ICON)) {
                setImageBitmap(view, cursor.getBlob(columnIndex));
                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.PROFILE_BG)) {
                setImageBitmap(view, cursor.getBlob(columnIndex));
                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.FRIEND_BG)) {
                setImageBitmap(view, cursor.getBlob(columnIndex));
                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.IMAGE_BG)) {
                setImageBitmap(view, cursor.getBlob(columnIndex));
                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.IMAGE)) {
                setImageBitmap(view, cursor.getBlob(columnIndex));
                return true;
            } else {
                return false;
            }
        }
    }
}