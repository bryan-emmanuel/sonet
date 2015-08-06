package com.piusvelte.sonet.loader;

import android.content.ContentValues;
import android.content.Context;

import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.provider.WidgetAccounts;

/**
 * Created by bemmanuel on 7/31/15.
 */
public class AddAccountLoader extends BaseAsyncTaskLoader<Boolean> {

    private Context mContext;
    private String mUsername;
    private String mToken;
    private String mSecret;
    private int mExpiry;
    private int mService;
    private String mSid;
    private long mAccountId;
    private int mAppWidgetId;

    public AddAccountLoader(Context context,
            String username,
            String token,
            String secret,
            int expiry,
            int service,
            String sid,
            long accountId,
            int appWidgetId) {
        super(context);

        mContext = context.getApplicationContext();
        mUsername = username;
        mToken = token;
        mSecret = secret;
        mExpiry = expiry;
        mService = service;
        mSid = sid;
        mAccountId = accountId;
        mAppWidgetId = appWidgetId;
    }

    @Override
    public Boolean loadInBackground() {
        ContentValues values = new ContentValues();
        values.put(Accounts.USERNAME, mUsername);
        values.put(Accounts.TOKEN, mToken);
        values.put(Accounts.SECRET, mSecret);
        values.put(Accounts.EXPIRY, mExpiry);
        values.put(Accounts.SERVICE, mService);
        values.put(Accounts.SID, mSid);

        if (mAccountId != Sonet.INVALID_ACCOUNT_ID) {
            // re-authenticating
            mContext.getContentResolver()
                    .update(Accounts.getContentUri(mContext), values, Accounts._ID + "=?", new String[] { Long.toString(mAccountId) });
        } else {
            // new account
            mAccountId = Long.parseLong(mContext.getContentResolver().insert(Accounts.getContentUri(mContext), values).getLastPathSegment());
            values.clear();
            values.put(WidgetAccounts.ACCOUNT, mAccountId);
            values.put(WidgetAccounts.WIDGET, mAppWidgetId);
            mContext.getContentResolver().insert(WidgetAccounts.getContentUri(mContext), values);
        }

        return true;
    }
}
