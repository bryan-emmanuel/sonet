package com.piusvelte.sonet.loader;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;

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

    public AccountsProfilesLoader(Context context) {
        super(context);
        mContext = context.getApplicationContext();
    }

    @Override
    public List<HashMap<String, String>> loadInBackground() {
        List<HashMap<String, String>> accounts = new ArrayList<>();

        Cursor cursor = mContext.getContentResolver().query(Accounts.getContentUri(mContext),
                new String[] { Accounts._ID,
                        Accounts.TOKEN,
                        Accounts.SECRET,
                        Accounts.SERVICE,
                        Accounts.USERNAME,
                        Accounts.SID },
                null,
                null,
                null);

        if (cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndexOrThrow(Accounts._ID);
            int tokenIndex = cursor.getColumnIndexOrThrow(Accounts.TOKEN);
            int secretIndex = cursor.getColumnIndexOrThrow(Accounts.SECRET);
            int serviceIndex = cursor.getColumnIndexOrThrow(Accounts.SERVICE);
            int sidIndex = cursor.getColumnIndexOrThrow(Accounts.SID);
            int usernameIndex = cursor.getColumnIndexOrThrow(Accounts.USERNAME);

            while (!cursor.isAfterLast()) {
                HashMap<String, String> account = new HashMap<>(4);
                SonetCrypto sonetCrypto = SonetCrypto.getInstance(mContext);
                String token = sonetCrypto.Decrypt(cursor.getString(tokenIndex));
                String secret = sonetCrypto.Decrypt(cursor.getString(secretIndex));
                int service = cursor.getInt(serviceIndex);
                String sid = sonetCrypto.Decrypt(cursor.getString(sidIndex));

                Client client = new Client.Builder(mContext)
                        .setNetwork(service)
                        .setCredentials(token, secret)
                        .build();

                account.put(Accounts._ID, Long.toString(cursor.getLong(idIndex)));
                account.put(Accounts.SERVICE, Integer.toString(service));
                account.put(Entity.PROFILE_URL, client.getProfilePhotoUrl(sid));
                account.put(Accounts.USERNAME, cursor.getString(usernameIndex));
                accounts.add(account);

                cursor.moveToNext();
            }
        }

        cursor.close();
        return accounts;
    }
}
