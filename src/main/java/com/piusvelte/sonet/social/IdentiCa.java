package com.piusvelte.sonet.social;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.piusvelte.sonet.BuildConfig;

/**
 * Created by bemmanuel on 2/15/15.
 */
public class IdentiCa extends Twitter {

    private static final String IDENTICA_BASE_URL = "https://identi.ca/api/";
    private static final String IDENTICA_URL_REQUEST = "%soauth/request_token";
    private static final String IDENTICA_URL_AUTHORIZE = "%soauth/authorize";
    private static final String IDENTICA_URL_ACCESS = "%soauth/access_token";
    private static final String IDENTICA_URL_FEED = "%sstatuses/home_timeline.json?count=%s";
    private static final String IDENTICA_RETWEET = "%sstatuses/retweet/%s.json";
    private static final String IDENTICA_UPDATE = "%sstatuses/update.json";
    private static final String IDENTICA_MENTIONS = "%sstatuses/mentions.json%s";
    private static final String IDENTICA_SINCE_ID = "?since_id=%s";
    private static final String IDENTICA_USER = "%susers/show.json?user_id=%s";
    private static final String IDENTICA_PROFILE = "http://identi.ca/%s";
    private static final String IDENTICA_DATE_FORMAT = "EEE MMM dd HH:mm:ss Z yyyy";

    public IdentiCa(Context context, String token, String secret, String accountEsid, int network) {
        super(context, token, secret, accountEsid, network);
    }

    @Nullable
    @Override
    public String getProfilePhotoUrl() {
        return getProfilePhotoUrl(mAccountEsid);
    }

    @Override
    public void onDelete() {
    }

    @Nullable
    @Override
    public Uri getCallback() {
        return Uri.parse("sonet://identi.ca");
    }

    String getBaseUrl() {
        return IDENTICA_BASE_URL;
    }

    String getFeedUrl() {
        return IDENTICA_URL_FEED;
    }

    String getMentionsUrl() {
        return IDENTICA_MENTIONS;
    }

    String getUpdateUrl() {
        return IDENTICA_UPDATE;
    }

    String getRetweetUrl() {
        return IDENTICA_RETWEET;
    }

    @Override
    String getApiKey() {
        return BuildConfig.IDENTICA_KEY;
    }

    @Override
    String getApiSecret() {
        return BuildConfig.IDENTICA_SECRET;
    }

    @Override
    String getRequestUrlFormat() {
        return IDENTICA_URL_REQUEST;
    }

    @Override
    String getAccessUrlFormat() {
        return IDENTICA_URL_ACCESS;
    }

    @Override
    String getAuthorizeUrlFormat() {
        return IDENTICA_URL_AUTHORIZE;
    }

    @Override
    String getVerifyCredentialsUrl() {
        return "https://identi.ca/api/account/verify_credentials.json";
    }

    @Override
    String getProfileUrl() {
        return IDENTICA_PROFILE;
    }
}
