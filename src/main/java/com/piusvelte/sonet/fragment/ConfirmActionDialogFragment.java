package com.piusvelte.sonet.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

/**
 * Created by bemmanuel on 4/14/15.
 */
public class ConfirmActionDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    private static final String ARG_REQUEST_CODE = "request_code";
    private static final String ARG_TITLE = "title";

    public static ConfirmActionDialogFragment newInstance(@NonNull String title, int requestCode) {
        Bundle args = new Bundle();
        args.putInt(ARG_REQUEST_CODE, requestCode);
        args.putString(ARG_TITLE, title);

        ConfirmActionDialogFragment confirmActionDialogFragment = new ConfirmActionDialogFragment();
        confirmActionDialogFragment.setArguments(args);
        return confirmActionDialogFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(getArguments().getString(ARG_TITLE))
                .setPositiveButton(android.R.string.ok, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Bundle args = getArguments();
        int requestCode = args.getInt(ARG_REQUEST_CODE);
        Intent intent = new Intent();
        intent.putExtras(args);
        Fragment target = getTargetFragment();

        if (target != null) {
            target.onActivityResult(requestCode, Activity.RESULT_OK, intent);
        } else {
            Fragment parent = getParentFragment();

            if (parent != null) {
                parent.onActivityResult(requestCode, Activity.RESULT_OK, intent);
            } else {
                Activity activity = getActivity();

                if (activity instanceof OnDialogFragmentFinishListener) {
                    ((OnDialogFragmentFinishListener) activity).onDialogFragmentResult(requestCode, Activity.RESULT_OK, intent);
                }
            }
        }

        dismiss();
    }
}
