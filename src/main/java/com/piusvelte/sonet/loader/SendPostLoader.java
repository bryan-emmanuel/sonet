package com.piusvelte.sonet.loader;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.piusvelte.sonet.SonetCrypto;
import com.piusvelte.sonet.fragment.ChoosePostAccounts;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.social.Client;

import java.util.ArrayList;

/**
 * Created by bemmanuel on 3/29/15.
 */
public class SendPostLoader extends BaseAsyncTaskLoader<Boolean> {

    @NonNull
    private Context mContext;
    @NonNull
    private ArrayList<ChoosePostAccounts.Account> mAccounts;
    @NonNull
    private String mMessage;
    @Nullable
    private String mPhoto;

    public SendPostLoader(@NonNull Context context,
            @NonNull ArrayList<ChoosePostAccounts.Account> accounts,
            @NonNull String message,
            @Nullable String photo) {
        super(context);
        mContext = context.getApplicationContext();
        mAccounts = accounts;
        mMessage = message;
        mPhoto = photo;
    }

    @Override
    public Boolean loadInBackground() {
        Boolean success = null;

        for (ChoosePostAccounts.Account account : mAccounts) {
            SonetCrypto sonetCrypto = SonetCrypto.getInstance(mContext);
            // post or comment!
            Cursor cursor = mContext.getContentResolver().query(Accounts.getContentUri(mContext),
                    new String[] { Accounts._ID, Accounts.TOKEN, Accounts.SECRET },
                    Accounts._ID + "=?",
                    new String[] { Long.toString(account.id) },
                    null);

            if (cursor.moveToFirst()) {
                String token = sonetCrypto.Decrypt(cursor.getString(cursor.getColumnIndex(Accounts.TOKEN)));
                String secret = sonetCrypto.Decrypt(cursor.getString(cursor.getColumnIndex(Accounts.SECRET)));
                Client client = new Client.Builder(mContext)
                        .setNetwork(account.service)
                        .setCredentials(token, secret)
                        .build();
                String[] tags;

                if (account.tags != null) {
                    tags = account.tags.toArray(new String[account.tags.size()]);
                } else {
                    tags = null;
                }

                success = client.createPost(mMessage, account.location, account.latitude, account.longitude, mPhoto, tags);
            }

            cursor.close();
        }

        return success;
    }
}
