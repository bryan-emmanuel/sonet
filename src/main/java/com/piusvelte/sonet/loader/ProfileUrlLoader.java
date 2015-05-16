package com.piusvelte.sonet.loader;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.piusvelte.sonet.SonetCrypto;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.social.Client;

/**
 * Created by bemmanuel on 4/26/15.
 */
public class ProfileUrlLoader extends BaseAsyncTaskLoader {

    @NonNull
    private Context mContext;
    private long mAccountId;
    @NonNull
    private String mEsid;

    public ProfileUrlLoader(Context context, long accountId, @NonNull String esid) {
        super(context);
        mContext = context.getApplicationContext();
        mAccountId = accountId;
        mEsid = esid;
    }

    @Override
    public Object loadInBackground() {
        String url = null;
        Cursor cursor = mContext.getContentResolver().query(Accounts.getContentUri(mContext),
                new String[] { Accounts._ID,
                        Accounts.TOKEN,
                        Accounts.SECRET,
                        Accounts.SERVICE },
                Accounts._ID + "=?",
                new String[] { Long.toString(mAccountId) },
                null);

        if (cursor.moveToFirst()) {
            SonetCrypto sonetCrypto = SonetCrypto.getInstance(mContext);
            String token = sonetCrypto.Decrypt(cursor.getString(cursor.getColumnIndexOrThrow(Accounts.TOKEN)));
            String secret = sonetCrypto.Decrypt(cursor.getString(cursor.getColumnIndexOrThrow(Accounts.SECRET)));
            int service = cursor.getInt(cursor.getColumnIndexOrThrow(Accounts.SERVICE));

            Client client = new Client.Builder(mContext)
                    .setNetwork(service)
                    .setCredentials(token, secret)
                    .build();

            url = client.getProfileUrl(mEsid);
        }

        cursor.close();
        return url;
    }
}
