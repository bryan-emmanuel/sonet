package com.piusvelte.sonet.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.piusvelte.sonet.R;
import com.piusvelte.sonet.SonetComments;
import com.piusvelte.sonet.provider.Notifications;
import com.piusvelte.sonet.service.ClearNotificationsService;
import com.piusvelte.sonet.service.LoadNotificationsService;

import static com.piusvelte.sonet.Sonet.RESULT_REFRESH;

/**
 * Created by bemmanuel on 3/21/15.
 */
public class NotificationsList extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_NOTIFICATIONS = 0;

    private static final int CLEAR = 1;

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
                R.layout.notifications_row,
                null,
                new String[] { Notifications.CLEARED,
                        Notifications.NOTIFICATION },
                new int[] { R.id.notification,
                        R.id.notification },
                0);
        mAdapter.setViewBinder(new NotificationsViewBinder());

        registerForContextMenu(getListView());
        setListAdapter(mAdapter);

        mLoadingView.setVisibility(View.VISIBLE);
        getLoaderManager().initLoader(LOADER_NOTIFICATIONS, null, this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onListItemClick(ListView list, final View view, int position, final long id) {
        super.onListItemClick(list, view, position, id);
        // load SonetComments.java, the notification will be clear there
        startActivityForResult(
                new Intent(getActivity(), SonetComments.class)
                        .setData(Uri.withAppendedPath(Notifications.getContentUri(getActivity()), Long.toString(id))),
                RESULT_REFRESH);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        // create clearing option
        menu.add(0, CLEAR, 0, R.string.clear);
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case CLEAR:
                getActivity().startService(ClearNotificationsService.obtainIntent(getActivity(),
                        ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id));
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_notifications, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_notifications_refresh:
                getActivity().startService(new Intent(getActivity(), LoadNotificationsService.class));
                return true;

            case R.id.menu_notifications_clear_all:
                getActivity().startService(ClearNotificationsService.obtainIntent(getActivity()));
                return true;

            default:
                return super.onOptionsItemSelected(item);
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
            case LOADER_NOTIFICATIONS:
                return new CursorLoader(getActivity(),
                        Notifications.getContentUri(getActivity()),
                        new String[] { Notifications._ID,
                                Notifications.CLEARED,
                                Notifications.NOTIFICATION },
                        Notifications.CLEARED + "!=1",
                        null,
                        null);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case LOADER_NOTIFICATIONS:
                mLoadingView.setVisibility(View.GONE);
                mAdapter.changeCursor(data);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_NOTIFICATIONS:
                mAdapter.changeCursor(null);
                break;
        }
    }

    private static class NotificationsViewBinder implements SimpleCursorAdapter.ViewBinder {

        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            if (columnIndex == cursor.getColumnIndex(Notifications.CLEARED)) {
                view.setEnabled(cursor.getInt(columnIndex) != 1);
                return true;
            } else {
                return false;
            }
        }
    }
}