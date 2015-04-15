package com.piusvelte.sonet.social;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.piusvelte.sonet.BuildConfig;

import java.util.HashMap;
import java.util.List;

import static com.piusvelte.sonet.Sonet.IDENTICA_BASE_URL;
import static com.piusvelte.sonet.Sonet.IDENTICA_MENTIONS;
import static com.piusvelte.sonet.Sonet.IDENTICA_UPDATE;
import static com.piusvelte.sonet.Sonet.IDENTICA_URL_ACCESS;
import static com.piusvelte.sonet.Sonet.IDENTICA_URL_AUTHORIZE;
import static com.piusvelte.sonet.Sonet.IDENTICA_URL_FEED;
import static com.piusvelte.sonet.Sonet.IDENTICA_RETWEET;
import static com.piusvelte.sonet.Sonet.IDENTICA_URL_REQUEST;

/**
 * Created by bemmanuel on 2/15/15.
 */
public class IdentiCa extends Twitter {

    public IdentiCa(Context context, String token, String secret, String accountEsid, int network) {
        super(context, token, secret, accountEsid, network);
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
}
