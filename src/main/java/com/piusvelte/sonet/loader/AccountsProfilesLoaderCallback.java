package com.piusvelte.sonet.loader;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

import java.util.HashMap;
import java.util.List;

/**
 * Created by bemmanuel on 6/3/15.
 */
public class AccountsProfilesLoaderCallback implements LoaderManager.LoaderCallbacks<List<HashMap<String, String>>> {

    private final int mLoaderId;
    @NonNull
    private OnAccountsLoadedListener mListener;

    public AccountsProfilesLoaderCallback(@NonNull OnAccountsLoadedListener listener, int loaderId) {
        mListener = listener;
        mLoaderId = loaderId;
    }

    @Override
    public Loader<List<HashMap<String, String>>> onCreateLoader(int id, Bundle args) {
        if (id == mLoaderId) {
            return new AccountsProfilesLoader(mListener.getContext());
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<List<HashMap<String, String>>> loader, List<HashMap<String, String>> data) {
        if (loader.getId() == mLoaderId) {
            mListener.onAccountsLoaded(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<HashMap<String, String>>> loader) {
        if (loader.getId() == mLoaderId) {
            mListener.onAccountsLoaded(null);
        }
    }

    public interface OnAccountsLoadedListener {
        Context getContext();

        void onAccountsLoaded(List<HashMap<String, String>> accounts);
    }
}
