package com.piusvelte.sonet.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;

import static com.piusvelte.sonet.Sonet.INVALID_ACCOUNT_ID;

/**
 * Created by bemmanuel on 4/14/15.
 */
public class ChooseLocationDialogFragment {

    private static final String ARG_ACCOUNT = "account";
    private static final String ARG_IDS = "ids";

    public static SingleChoiceDialogFragment newInstance(long account,
            @NonNull HashMap<String, String> locations,
            @NonNull String title,
            int requestCode) {
        CharSequence[] items = locations.values().toArray(new String[locations.size()]);
        String[] ids = locations.keySet().toArray(new String[locations.size()]);
        SingleChoiceDialogFragment singleChoiceDialogFragment = SingleChoiceDialogFragment.newInstance(items, -1, title, requestCode);
        Bundle args = singleChoiceDialogFragment.getArguments();
        args.putLong(ARG_ACCOUNT, account);
        args.putStringArray(ARG_IDS, ids);
        return singleChoiceDialogFragment;
    }

    public static long getAccount(@Nullable Intent intent) {
        if (intent != null) {
            return intent.getLongExtra(ARG_ACCOUNT, INVALID_ACCOUNT_ID);
        }

        return INVALID_ACCOUNT_ID;
    }

    public static String getSelectedId(@Nullable Intent intent, String defaultValue) {
        if (intent != null) {
            int which = SingleChoiceDialogFragment.getWhich(intent, -1);
            String[] ids = intent.getStringArrayExtra(ARG_IDS);

            if (which >= 0 && ids != null && ids.length > which) {
                return ids[which];
            }
        }

        return defaultValue;
    }
}
