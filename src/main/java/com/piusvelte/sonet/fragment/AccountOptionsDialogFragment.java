package com.piusvelte.sonet.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * Created by bemmanuel on 4/19/15.
 */
@Deprecated
public class AccountOptionsDialogFragment {

    private static final String ARG_ACCOUNT_ID = "account_id";
    private static final String ARG_IS_ENABLED = "is_enabled";

    public static ItemsDialogFragment newInstance(int requestCode, long accountId, CharSequence[] items, boolean isEnabled) {
        ItemsDialogFragment dialogFragment = ItemsDialogFragment.newInstance(items, requestCode);
        Bundle args = dialogFragment.getArguments();
        args.putLong(ARG_ACCOUNT_ID, accountId);
        args.putBoolean(ARG_IS_ENABLED, isEnabled);
        return dialogFragment;
    }

    public static long getAccountId(@Nullable Intent intent, long defaultValue) {
        if (intent == null) {
            return defaultValue;
        }

        return intent.getLongExtra(ARG_ACCOUNT_ID, defaultValue);
    }

    public static boolean getIsEnabled(@Nullable Intent intent, boolean defaultValue) {
        if (intent == null) {
            return defaultValue;
        }

        return intent.getBooleanExtra(ARG_IS_ENABLED, defaultValue);
    }
}
