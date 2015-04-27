package com.piusvelte.sonet.loader;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.piusvelte.sonet.SonetCrypto;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.social.Client;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bemmanuel on 3/29/15.
 */
public class SendPostLoader extends BaseAsyncTaskLoader {

    @NonNull
    private Context mContext;
    @NonNull
    private HashMap<Long, Integer> mAccounts;
    @NonNull
    private String mMessage;
    @NonNull
    private HashMap<Long, String> mLocations;
    @Nullable
    private String mLatitude;
    @Nullable
    private String mLongitude;
    @Nullable
    private String mPhoto;
    @NonNull
    private HashMap<Long, String[]> mTags;

    public SendPostLoader(@NonNull Context context,
            @NonNull HashMap<Long, Integer> accounts,
            @NonNull String message,
            @NonNull HashMap<Long, String> locations,
            @Nullable String latitude,
            @Nullable String longitude,
            @Nullable String photo,
            @NonNull HashMap<Long, String[]> tags) {
        super(context);
        mContext = context.getApplicationContext();
        mAccounts = accounts;
        mMessage = message;
        mLocations = locations;
        mLatitude = latitude;
        mLongitude = longitude;
        mPhoto = photo;
        mTags = tags;
    }

    @Override
    public Object loadInBackground() {
        Boolean success = null;

        for (Map.Entry<Long, Integer> account : mAccounts.entrySet()) {
            final long accountId = account.getKey();
            final int service = account.getValue();
            SonetCrypto sonetCrypto = SonetCrypto.getInstance(mContext);
            // post or comment!
            Cursor cursor = mContext.getContentResolver().query(Accounts.getContentUri(mContext),
                    new String[] { Accounts._ID, Accounts.TOKEN, Accounts.SECRET },
                    Accounts._ID + "=?",
                    new String[] { Long.toString(accountId) },
                    null);

            if (cursor.moveToFirst()) {
//                final String serviceName = Sonet.getServiceName(getResources(), service);
//                publishProgress(serviceName);
                String token = sonetCrypto.Decrypt(cursor.getString(cursor.getColumnIndex(Accounts.TOKEN)));
                String secret = sonetCrypto.Decrypt(cursor.getString(cursor.getColumnIndex(Accounts.SECRET)));
                Client client = new Client.Builder(mContext)
                        .setNetwork(service)
                        .setCredentials(token, secret)
                        .build();
                success = client.createPost(mMessage, mLocations.get(accountId), mLatitude, mLongitude, mPhoto, mTags.get(accountId));
//                publishProgress(serviceName, getString(success ? R.string.success : R.string.failure));
            }

            cursor.close();
        }

        return success;
    }
}
