package com.piusvelte.sonet.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.piusvelte.sonet.R;

/**
 * Created by bemmanuel on 4/7/15.
 */
public class LoadingDialogFragment extends BaseDialogFragment {

    private static final String ARG_REQUEST_CODE = "request_code";

    public static LoadingDialogFragment newInstance(int requestCode) {
        LoadingDialogFragment loadingDialogFragment = new LoadingDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_REQUEST_CODE, requestCode);
        loadingDialogFragment.setArguments(args);
        return loadingDialogFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog loadingDialog = new ProgressDialog(getActivity());
        loadingDialog.setMessage(getString(R.string.loading));
        return loadingDialog;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        deliverResult(Activity.RESULT_CANCELED);
    }
}
