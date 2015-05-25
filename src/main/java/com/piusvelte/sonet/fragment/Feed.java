package com.piusvelte.sonet.fragment;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.piusvelte.sonet.R;
import com.piusvelte.sonet.SonetComments;
import com.piusvelte.sonet.provider.StatusLinks;
import com.piusvelte.sonet.provider.StatusesStyles;
import com.piusvelte.sonet.social.Client;
import com.piusvelte.sonet.util.CircleTransformation;
import com.squareup.picasso.Picasso;

import static com.piusvelte.sonet.Sonet.sBFOptions;

/**
 * Created by bemmanuel on 3/21/15.
 */
public class Feed extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_FEED = 0;

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
                        StatusesStyles.PROFILE_URL,
                        StatusesStyles.SERVICE,
                        StatusesStyles.IMAGE_URL },
                new int[] { R.id.friend,
                        R.id.message,
                        R.id.created,
                        R.id.profile,
                        R.id.icon,
                        R.id.image },
                0);
        mAdapter.setViewBinder(new WidgetsViewBinder(getActivity()));
        setListAdapter(mAdapter);

        mLoadingView.setVisibility(View.VISIBLE);
        getLoaderManager().initLoader(LOADER_FEED, null, this);
    }

    @Override
    public void onDestroyView() {
        mLoadingView.setVisibility(View.GONE);
        super.onDestroyView();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_FEED:
                return new CursorLoader(getActivity(),
                        StatusesStyles.getContentUri(getActivity()),
                        new String[] { StatusesStyles._ID,
                                StatusesStyles.FRIEND,
                                StatusesStyles.PROFILE_URL,
                                StatusesStyles.MESSAGE,
                                StatusesStyles.CREATEDTEXT,
                                StatusesStyles.SERVICE,
                                StatusesStyles.IMAGE_URL },
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
        startActivity(new Intent(getActivity(), SonetComments.class)
                .putExtra(StatusLinks.STATUS_ID, Long.toString(id))
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case LOADER_FEED:
                mLoadingView.setVisibility(View.GONE);
                mAdapter.changeCursor(cursor);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_FEED:
                mAdapter.changeCursor(null);
                break;
        }
    }

    private static class WidgetsViewBinder implements SimpleCursorAdapter.ViewBinder {

        private Picasso mPicasso;
        private CircleTransformation mCircleTransformation;

        WidgetsViewBinder(@NonNull Context context) {
            mPicasso = Picasso.with(context);
            mCircleTransformation = new CircleTransformation();
        }

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
            if (columnIndex == cursor.getColumnIndex(StatusesStyles.PROFILE_URL)) {
                String profileUrl = cursor.getString(columnIndex);

                if (TextUtils.isEmpty(profileUrl)) {
                    mPicasso.load(R.drawable.ic_account_box_grey600_48dp)
                            .transform(mCircleTransformation)
                            .into((ImageView) view);
                } else {
                    mPicasso.load(cursor.getString(columnIndex))
                            .transform(mCircleTransformation)
                            .into((ImageView) view);
                }
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
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.SERVICE)) {
                ((ImageView) view).setImageResource(Client.Network.get(cursor.getInt(columnIndex)).getIcon());
                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.IMAGE_URL)) {
                String imageUrl = cursor.getString(columnIndex);

                if (!TextUtils.isEmpty(imageUrl)) {
                    view.setVisibility(View.VISIBLE);
                    mPicasso.load(imageUrl).into((ImageView) view);
                } else {
                    view.setVisibility(View.GONE);
                }

                return true;
            } else {
                return false;
            }
        }
    }
}