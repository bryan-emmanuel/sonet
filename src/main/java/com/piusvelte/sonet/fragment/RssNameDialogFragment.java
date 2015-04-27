package com.piusvelte.sonet.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.text.TextUtils;
import android.widget.EditText;

import com.piusvelte.sonet.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by bemmanuel on 4/19/15.
 */
public class RssNameDialogFragment extends BaseDialogFragment {

    private static final String ARG_URL = "url";
    private static final String ARG_NAME = "name";

    @StringDef({ ARG_URL, ARG_NAME })
    @Retention(RetentionPolicy.SOURCE)
    private @interface Argument {
    }

    private EditText mName;

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
        mName = new EditText(getActivity());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.rss_channel)
                .setView(mName);

        return builder.create();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mName = null;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        String name = mName.getText().toString();
        int resultCode = TextUtils.isEmpty(name) ? Activity.RESULT_CANCELED : Activity.RESULT_OK;
        getArguments().putString(ARG_NAME, name);
        deliverResult(resultCode);
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
