package com.piusvelte.sonet.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by bemmanuel on 4/19/15.
 */
public class CommentOptionsDialogFragment {

    private static final String ARG_SID = "sid";
    private static final String ARG_DO_LIKE = "do_liked";

    public static ItemsDialogFragment newInstance(int requestCode, CharSequence[] items, @NonNull String sid, boolean doLiked) {
        ItemsDialogFragment dialogFragment = ItemsDialogFragment.newInstance(items, requestCode);
        Bundle args = dialogFragment.getArguments();
        args.putString(ARG_SID, sid);
        args.putBoolean(ARG_DO_LIKE, doLiked);
        return dialogFragment;
    }

    public static String getSid(@NonNull Intent intent) {
        return intent.getStringExtra(ARG_SID);
    }

    public static boolean getDoLiked(@Nullable Intent intent, boolean defaultValue) {
        if (intent == null) {
            return defaultValue;
        }

        return intent.getBooleanExtra(ARG_DO_LIKE, defaultValue);
    }
}
