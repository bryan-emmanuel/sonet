package com.piusvelte.sonet.loader;

import android.content.Context;
import android.database.Cursor;

import com.piusvelte.sonet.SonetCrypto;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.social.Client;

/**
 * Created by bemmanuel on 3/25/15.
 */
public class FriendsLoader extends BaseAsyncTaskLoader {

    private Context mContext;
    private long mAccountId;

    public FriendsLoader(Context context, long accountId) {
        super(context);
        mContext = context.getApplicationContext();
        mAccountId = accountId;
    }

    @Override
    public Object loadInBackground() {
        Client client = null;
        SonetCrypto sonetCrypto = SonetCrypto.getInstance(mContext);
        // load the session
        Cursor account = mContext.getContentResolver().query(Accounts.getContentUri(mContext),
                new String[]{Accounts.TOKEN, Accounts.SECRET, Accounts.SERVICE}, Accounts._ID + "=?",
                new String[]{Long.toString(mAccountId)},
                null);

        if (account.moveToFirst()) {
            String token = sonetCrypto.Decrypt(account.getString(account.getColumnIndexOrThrow(Accounts.TOKEN)));
            String secret = sonetCrypto.Decrypt(account.getString(account.getColumnIndexOrThrow(Accounts.SECRET)));

            client = new Client.Builder(mContext)
                    .setNetwork(account.getInt(account.getColumnIndexOrThrow(Accounts.SERVICE)))
                    .setCredentials(token, secret)
                    .build();
        }

        account.close();

        if (client != null) {
            return client.getFriends();
        }

        return null;
    }
}
