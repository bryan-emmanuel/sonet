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
    private View mLoadingView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.loading_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mLoadingView = view.findViewById(R.id.loading);

        mAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.widget_item,
                null,
                new String[] { StatusesStyles.FRIEND,
                        StatusesStyles.MESSAGE,
                        StatusesStyles.CREATEDTEXT,
                        StatusesStyles.PROFILE,
                        StatusesStyles.ICON,
                        StatusesStyles.IMAGE },
                new int[] { R.id.friend,
                        R.id.message,
                        R.id.created,
                        R.id.profile,
                        R.id.icon,
                        R.id.image },
                0);
        mAdapter.setViewBinder(new WidgetsViewBinder());
        setListAdapter(mAdapter);

        mLoadingView.setVisibility(View.VISIBLE);
        getLoaderManager().initLoader(LOADER_WIDGETS, null, this);
    }

    @Override
    public void onDestroyView() {
        mLoadingView.setVisibility(View.GONE);
        super.onDestroyView();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_WIDGETS:
                return new CursorLoader(getActivity(),
                        StatusesStyles.getContentUri(getActivity()),
                        new String[] { StatusesStyles._ID,
                                StatusesStyles.FRIEND,
                                StatusesStyles.PROFILE,
                                StatusesStyles.MESSAGE,
                                StatusesStyles.CREATEDTEXT,
                                StatusesStyles.ICON,
                                StatusesStyles.IMAGE },
                        StatusesStyles.WIDGET + "=?",
                        new String[] { Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID) },
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
        startActivity(new Intent(getActivity(), StatusDialog.class)
                .setData(Uri.withAppendedPath(StatusesStyles.getContentUri(getActivity()), Long.toString(id)))
                .putExtra(LauncherIntent.Extra.Scroll.EXTRA_SOURCE_BOUNDS, r)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case LOADER_WIDGETS:
                mLoadingView.setVisibility(View.GONE);
                mAdapter.changeCursor(cursor);
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

        private static boolean setImageBitmap(View view, byte[] data) {
            if (data != null) {
                Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length, sBFOptions);

                if (bmp != null) {
                    ((ImageView) view).setImageBitmap(bmp);
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            if (columnIndex == cursor.getColumnIndex(StatusesStyles.PROFILE)) {
                setImageBitmap(view, cursor.getBlob(columnIndex));
                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.FRIEND)) {
                ((TextView) view).setText(cursor.getString(columnIndex));
                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.CREATEDTEXT)) {
                ((TextView) view).setText(cursor.getString(columnIndex));
                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.MESSAGE)) {
                ((TextView) view).setText(cursor.getString(columnIndex));
                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.ICON)) {
                setImageBitmap(view, cursor.getBlob(columnIndex));
                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.IMAGE)) {
                if (!setImageBitmap(view, cursor.getBlob(columnIndex))) {
                    view.setVisibility(View.GONE);
                }
                return true;
            } else {
                return false;
            }
        }
    }
}