package com.piusvelte.sonet.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by bemmanuel on 4/11/15.
 */
public class SingleChoiceDialogFragment extends BaseDialogFragment implements DialogInterface.OnClickListener {

    private static final String ARG_ITEMS = "items";
    private static final String ARG_WHICH = "which";
    private static final String ARG_TITLE = "title";

    public static SingleChoiceDialogFragment newInstance(@NonNull CharSequence[] items, int which, int requestCode) {
        SingleChoiceDialogFragment dialogFragment = new SingleChoiceDialogFragment();
        dialogFragment.setRequestCode(requestCode);

        Bundle args = dialogFragment.getArguments();
        args.putCharSequenceArray(ARG_ITEMS, items);
        args.putInt(ARG_WHICH, which);
        return dialogFragment;
    }

    public static SingleChoiceDialogFragment newInstance(@NonNull CharSequence[] items, int which, @NonNull String title, int requestCode) {
        SingleChoiceDialogFragment chooseAccountDialogFragment = newInstance(items, which, requestCode);
        chooseAccountDialogFragment.getArguments().putString(ARG_TITLE, title);
        return chooseAccountDialogFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setSingleChoiceItems(args.getCharSequenceArray(ARG_ITEMS), args.getInt(ARG_WHICH), this);

        if (args.containsKey(ARG_TITLE)) {
            builder.setTitle(args.getString(ARG_TITLE));
        }

        return builder.create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        deliverResult(Activity.RESULT_CANCELED);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Bundle args = getArguments();
        args.putInt(ARG_WHICH, which);
        deliverResult(Activity.RESULT_OK);
        dismiss();
    }

    public static int getWhich(@Nullable Intent intent, int defaultValue) {
        if (intent == null) {
            return defaultValue;
        }

        return intent.getIntExtra(ARG_WHICH, defaultValue);
    }
}
