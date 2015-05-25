package com.piusvelte.sonet.loader;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.util.Log;

import com.piusvelte.sonet.BuildConfig;
import com.piusvelte.sonet.R;
import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.SonetCrypto;
import com.piusvelte.sonet.provider.StatusLinks;
import com.piusvelte.sonet.provider.StatusesStyles;

import static com.piusvelte.sonet.Sonet.IDENTICA;
import static com.piusvelte.sonet.Sonet.PINTEREST;
import static com.piusvelte.sonet.Sonet.RSS;
import static com.piusvelte.sonet.Sonet.SMS;
import static com.piusvelte.sonet.Sonet.TWITTER;

/**
 * Created by bemmanuel on 4/22/15.
 */
@Deprecated
public class StatusLoader extends BaseAsyncTaskLoader<StatusLoader.Result> {

    private static String TAG = StatusLoader.class.getSimpleName();

    private Context mContext;
    private Uri mData;

    public StatusLoader(Context context, @NonNull Uri data) {
        super(context);
        mContext = context.getApplicationContext();
        mData = data;
    }

    @Override
    public Result loadInBackground() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "get status: " + mData.getLastPathSegment());
        }

        Result result = new Result();
        result.data = mData;
        result.sonetCrypto = SonetCrypto.getInstance(mContext);

        Cursor c = mContext.getContentResolver().query(StatusesStyles.getContentUri(mContext),
                new String[] { StatusesStyles._ID,
                        StatusesStyles.WIDGET,
                        StatusesStyles.ACCOUNT,
                        StatusesStyles.ESID,
                        StatusesStyles.MESSAGE,
                        StatusesStyles.FRIEND,
                        StatusesStyles.SERVICE,
                        StatusesStyles.SID },
                StatusesStyles._ID + "=?",
                new String[] { mData.getLastPathSegment() },
                null);

        if (c.moveToFirst()) {
            result.appwidgetId = c.getInt(c.getColumnIndexOrThrow(StatusesStyles.WIDGET));
            result.accountId = c.getLong(c.getColumnIndexOrThrow(StatusesStyles.ACCOUNT));

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "account: " + result.accountId);
            }

            // informational messages go directly to settings, otherwise, load up the options
            if (result.accountId != Sonet.INVALID_ACCOUNT_ID) {
                result.service = c.getInt(c.getColumnIndexOrThrow(StatusesStyles.SERVICE));

                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "service: " + result.service);
                }

                if (result.service == PINTEREST) {
                    // pinterest uses the username for the profile page
                    result.esid = c.getString(c.getColumnIndexOrThrow(StatusesStyles.FRIEND));
                } else {
                    result.esid = result.sonetCrypto.Decrypt(c.getString(c.getColumnIndexOrThrow(StatusesStyles.ESID)));
                }

                result.sid = result.sonetCrypto.Decrypt(c.getString(c.getColumnIndexOrThrow(StatusesStyles.SID)));

                if (result.service == SMS) {
                    // lookup the contact, else null rect
                    Cursor phones = mContext.getContentResolver()
                            .query(Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(result.esid)),
                                    new String[] { ContactsContract.PhoneLookup.LOOKUP_KEY },
                                    null, null, null);

                    if (phones.moveToFirst()) {
                        result.esid = phones.getString(phones.getColumnIndexOrThrow(ContactsContract.PhoneLookup.LOOKUP_KEY));
                    }

                    phones.close();
                } else if (result.service != RSS) {
                    result.serviceName = Sonet.getServiceName(mContext.getResources(), result.service);
                    // get links from table
                    Cursor links = mContext.getContentResolver().query(StatusLinks.getContentUri(mContext),
                            new String[] { StatusLinks.LINK_URI,
                                    StatusLinks.LINK_TYPE },
                            StatusLinks.STATUS_ID + "=?",
                            new String[] { Long.toString(c.getLong(c.getColumnIndexOrThrow(StatusesStyles._ID))) },
                            null);
                    //						count += links.getCount();
                    int count = links.getCount();
                    result.items = new String[Result.PROFILE + count + 1];
                    result.itemsData = new String[result.items.length];
                    // for facebook wall posts, remove everything after the " > "
                    String friend = c.getString(c.getColumnIndexOrThrow(StatusesStyles.FRIEND));

                    if (friend != null && friend.indexOf(">") > 0) {
                        friend = friend.substring(0, friend.indexOf(">") - 1);
                    }

                    if (result.service == TWITTER) {
                        result.items[Result.COMMENT] = mContext.getString(R.string.reply) + " @" + friend;
                        result.items[Result.POST] = mContext.getString(R.string.tweet);
                    } else if (result.service == IDENTICA) {
                        result.items[Result.COMMENT] = mContext.getString(R.string.reply) + " @" + friend;
                        result.items[Result.POST] = String.format(mContext.getString(R.string.update_status), result.serviceName);
                    } else {
                        result.items[Result.COMMENT] = String.format(mContext.getString(R.string.comment_status), friend);
                        result.items[Result.POST] = String.format(mContext.getString(R.string.update_status), result.serviceName);
                    }

                    result.items[Result.NOTIFICATIONS] = mContext.getString(R.string.notifications);
                    result.items[Result.REFRESH] = mContext.getString(R.string.button_refresh);
                    result.items[Result.PROFILE] = String.format(mContext.getString(R.string.userProfile), friend);
                    count = Result.PROFILE + 1;

                    // links
                    if (links.moveToFirst()) {
                        while (!links.isAfterLast()) {
                            result.itemsData[count] = links.getString(0);
                            String host = Uri.parse(links.getString(0)).getHost();
                            String type = links.getString(1);

                            if (type.equals(Sonet.Spicture)) {
                                result.items[count] = String.format(mContext.getString(R.string.open_picture), host);
                            } else if (type.equals(Sonet.Sphoto)) {
                                result.items[count] = String.format(mContext.getString(R.string.open_page), host);
                            } else {
                                result.items[count] = String.format(mContext.getString(R.string.open_link), host);
                            }

                            count++;
                            links.moveToNext();
                        }
                    }

                    links.close();
                }
            }
        }

        c.close();
        return result;
    }

    public static class Result {
        public static final int COMMENT = 0;
        public static final int POST = 1;
        public static final int NOTIFICATIONS = 2;
        public static final int REFRESH = 3;
        public static final int PROFILE = 4;

        public Uri data;
        public SonetCrypto sonetCrypto;
        public int appwidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        public long accountId = Sonet.INVALID_ACCOUNT_ID;
        public int service;
        public String serviceName;
        public String esid;
        public String sid;
        public String[] items;
        public String[] itemsData;
    }
}
