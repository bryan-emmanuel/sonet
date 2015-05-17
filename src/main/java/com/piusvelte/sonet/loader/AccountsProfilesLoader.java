package com.piusvelte.sonet.loader;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.SonetCrypto;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.social.Client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by bemmanuel on 4/26/15.
 */
public class AccountsProfilesLoader extends BaseAsyncTaskLoader {

    public static final String PROFILE = "profile";
    public static final String ICON = "icon";

    @NonNull
    private Context mContext;

    public AccountsProfilesLoader(Context context) {
        super(context);
        mContext = context.getApplicationContext();
    }

    @Override
    public Object loadInBackground() {
        List<HashMap<String, String>> accounts = new ArrayList<>();

        Cursor cursor = mContext.getContentResolver().query(Accounts.getContentUri(mContext),
                new String[] { Accounts._ID,
                        Accounts.TOKEN,
                        Accounts.SECRET,
                        Accounts.SERVICE,
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
                account.put(PROFILE, client.getProfilePhotoUrl(sid));
                account.put(ICON, Integer.toString(Client.Network.get(service).getIcon()));
                accounts.add(account);

                cursor.moveToNext();
            }
        }

        cursor.close();

        // always allow adding an account
        HashMap<String, String> account = new HashMap<>(4);
        account.put(Accounts._ID, Long.toString(Sonet.INVALID_ACCOUNT_ID));
        account.put(Accounts.SERVICE, null);
        account.put(PROFILE, null);
        account.put(ICON, null);
        accounts.add(account);

        return accounts;
    }
}
