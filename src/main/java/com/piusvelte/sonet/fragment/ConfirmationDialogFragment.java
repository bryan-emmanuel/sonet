package com.piusvelte.sonet.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

/**
 * Created by bemmanuel on 4/14/15.
 */
public class ConfirmationDialogFragment extends BaseDialogFragment implements DialogInterface.OnClickListener {

    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";

    public static ConfirmationDialogFragment newInstance(@StringRes int title, int requestCode) {
        ConfirmationDialogFragment dialogFragment = new ConfirmationDialogFragment();
        dialogFragment.setRequestCode(requestCode);

        Bundle args = dialogFragment.getArguments();
        args.putInt(ARG_TITLE, title);

        return dialogFragment;
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
        deliverResult(Activity.RESULT_OK);
        dismiss();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        deliverResult(Activity.RESULT_CANCELED);
    }
}
