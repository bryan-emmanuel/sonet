package com.piusvelte.sonet.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.EditText;

import com.piusvelte.sonet.R;

/**
 * Created by bemmanuel on 4/19/15.
 */
public class RssUrlDialogFragment extends BaseDialogFragment {

    private static final String ARG_URL = "url";

    private EditText mUrl;

    public static RssUrlDialogFragment newInstance(int requestCode) {
        RssUrlDialogFragment dialogFragment = new RssUrlDialogFragment();
        dialogFragment.setRequestCode(requestCode);
        return dialogFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mUrl = new EditText(getActivity());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.rss_url)
                .setView(mUrl);

        return builder.create();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUrl = null;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        String url = mUrl.getText().toString();
        int resultCode = TextUtils.isEmpty(url) ? Activity.RESULT_CANCELED : Activity.RESULT_OK;
        getArguments().putString(ARG_URL, url);
        deliverResult(resultCode);
    }

    @Nullable
    public static String getUrl(@Nullable Intent intent, @Nullable String defaultValue) {
        if (intent == null) {
            return defaultValue;
        }

        return intent.getStringExtra(ARG_URL);
    }
}
