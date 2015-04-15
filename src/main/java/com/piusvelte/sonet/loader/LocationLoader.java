package com.piusvelte.sonet.loader;

import android.content.Context;
import android.database.Cursor;

import com.piusvelte.sonet.SonetCrypto;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.social.Client;

import java.util.HashMap;

/**
 * Created by bemmanuel on 3/27/15.
 */
public class LocationLoader extends BaseAsyncTaskLoader {

    private Context mContext;
    private long mAccountId;
    private String mLatitude;
    private String mLongitude;

    public LocationLoader(Context context, long accountId, String latitude, String longitude) {
        super(context);
        mContext = context.getApplicationContext();
        mAccountId = accountId;
        mLatitude = latitude;
        mLongitude = longitude;
    }

    @Override
    public Object loadInBackground() {
        LocationResult result = null;
        Cursor account = mContext.getContentResolver().query(Accounts.getContentUri(mContext),
                new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SERVICE, Accounts.SECRET},
                Accounts._ID + "=?",
                new String[]{Long.toString(mAccountId)}, null);

        if (account.moveToFirst()) {
            SonetCrypto sonetCrypto = SonetCrypto.getInstance(mContext);
            int serviceId = account.getInt(account.getColumnIndex(Accounts.SERVICE));
            String token = sonetCrypto.Decrypt(account.getString(account.getColumnIndexOrThrow(Accounts.TOKEN)));
            String secret = sonetCrypto.Decrypt(account.getString(account.getColumnIndexOrThrow(Accounts.SECRET)));
            Client client = new Client.Builder(mContext)
                    .setNetwork(serviceId)
                    .setCredentials(token, secret)
                    .build();

            HashMap<String, String> locations = client.getLocations(mLatitude, mLongitude);

            if (locations != null && !locations.isEmpty()) {
                result = new LocationResult();
                result.accountId = mAccountId;
                result.locations = locations;
            }
        }

        account.close();
        return result;
    }

    public static class LocationResult {
        public long accountId;
        public HashMap<String, String> locations;
    }
}
