package com.piusvelte.sonet.fragment;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;

import static com.piusvelte.sonet.Sonet.INVALID_ACCOUNT_ID;

/**
 * Created by bemmanuel on 4/11/15.
 */
public class ChooseAccountDialogFragment {

    private static final String ARG_IDS = "ids";

    public static SingleChoiceDialogFragment newInstance(@NonNull HashMap<Long, String> accounts, @NonNull String title, int requestCode) {
        CharSequence[] items = accounts.values().toArray(new String[accounts.size()]);

        int i = 0;
        long[] ids = new long[accounts.size()];

        for (Long id : accounts.keySet()) {
            ids[i++] = id;
        }

        SingleChoiceDialogFragment singleChoiceDialogFragment = SingleChoiceDialogFragment.newInstance(items, -1, title, requestCode);
        singleChoiceDialogFragment.getArguments().putLongArray(ARG_IDS, ids);
        return singleChoiceDialogFragment;
    }

    public static long getSelectedId(@Nullable Intent intent) {
        if (intent != null) {
            int which = SingleChoiceDialogFragment.getWhich(intent, -1);
            long[] ids = intent.getLongArrayExtra(ARG_IDS);

            if (which >= 0 && ids != null && ids.length > which) {
                return ids[which];
            }
        }

        return INVALID_ACCOUNT_ID;
    }
}
