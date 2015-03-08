package com.piusvelte.sonet.social;

import android.content.Context;

import com.piusvelte.sonet.BuildConfig;

import static com.piusvelte.sonet.Sonet.IDENTICA_BASE_URL;
import static com.piusvelte.sonet.Sonet.IDENTICA_MENTIONS;
import static com.piusvelte.sonet.Sonet.IDENTICA_UPDATE;
import static com.piusvelte.sonet.Sonet.IDENTICA_URL_FEED;
import static com.piusvelte.sonet.Sonet.IDENTICA_RETWEET;

/**
 * Created by bemmanuel on 2/15/15.
 */
public class IdentiCaClient extends TwitterClient {

    public IdentiCaClient(Context context, String token, String secret, String accountEsid) {
        super(context, token, secret, accountEsid);
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
}
