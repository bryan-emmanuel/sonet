package com.piusvelte.sonet.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;

import com.piusvelte.sonet.Sonet;

/**
 * Created by bemmanuel on 3/22/15.
 */
public final class StatusesStyles implements BaseColumns {

    public static final String VIEW = "statuses_styles";

    // this is actually a view, joining the account/widget/default styles to the statuses

    private StatusesStyles() {
    }

    public static Uri getContentUri(Context context) {
        return Uri.parse("content://" + Sonet.getAuthority(context) + "/statuses_styles");
    }

    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.statuses_styles";

    public static final String CREATED = "created";
    public static final String FRIEND = "friend";
    public static final String PROFILE_URL = "profile_url";
    public static final String MESSAGE = "message";
    public static final String SERVICE = "service";
    public static final String WIDGET = "widget";
    // account specific settings per widget
    public static final String ACCOUNT = "account";
    public static final String CREATEDTEXT = "createdtext";
    // service id, for posting and linking
    public static final String SID = "sid";
    // store friend and profile data in a separate table
    public static final String ENTITY = "entity";
    public static final String ESID = "esid";
    public static final String IMAGE_URL = "image_url";

    public static void createView(@NonNull SQLiteDatabase db) {
        db.execSQL("create view if not exists " + VIEW + " as select " +
                "s." + Statuses._ID + " as " + StatusesStyles._ID
                + ",s." + Statuses.CREATED + " as " + StatusesStyles.CREATED
                + ",(case when " + "s." + Statuses.FRIEND_OVERRIDE + " != \"\" then " + "s." + Statuses.FRIEND_OVERRIDE + " else " + "e." +
                Entity.FRIEND + " end) as " + StatusesStyles.FRIEND
                + ",s." + Statuses.MESSAGE + " as " + StatusesStyles.MESSAGE
                + ",s." + Statuses.SERVICE + " as " + StatusesStyles.SERVICE
                + ",s." + Statuses.CREATEDTEXT + " as " + StatusesStyles.CREATEDTEXT
                + ",s." + Statuses.WIDGET + " as " + StatusesStyles.WIDGET
                + ",s." + Statuses.ACCOUNT + " as " + StatusesStyles.ACCOUNT
                + ",s." + Statuses.SID + " as " + StatusesStyles.SID
                + ",e." + Entity._ID + " as " + StatusesStyles.ENTITY
                + ",e." + Entity.ESID + " as " + StatusesStyles.ESID
                + ",e." + Entity.PROFILE_URL + " as " + StatusesStyles.PROFILE_URL
                + ",i." + StatusImages.URL + " as " + StatusesStyles.IMAGE_URL
                + " from " + Statuses.TABLE + " s,"
                + Entity.TABLE + " e"
                + " left join " + StatusImages.TABLE + " i"
                + " on i." + StatusImages.STATUS_ID + "=s." + Statuses._ID
                + " where "
                + "e." + Entity._ID + "=s." + Statuses.ENTITY + ";");
    }
}
