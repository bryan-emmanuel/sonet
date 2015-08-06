package com.piusvelte.sonet.loader;

import android.content.Context;
import android.database.Cursor;

import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.provider.Notifications;
import com.piusvelte.sonet.provider.StatusImages;
import com.piusvelte.sonet.provider.StatusLinks;
import com.piusvelte.sonet.provider.Statuses;
import com.piusvelte.sonet.provider.WidgetAccounts;
import com.piusvelte.sonet.provider.Widgets;
import com.piusvelte.sonet.social.Client;

/**
 * Created by bemmanuel on 7/31/15.
 */
public class DeleteAccountLoader extends BaseAsyncTaskLoader<Boolean> {

    private Context mContext;
    private long mAccountId;
    private int mService;
    private String mEntityId;

    public DeleteAccountLoader(Context context, long accountId, int service, String entityId) {
        super(context);

        mContext = context.getApplicationContext();
        mAccountId = accountId;
        mService = service;
        mEntityId = entityId;
    }

    @Override
    public Boolean loadInBackground() {
        String[] queryArgs = new String[] { String.valueOf(mAccountId) };
        mContext.getContentResolver().delete(Accounts.getContentUri(mContext),
                Accounts._ID + "=?",
                queryArgs);
        // need to delete the statuses and settings for all accounts
        mContext.getContentResolver().delete(Widgets.getContentUri(mContext),
                Widgets.ACCOUNT + "=?",
                queryArgs);
        Cursor statuses = mContext.getContentResolver().query(Statuses.getContentUri(mContext),
                new String[] { Statuses._ID },
                Statuses.ACCOUNT + "=?",
                queryArgs,
                null);

        if (statuses.moveToFirst()) {
            int statusIdIndex = statuses.getColumnIndexOrThrow(Statuses._ID);

            while (!statuses.isAfterLast()) {
                long statusId = statuses.getLong(statusIdIndex);
                String[] statusQueryArgs = new String[] { String.valueOf(statusId) };

                mContext.getContentResolver().delete(StatusLinks.getContentUri(mContext),
                        StatusLinks.STATUS_ID + "=?",
                        statusQueryArgs);
                mContext.getContentResolver().delete(StatusImages.getContentUri(mContext),
                        StatusImages.STATUS_ID + "=?",
                        statusQueryArgs);
                statuses.moveToNext();
            }
        }

        statuses.close();

        mContext.getContentResolver().delete(Statuses.getContentUri(mContext),
                Statuses.ACCOUNT + "=?",
                queryArgs);
        mContext.getContentResolver().delete(WidgetAccounts.getContentUri(mContext),
                WidgetAccounts.ACCOUNT + "=?",
                queryArgs);
        mContext.getContentResolver().delete(Notifications.getContentUri(mContext),
                Notifications.ACCOUNT + "=?",
                queryArgs);

        Client client = new Client.Builder(mContext)
                .setNetwork(mService)
                .setAccount(mEntityId)
                .build();
        client.onDelete();

        return true;
    }
}
