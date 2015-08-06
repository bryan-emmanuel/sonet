package com.piusvelte.sonet.loader;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;

import com.piusvelte.sonet.SonetCrypto;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.provider.Entity;
import com.piusvelte.sonet.social.Client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by bemmanuel on 4/26/15.
 */
public class AccountsProfilesLoader extends BaseAsyncTaskLoader<List<HashMap<String, String>>> {

    @NonNull
    private Context mContext;
    @Nullable
    private Cursor mCursor;
    @NonNull
    private ContentObserver mContentObserver;

    public AccountsProfilesLoader(Context context) {
        super(context);
        mContext = context.getApplicationContext();
        mContentObserver = new ForceLoadObserver(this);
    }

    @Override
    public List<HashMap<String, String>> loadInBackground() {
        if (mCursor != null) {
            mCursor.close();
        }

        List<HashMap<String, String>> accounts = new ArrayList<>();

        mCursor = mContext.getContentResolver().query(Accounts.getContentUri(mContext),
                new String[] { Accounts._ID,
                        Accounts.TOKEN,
                        Accounts.SECRET,
                        Accounts.SERVICE,
                        Accounts.USERNAME,
                        Accounts.SID },
                null,
                null,
                null);
        mCursor.registerContentObserver(mContentObserver);

        if (mCursor.moveToFirst()) {
            int idIndex = mCursor.getColumnIndexOrThrow(Accounts._ID);
            int tokenIndex = mCursor.getColumnIndexOrThrow(Accounts.TOKEN);
            int secretIndex = mCursor.getColumnIndexOrThrow(Accounts.SECRET);
            int serviceIndex = mCursor.getColumnIndexOrThrow(Accounts.SERVICE);
            int sidIndex = mCursor.getColumnIndexOrThrow(Accounts.SID);
            int usernameIndex = mCursor.getColumnIndexOrThrow(Accounts.USERNAME);

            while (!mCursor.isAfterLast()) {
                HashMap<String, String> account = new HashMap<>(4);
                SonetCrypto sonetCrypto = SonetCrypto.getInstance(mContext);
                String token = sonetCrypto.Decrypt(mCursor.getString(tokenIndex));
                String secret = sonetCrypto.Decrypt(mCursor.getString(secretIndex));
                int service = mCursor.getInt(serviceIndex);
                String sid = sonetCrypto.Decrypt(mCursor.getString(sidIndex));

                Client client = new Client.Builder(mContext)
                        .setNetwork(service)
                        .setCredentials(token, secret)
                        .setAccount(sid)
                        .build();

                account.put(Accounts._ID, Long.toString(mCursor.getLong(idIndex)));
                account.put(Accounts.SERVICE, Integer.toString(service));
                account.put(Entity.PROFILE_URL, client.getProfilePhotoUrl());
                account.put(Accounts.USERNAME, mCursor.getString(usernameIndex));
                account.put(Accounts.SID, sid);
                accounts.add(account);

                mCursor.moveToNext();
            }
        }

        return accounts;
    }

    @Override
    public void onCanceled(List<HashMap<String, String>> data) {
        super.onCanceled(data);

        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }

        mCursor = null;
    }

    @Override
    protected void onReset() {
        super.onReset();

        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }

        mCursor = null;
    }

    private static class ForceLoadObserver extends ContentObserver {

        @NonNull
        private Loader mLoader;

        ForceLoadObserver(@NonNull Loader loader) {
            super(new Handler());
            mLoader = loader;
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            mLoader.onContentChanged();
        }
    }
}
