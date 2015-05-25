package com.piusvelte.sonet.loader;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.telephony.SmsMessage;

import com.piusvelte.sonet.R;
import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.SonetCrypto;
import com.piusvelte.sonet.SonetNotifications;
import com.piusvelte.sonet.SonetService;
import com.piusvelte.sonet.provider.Entity;
import com.piusvelte.sonet.provider.Statuses;
import com.piusvelte.sonet.provider.WidgetAccountsView;
import com.piusvelte.sonet.provider.Widgets;
import com.piusvelte.sonet.provider.WidgetsSettings;

import static com.piusvelte.sonet.Sonet.NOTIFY_ID;
import static com.piusvelte.sonet.Sonet.SMS;
import static com.piusvelte.sonet.Sonet.initAccountSettings;

/**
 * Created by bemmanuel on 5/18/15.
 */
public class SMSLoader extends AsyncTask<SmsMessage, String, int[]> {

    @NonNull
    private SonetService mSonetService;

    public SMSLoader(@NonNull SonetService sonetService) {
        mSonetService = sonetService;
    }

    @Override
    protected int[] doInBackground(SmsMessage... msg) {
        // check if SMS is enabled anywhere
        Cursor widgets = mSonetService.getContentResolver().query(WidgetAccountsView.getContentUri(mSonetService),
                new String[] { WidgetAccountsView._ID,
                        WidgetAccountsView.WIDGET,
                        WidgetAccountsView.ACCOUNT },
                WidgetAccountsView.SERVICE + "=?",
                new String[] { Integer.toString(SMS) },
                null);
        int[] appWidgetIds = new int[widgets.getCount()];

        if (widgets.moveToFirst()) {
            // insert this message to the statuses db and requery scrollable/rebuild widget
            // check if this is a contact
            String phone = msg[0].getOriginatingAddress();
            String friend = phone;
            String profile = null;
            Uri content_uri = null;
            // unknown numbers crash here in the emulator
            Cursor phones = mSonetService.getContentResolver()
                    .query(Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone)),
                            new String[] { ContactsContract.PhoneLookup._ID },
                            null,
                            null,
                            null);

            if (phones.moveToFirst()) {
                content_uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI,
                        phones.getLong(phones.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID)));
            } else {
                Cursor emails = mSonetService.getContentResolver()
                        .query(Uri.withAppendedPath(ContactsContract.CommonDataKinds.Email.CONTENT_FILTER_URI, Uri.encode(phone)),
                                new String[] { ContactsContract.CommonDataKinds.Email._ID },
                                null,
                                null,
                                null);

                if (emails.moveToFirst()) {
                    content_uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI,
                            emails.getLong(emails.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email._ID)));
                }

                emails.close();
            }

            phones.close();

            if (content_uri != null) {
                // load contact
                Cursor contacts = mSonetService.getContentResolver()
                        .query(content_uri,
                                new String[] { ContactsContract.Contacts.DISPLAY_NAME },
                                null,
                                null,
                                null);

                if (contacts.moveToFirst()) {
                    friend = contacts.getString(contacts.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
                }

                contacts.close();
                profile = content_uri.toString();
            }

            long accountId = widgets.getLong(widgets.getColumnIndexOrThrow(WidgetAccountsView.ACCOUNT));
            long id;
            ContentValues values = new ContentValues();
            values.put(Entity.ESID, phone);
            values.put(Entity.FRIEND, friend);
            values.put(Entity.PROFILE_URL, profile);
            values.put(Entity.ACCOUNT, accountId);
            Cursor entity = mSonetService.getContentResolver().query(Entity.getContentUri(mSonetService),
                    new String[] { Entity._ID },
                    Entity.ACCOUNT + "=? and " + Entity.ESID + "=?",
                    new String[] { Long.toString(accountId),
                            SonetCrypto.getInstance(mSonetService).Encrypt(phone) },
                    null);

            if (entity.moveToFirst()) {
                id = entity.getLong(0);
                mSonetService.getContentResolver().update(Entity.getContentUri(mSonetService),
                        values,
                        Entity._ID + "=?",
                        new String[] { Long.toString(id) });
            } else {
                id = Long.parseLong(
                        mSonetService.getContentResolver().insert(Entity.getContentUri(mSonetService),
                                values).getLastPathSegment());
            }

            entity.close();
            values.clear();
            Long created = msg[0].getTimestampMillis();
            values.put(Statuses.CREATED, created);
            values.put(Statuses.ENTITY, id);
            values.put(Statuses.MESSAGE, msg[0].getMessageBody());
            values.put(Statuses.SERVICE, SMS);

            final int widgetIndex = widgets.getColumnIndexOrThrow(WidgetAccountsView.WIDGET);

            while (!widgets.isAfterLast()) {
                int widget = widgets.getInt(widgetIndex);
                appWidgetIds[widgets.getPosition()] = widget;
                // get settings
                boolean time24hr = true;
                int status_count = Sonet.default_statuses_per_account;
                int notifications = 0;

                Cursor c = mSonetService.getContentResolver().query(WidgetsSettings.getContentUri(mSonetService),
                        new String[] { Widgets.TIME24HR,
                                Widgets.SOUND,
                                Widgets.VIBRATE,
                                Widgets.LIGHTS },
                        Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?",
                        new String[] { Integer.toString(widget),
                                Long.toString(accountId) },
                        null);

                if (!c.moveToFirst()) {
                    c.close();
                    c = mSonetService.getContentResolver().query(WidgetsSettings.getContentUri(mSonetService),
                            new String[] { Widgets.TIME24HR,
                                    Widgets.SOUND,
                                    Widgets.VIBRATE,
                                    Widgets.LIGHTS },
                            Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?",
                            new String[] { Integer.toString(widget),
                                    Long.toString(Sonet.INVALID_ACCOUNT_ID) },
                            null);

                    if (!c.moveToFirst()) {
                        c.close();
                        c = mSonetService.getContentResolver().query(WidgetsSettings.getContentUri(mSonetService),
                                new String[] { Widgets.TIME24HR,
                                        Widgets.SOUND,
                                        Widgets.VIBRATE,
                                        Widgets.LIGHTS },
                                Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?",
                                new String[] { Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID),
                                        Long.toString(Sonet.INVALID_ACCOUNT_ID) },
                                null);

                        if (!c.moveToFirst()) {
                            initAccountSettings(mSonetService, AppWidgetManager.INVALID_APPWIDGET_ID,
                                    Sonet.INVALID_ACCOUNT_ID);
                        }

                        if (widget != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            initAccountSettings(mSonetService, widget, Sonet.INVALID_ACCOUNT_ID);
                        }
                    }

                    initAccountSettings(mSonetService, widget, accountId);
                }

                if (c.moveToFirst()) {
                    time24hr = c.getInt(c.getColumnIndexOrThrow(Widgets.TIME24HR)) == 1;

                    if (c.getInt(c.getColumnIndexOrThrow(Widgets.SOUND)) == 1) {
                        notifications |= Notification.DEFAULT_SOUND;
                    }

                    if (c.getInt(c.getColumnIndexOrThrow(Widgets.VIBRATE)) == 1) {
                        notifications |= Notification.DEFAULT_VIBRATE;
                    }

                    if (c.getInt(c.getColumnIndexOrThrow(Widgets.LIGHTS)) == 1) {
                        notifications |= Notification.DEFAULT_LIGHTS;
                    }
                }

                c.close();
                values.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(created, time24hr));
                // insert the message
                values.put(Statuses.WIDGET, widget);
                values.put(Statuses.ACCOUNT, accountId);
                mSonetService.getContentResolver().insert(Statuses.getContentUri(mSonetService), values);
                // check the status count, removing old sms
                Cursor statuses = mSonetService.getContentResolver()
                        .query(Statuses.getContentUri(mSonetService), new String[] { Statuses._ID },
                                Statuses.WIDGET + "=? and " + Statuses.ACCOUNT + "=?",
                                new String[] { Integer.toString(widget),
                                        Long.toString(accountId) },
                                Statuses.CREATED + " desc");

                if (statuses.moveToFirst()) {
                    while (!statuses.isAfterLast()) {
                        if (statuses.getPosition() >= status_count) {
                            mSonetService.getContentResolver().delete(Statuses.getContentUri(mSonetService),
                                    Statuses._ID + "=?",
                                    new String[] { Long.toString(statuses.getLong(statuses.getColumnIndex(Statuses._ID))) });
                        }

                        statuses.moveToNext();
                    }
                }

                statuses.close();

                if (notifications != 0) {
                    publishProgress(Integer.toString(notifications), friend + " sent a message");
                }

                widgets.moveToNext();
            }
        }

        widgets.close();
        return appWidgetIds;
    }

    @Override
    protected void onProgressUpdate(String... updates) {
        int notifications = Integer.parseInt(updates[0]);

        if (notifications != 0) {
            Notification notification = new Notification(R.drawable.notification, updates[1], System.currentTimeMillis());
            notification.setLatestEventInfo(mSonetService.getBaseContext(),
                    "New messages",
                    updates[1],
                    PendingIntent
                            .getActivity(mSonetService,
                                    0,
                                    new Intent(mSonetService, SonetNotifications.class),
                                    0));
            notification.defaults |= notifications;
            ((NotificationManager) mSonetService.getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFY_ID, notification);
        }
    }

    @Override
    protected void onPostExecute(int[] appWidgetIds) {
        // remove self from thread list
        if (!mSonetService.mSMSLoaders.isEmpty()) {
            mSonetService.mSMSLoaders.remove(this);
        }

        mSonetService.putValidatedUpdates(appWidgetIds, 0);
    }
}
