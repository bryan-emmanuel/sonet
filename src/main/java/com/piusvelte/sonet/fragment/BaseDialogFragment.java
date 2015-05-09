package com.piusvelte.sonet.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

/**
 * Created by bemmanuel on 4/18/15.
 */
public abstract class BaseDialogFragment extends DialogFragment {

    private static final String ARG_REQUEST_CODE = "request_code";

    protected void setRequestCode(int requestCode) {
        Bundle args = new Bundle();
        args.putInt(ARG_REQUEST_CODE, requestCode);
        setArguments(args);
    }

    protected void deliverResult(int resultCode) {
        Bundle args = getArguments();
        int requestCode = args.getInt(ARG_REQUEST_CODE);

        Intent intent = new Intent()
                .putExtras(args);
        Fragment target = getTargetFragment();

        if (target != null) {
            target.onActivityResult(requestCode, resultCode, intent);
        } else {
            Fragment parent = getParentFragment();

            if (parent != null) {
                parent.onActivityResult(requestCode, resultCode, intent);
            } else {
                Activity activity = getActivity();

                if (activity instanceof OnResultListener) {
                    ((OnResultListener) activity).onResult(requestCode, resultCode, intent);
                }
            }
        }
    }

    public interface OnResultListener {

        void onResult(int requestCode, int resultCode, Intent data);
    }
}
