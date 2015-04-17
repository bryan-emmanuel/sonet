package com.piusvelte.sonet.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

/**
 * Created by bemmanuel on 4/14/15.
 */
public class ConfirmationDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    private static final String ARG_REQUEST_CODE = "request_code";
    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";

    public static ConfirmationDialogFragment newInstance(@StringRes int title, int requestCode) {
        Bundle args = new Bundle();
        args.putInt(ARG_REQUEST_CODE, requestCode);
        args.putInt(ARG_TITLE, title);

        ConfirmationDialogFragment confirmationDialogFragment = new ConfirmationDialogFragment();
        confirmationDialogFragment.setArguments(args);
        return confirmationDialogFragment;
    }

    public static ConfirmationDialogFragment newInstance(@StringRes int title, @StringRes int message, int requestCode) {
        ConfirmationDialogFragment confirmationDialogFragment = newInstance(title, requestCode);
        confirmationDialogFragment.getArguments().putInt(ARG_MESSAGE, message);
        return confirmationDialogFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(args.getInt(ARG_TITLE))
                .setPositiveButton(android.R.string.ok, this)
                .setOnCancelListener(this);

        if (args.containsKey(ARG_MESSAGE)) {
            builder.setMessage(args.getInt(ARG_MESSAGE));
        }

        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        sendResult(Activity.RESULT_OK);
        dismiss();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        sendResult(Activity.RESULT_CANCELED);
    }

    private void sendResult(int result) {
        Bundle args = getArguments();
        int requestCode = args.getInt(ARG_REQUEST_CODE);
        Intent intent = new Intent();
        intent.putExtras(args);
        Fragment target = getTargetFragment();

        if (target != null) {
            target.onActivityResult(requestCode, result, intent);
        } else {
            Fragment parent = getParentFragment();

            if (parent != null) {
                parent.onActivityResult(requestCode, result, intent);
            } else {
                Activity activity = getActivity();

                if (activity instanceof OnDialogFragmentFinishListener) {
                    ((OnDialogFragmentFinishListener) activity).onDialogFragmentResult(requestCode, result, intent);
                }
            }
        }
    }
}
