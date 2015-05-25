package com.piusvelte.sonet.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.piusvelte.sonet.provider.Notifications;

/**
 * Created by bemmanuel on 5/22/15.
 */
public class ClearNotificationsService extends IntentService {

    private static final String EXTRA_STATUS_ID = "status_id";

    public ClearNotificationsService() {
        super(ClearNotificationsService.class.getSimpleName());
    }

    public static Intent obtainIntent(@NonNull Context context) {
        return new Intent(context, ClearNotificationsService.class);
    }

    public static Intent obtainIntent(@NonNull Context context, long statusId) {
        return obtainIntent(context)
                .putExtra(EXTRA_STATUS_ID, statusId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ContentValues values = new ContentValues();
        values.put(Notifications.CLEARED, 1);

        if (intent.hasExtra(EXTRA_STATUS_ID)) {
            getContentResolver()
                    .update(Notifications.getContentUri(getApplicationContext()),
                            values,
                            Notifications._ID + "=?",
                            new String[] { Long.toString(intent.getLongExtra(EXTRA_STATUS_ID, -1)) });
        } else {
            getContentResolver()
                    .update(Notifications.getContentUri(getApplicationContext()),
                            values,
                            null,
                            null);
        }
    }
}
