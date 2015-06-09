package com.piusvelte.sonet.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.piusvelte.sonet.R;
import com.piusvelte.sonet.loader.AddRssLoader;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by bemmanuel on 4/19/15.
 */
public class RssNameDialogFragment extends BaseDialogFragment implements LoaderManager.LoaderCallbacks<String> {

    private static final String ARG_URL = "url";
    private static final String ARG_NAME = "name";

    private static final int LOADER_RSS = 0;

    @StringDef({ ARG_URL, ARG_NAME })
    @Retention(RetentionPolicy.SOURCE)
    private @interface Argument {
    }

    private EditText mName;
    private View mLoadingView;

    public static RssNameDialogFragment newInstance(int requestCode, @NonNull String url) {
        RssNameDialogFragment dialogFragment = new RssNameDialogFragment();
        dialogFragment.setRequestCode(requestCode);

        Bundle args = dialogFragment.getArguments();
        args.putString(ARG_URL, url);

        return dialogFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.rss_channel);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.rss_name, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mName = (EditText) view.findViewById(R.id.rss_name);
        mLoadingView = view.findViewById(R.id.loading);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mLoadingView.setVisibility(View.VISIBLE);
        getLoaderManager().initLoader(LOADER_RSS, getArguments(), this);
    }

    @Override
    public void onDestroyView() {
        mName = null;
        mLoadingView = null;
        super.onDestroyView();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        String name = mName.getText().toString();
        int resultCode = TextUtils.isEmpty(name) ? Activity.RESULT_CANCELED : Activity.RESULT_OK;
        getArguments().putString(ARG_NAME, name);
        deliverResult(resultCode);
    }

    @Override
    public Loader<String> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_RSS:
                return new AddRssLoader(getActivity(), args.getString(ARG_URL));

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<String> loader, String data) {
        switch (loader.getId()) {
            case LOADER_RSS:
                mLoadingView.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<String> loader) {
        // NO-OP
    }

    private static String getArg(@Nullable Intent intent, @Nullable String defaultValue, @Argument String key) {
        if (intent == null) {
            return defaultValue;
        }

        return intent.getStringExtra(key);
    }

    @Nullable
    public static String getUrl(@Nullable Intent intent, @Nullable String defaultValue) {
        return getArg(intent, defaultValue, ARG_URL);
    }

    @Nullable
    public static String getName(@Nullable Intent intent, @Nullable String defaultValue) {
        return getArg(intent, defaultValue, ARG_NAME);
    }
}
