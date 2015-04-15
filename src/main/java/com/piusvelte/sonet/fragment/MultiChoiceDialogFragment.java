package com.piusvelte.sonet.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

/**
 * Created by bemmanuel on 4/11/15.
 */
public class MultiChoiceDialogFragment extends DialogFragment implements DialogInterface.OnMultiChoiceClickListener {

    private static final String ARG_REQUEST_CODE = "request_code";
    private static final String ARG_ITEMS = "items";
    private static final String ARG_WHICH = "which";
    private static final String ARG_TITLE = "title";

    public static MultiChoiceDialogFragment newInstance(@NonNull CharSequence[] items, @NonNull boolean[] which, @NonNull String title, int requestCode) {
        Bundle args = new Bundle();
        args.putInt(ARG_REQUEST_CODE, requestCode);
        args.putCharSequenceArray(ARG_ITEMS, items);
        args.putBooleanArray(ARG_WHICH, which);
        args.putString(ARG_TITLE, title);

        MultiChoiceDialogFragment chooseAccountDialogFragment = new MultiChoiceDialogFragment();
        chooseAccountDialogFragment.setArguments(args);
        return chooseAccountDialogFragment;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        return new AlertDialog.Builder(getActivity())
                .setTitle(args.getString(ARG_TITLE))
                .setMultiChoiceItems(args.getCharSequenceArray(ARG_ITEMS), args.getBooleanArray(ARG_WHICH), this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
        Bundle args = getArguments();

        boolean[] argsWhich = args.getBooleanArray(ARG_WHICH);

        if (which >= 0 && argsWhich != null && argsWhich.length < which) {
            argsWhich[which] = isChecked;
            args.putBooleanArray(ARG_WHICH, argsWhich);
        }

        Fragment fragment = getTargetFragment();

        if (fragment instanceof OnMultiChoiceClickListener) {
            ((OnMultiChoiceClickListener) fragment).onClick(this, getArguments().getInt(ARG_REQUEST_CODE), which, isChecked);
        } else {
            fragment = getParentFragment();

            if (fragment instanceof OnMultiChoiceClickListener) {
                ((OnMultiChoiceClickListener) fragment).onClick(this, getArguments().getInt(ARG_REQUEST_CODE), which, isChecked);
            } else {
                Activity activity = getActivity();

                if (activity instanceof OnMultiChoiceClickListener) {
                    ((OnMultiChoiceClickListener) activity).onClick(this, getArguments().getInt(ARG_REQUEST_CODE), which, isChecked);
                }
            }
        }
    }

    public interface OnMultiChoiceClickListener {

        void onClick(MultiChoiceDialogFragment multiChoiceDialogFragment, int requestCode, int which, boolean isChecked);

    }
}
