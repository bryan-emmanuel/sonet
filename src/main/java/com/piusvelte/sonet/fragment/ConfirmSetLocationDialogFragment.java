package com.piusvelte.sonet.fragment;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import static com.piusvelte.sonet.Sonet.INVALID_ACCOUNT_ID;

/**
 * Created by bemmanuel on 4/14/15.
 */
public class ConfirmSetLocationDialogFragment {

    private static final String ARG_ID = "id";

    public static ConfirmationDialogFragment newInstance(long accountId, @StringRes int title, int requestCode) {
        ConfirmationDialogFragment dialogFragment = ConfirmationDialogFragment.newInstance(title, requestCode);
        dialogFragment.getArguments().putLong(ARG_ID, accountId);
        return dialogFragment;
    }

    public static long getAccountId(@Nullable Intent intent) {
        if (intent != null) {
            return intent.getLongExtra(ARG_ID, INVALID_ACCOUNT_ID);
        }

        return INVALID_ACCOUNT_ID;
    }
}
