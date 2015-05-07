package com.piusvelte.sonet.fragment;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.piusvelte.sonet.R;
import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.provider.Accounts;

import java.util.Arrays;

/**
 * Created by bemmanuel on 4/30/15.
 */
public class ChooseAccount extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String ARG_REQUEST_CODE = "request_code";
    private static final String ARG_FILTER_IDS = "filter_ids";

    private static final String ID = "id";

    private static final int LOADER_ACCOUNT_NAMES = 0;

    private View mLoadingView;

    private SimpleCursorAdapter mAdapter;

    public static ChooseAccount newInstance(int requestCode) {
        ChooseAccount fragment = new ChooseAccount();
        Bundle args = new Bundle();
        args.putInt(ARG_REQUEST_CODE, requestCode);
        fragment.setArguments(args);
        return fragment;
    }

    public static ChooseAccount newInstance(int requestCode, @NonNull long[] filterIds) {
        ChooseAccount fragment = new ChooseAccount();
        Bundle args = new Bundle();
        args.putInt(ARG_REQUEST_CODE, requestCode);
        args.putLongArray(ARG_FILTER_IDS, filterIds);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.loading_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mLoadingView = view.findViewById(R.id.loading);

        mAdapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_list_item_1,
                null,
                new String[] { Accounts.USERNAME },
                new int[] { android.R.id.text1 },
                0);

        mLoadingView.setVisibility(View.VISIBLE);

        Bundle loaderArgs;

        if (getArguments().containsKey(ARG_FILTER_IDS)) {
            loaderArgs = new Bundle();
            loaderArgs.putLongArray(ARG_FILTER_IDS, getArguments().getLongArray(ARG_FILTER_IDS));
        } else {
            loaderArgs = null;
        }

        getLoaderManager().initLoader(LOADER_ACCOUNT_NAMES, loaderArgs, this);
    }

    @Override
    public void onDestroyView() {
        mLoadingView = null;
        super.onDestroyView();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        int requestCode = getArguments().getInt(ARG_REQUEST_CODE);
        Intent intent = new Intent();
        intent.putExtra(ID, id);

        Fragment target = getTargetFragment();

        if (target != null) {
            target.onActivityResult(requestCode, Activity.RESULT_OK, intent);
        } else {
            Fragment parent = getParentFragment();

            if (parent != null) {
                parent.onActivityResult(requestCode, Activity.RESULT_OK, intent);
            } else {
                Activity activity = getActivity();

                if (activity instanceof BaseDialogFragment.OnResultListener) {
                    ((BaseDialogFragment.OnResultListener) activity).onResult(requestCode, Activity.RESULT_OK, intent);
                }
            }
        }
    }

    public static long getSelectedId(Intent intent) {
        return intent.getLongExtra(ID, Sonet.INVALID_ACCOUNT_ID);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ACCOUNT_NAMES:
                if (args != null && args.containsKey(ARG_FILTER_IDS)) {
                    return new CursorLoader(getActivity(),
                            Accounts.getContentUri(getActivity()),
                            new String[] { Accounts._ID, Accounts.SERVICE, Accounts.ACCOUNTS_QUERY },
                            Accounts._ID + " in (?)",
                            new String[] { TextUtils.join(",", Arrays.asList(args.getLongArray(ARG_FILTER_IDS))) },
                            null);
                } else {
                    return new CursorLoader(getActivity(),
                            Accounts.getContentUri(getActivity()),
                            new String[] { Accounts._ID, Accounts.SERVICE, Accounts.ACCOUNTS_QUERY },
                            null,
                            null,
                            null);
                }

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case LOADER_ACCOUNT_NAMES:
                mLoadingView.setVisibility(View.GONE);
                mAdapter.changeCursor(cursor);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_ACCOUNT_NAMES:
                mAdapter.changeCursor(null);
                break;
        }
    }
}
