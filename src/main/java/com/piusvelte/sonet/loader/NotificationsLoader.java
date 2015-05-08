package com.piusvelte.sonet.loader;

import android.content.Context;
import android.database.Cursor;

import com.piusvelte.sonet.SonetCrypto;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.provider.Notifications;
import com.piusvelte.sonet.provider.Widgets;
import com.piusvelte.sonet.provider.WidgetsSettings;
import com.piusvelte.sonet.social.Client;

/**
 * Created by bemmanuel on 5/7/15.
 */
public class NotificationsLoader extends BaseAsyncTaskLoader {

    private Context mContext;

    public NotificationsLoader(Context context) {
        super(context);
        mContext = context.getApplicationContext();
    }

    @Override
    public Object loadInBackground() {
        // select all accounts with notifications set
        Cursor widgets = mContext.getContentResolver().query(WidgetsSettings.getDistinctContentUri(mContext),
                new String[] { Widgets.ACCOUNT },
                Widgets.ACCOUNT + "!=-1 and (" + Widgets.LIGHTS + "=1 or " + Widgets.VIBRATE + "=1 or " + Widgets.SOUND + "=1)",
                null,
                null);

        if (widgets.moveToFirst()) {
            SonetCrypto sonetCrypto = SonetCrypto.getInstance(mContext);

            while (!widgets.isAfterLast()) {
                long accountId = widgets.getLong(0);
                Cursor account = mContext.getContentResolver().query(Accounts.getContentUri(mContext),
                        new String[] { Accounts.TOKEN,
                                Accounts.SECRET,
                                Accounts.SERVICE,
                                Accounts.SID },
                        Accounts._ID + "=?",
                        new String[] { Long.toString(accountId) },
                        null);

                if (account.moveToFirst()) {
                    // for each account, for each notification, check for updates
                    // if there are no updates past 24hrs and cleared, delete
                    String token = sonetCrypto.Decrypt(account.getString(account.getColumnIndexOrThrow(Accounts.TOKEN)));
                    String secret = sonetCrypto.Decrypt(account.getString(account.getColumnIndexOrThrow(Accounts.SECRET)));
                    int service = account.getInt(account.getColumnIndexOrThrow(Accounts.SERVICE));
                    String accountEsid = sonetCrypto.Decrypt(account.getString(account.getColumnIndexOrThrow(Accounts.SID)));

                    Client client = new Client.Builder(mContext)
                            .setNetwork(service)
                            .setCredentials(token, secret)
                            .setAccount(accountEsid)
                            .build();

                    client.getNotifications(accountId, new String[1]);

                    // remove old notifications
                    mContext.getContentResolver().delete(Notifications.getContentUri(mContext),
                            Notifications.CLEARED + "=1 and " + Notifications.ACCOUNT + "=? and " + Notifications.CREATED + "<?",
                            new String[] { Long.toString(accountId),
                                    Long.toString(System.currentTimeMillis() - 86400000) });
                }

                account.close();
                widgets.moveToNext();
            }
        }

        widgets.close();
        return null;
    }
}
