package com.piusvelte.sonet.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import static com.piusvelte.sonet.Sonet.INVALID_ACCOUNT_ID;

/**
 * Created by bemmanuel on 4/14/15.
 */
public class ConfirmSetLocationDialogFragment {

    private static final String ARG_ID = "id";
    private static final String ARG_LATITUDE = "latitude";
    private static final String ARG_LONGITUDE = "longitude";

    public static ConfirmationDialogFragment newInstance(long accountId, String latitude, String longitude, @StringRes int title, int requestCode) {
        ConfirmationDialogFragment dialogFragment = ConfirmationDialogFragment.newInstance(title, requestCode);
        Bundle args = dialogFragment.getArguments();
        args.putLong(ARG_ID, accountId);
        args.putString(ARG_LATITUDE, latitude);
        args.putString(ARG_LONGITUDE, longitude);
        return dialogFragment;
    }

    public static long getAccountId(@NonNull Intent intent) {
        return intent.getLongExtra(ARG_ID, INVALID_ACCOUNT_ID);
    }

    public static String getLatitude(@NonNull Intent intent) {
        return intent.getStringExtra(ARG_LATITUDE);
    }

    public static String getLongitude(@NonNull Intent intent) {
        return intent.getStringExtra(ARG_LONGITUDE);
    }
}
