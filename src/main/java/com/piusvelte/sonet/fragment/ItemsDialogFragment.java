package com.piusvelte.sonet.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

/**
 * Created by bemmanuel on 4/11/15.
 */
public class ItemsDialogFragment extends BaseDialogFragment implements DialogInterface.OnClickListener {

    private static final String ARG_REQUEST_CODE = "request_code";
    private static final String ARG_ITEMS = "items";
    private static final String ARG_WHICH = "which";
    /** Resource id for title [int] */
    private static final String ARG_TITLE = "title";

    public static ItemsDialogFragment newInstance(@NonNull CharSequence[] items, int requestCode) {
        ItemsDialogFragment chooseAccountDialogFragment = new ItemsDialogFragment();
        chooseAccountDialogFragment.setRequestCode(requestCode);
        Bundle args = chooseAccountDialogFragment.getArguments();
        args.putInt(ARG_REQUEST_CODE, requestCode);
        args.putCharSequenceArray(ARG_ITEMS, items);
        return chooseAccountDialogFragment;
    }

    public static ItemsDialogFragment newInstance(@NonNull CharSequence[] items, int requestCode, @StringRes int title) {
        ItemsDialogFragment fragment = newInstance(items, requestCode);
        Bundle args = fragment.getArguments();
        args.putInt(ARG_TITLE, title);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setItems(args.getCharSequenceArray(ARG_ITEMS), this);

        if (args.containsKey(ARG_TITLE)) {
            builder.setTitle(args.getInt(ARG_TITLE));
        }

        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (!getArguments().containsKey(ARG_WHICH)) {
            deliverResult(Activity.RESULT_CANCELED);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Bundle args = getArguments();
        args.putInt(ARG_WHICH, which);
        deliverResult(Activity.RESULT_OK);
        dismiss();
    }

    public static String[] getItems(@Nullable Intent intent) {
        if (intent == null) {
            return null;
        }

        return (String[]) intent.getCharSequenceArrayExtra(ARG_ITEMS);
    }

    public static int getWhich(@Nullable Intent intent, int defaultValue) {
        if (intent == null) {
            return defaultValue;
        }

        return intent.getIntExtra(ARG_WHICH, defaultValue);
    }
}
