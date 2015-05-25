package com.piusvelte.sonet.fragment;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;

/**
 * Created by bemmanuel on 4/11/15.
 */
@Deprecated
public class ChooseWidgetDialogFragment {

    private static final String ARG_IDS = "ids";

    public static ItemsDialogFragment newInstance(@NonNull HashMap<Integer, String> widgets, int requestCode) {
        CharSequence[] items = widgets.values().toArray(new String[widgets.size()]);

        int i = 0;
        int[] ids = new int[widgets.size()];

        for (Integer id : widgets.keySet()) {
            ids[i++] = id;
        }

        ItemsDialogFragment dialogFragment = ItemsDialogFragment.newInstance(items, requestCode);
        dialogFragment.getArguments().putIntArray(ARG_IDS, ids);
        return dialogFragment;
    }

    public static int getSelectedId(@Nullable Intent intent) {
        if (intent != null) {
            int which = ItemsDialogFragment.getWhich(intent, -1);
            int[] ids = intent.getIntArrayExtra(ARG_IDS);

            if (which >= 0 && ids != null && ids.length > which) {
                return ids[which];
            }
        }

        return AppWidgetManager.INVALID_APPWIDGET_ID;
    }
}
