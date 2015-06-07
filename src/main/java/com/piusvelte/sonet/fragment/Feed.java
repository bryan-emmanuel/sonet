package com.piusvelte.sonet.fragment;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.piusvelte.sonet.R;
import com.piusvelte.sonet.SonetComments;
import com.piusvelte.sonet.SonetCreatePost;
import com.piusvelte.sonet.SonetService;
import com.piusvelte.sonet.provider.StatusLinks;
import com.piusvelte.sonet.provider.StatusesStyles;
import com.piusvelte.sonet.social.Client;
import com.piusvelte.sonet.util.CircleTransformation;
import com.squareup.picasso.Picasso;

import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;

/**
 * Created by bemmanuel on 3/21/15.
 */
public class Feed extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener {

    private static final int LOADER_FEED = 0;

    private SimpleCursorAdapter mAdapter;
    private FloatingActionButton mFloatingActionButton;
    private View mLoadingView;

    public Feed() {
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.loading_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mFloatingActionButton = (FloatingActionButton) view.findViewById(R.id.fab);
        mFloatingActionButton.setOnClickListener(this);

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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_feed, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                Toast.makeText(getActivity(), R.string.refreshing, Toast.LENGTH_LONG).show();
                getActivity().startService(new Intent(getActivity(), SonetService.class)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                        .setAction(ACTION_REFRESH));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setFABVisibility();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isResumed() && !isRemoving()) {
            setFABVisibility();
        }
    }

    private void setFABVisibility() {
        if (getUserVisibleHint()) {
            if (mFloatingActionButton.getVisibility() != View.VISIBLE) {
                mFloatingActionButton.setTranslationY(getResources().getDimension(R.dimen.fab_animation_height));
                mFloatingActionButton.setVisibility(View.VISIBLE);
                mFloatingActionButton.animate()
                        .translationY(0)
                        .setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime))
                        .start();
            }
        } else if (mFloatingActionButton.getVisibility() == View.VISIBLE) {
            mFloatingActionButton.setVisibility(View.GONE);
        }
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
                .putExtra(StatusLinks.STATUS_ID, Long.toString(id)));
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

    @Override
    public void onClick(View v) {
        if (v == mFloatingActionButton) {
            startActivity(new Intent(getActivity(), SonetCreatePost.class));
        }
    }

    private static class WidgetsViewBinder implements SimpleCursorAdapter.ViewBinder {

        private Picasso mPicasso;
        private CircleTransformation mCircleTransformation;

        WidgetsViewBinder(@NonNull Context context) {
            mPicasso = Picasso.with(context);
            mCircleTransformation = new CircleTransformation();
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