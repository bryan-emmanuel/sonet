package com.piusvelte.sonet.network.oauth10;

import android.support.annotation.NonNull;

import java.util.SortedSet;

/**
 * Created by bemmanuel on 6/14/15.
 */
public class OAuth10RequestTokenRequestBuilder extends OAuth10RequestBuilder {

    @NonNull
    private String mCallbackUrl;

    public OAuth10RequestTokenRequestBuilder(@NonNull String consumerKey, @NonNull String consumerSecret, @NonNull String callbackUrl) {
        super(consumerKey, consumerSecret);
        mCallbackUrl = callbackUrl;
    }

    @Override
    protected void onAddOAuthParameters(@NonNull SortedSet<String> parameters) {
        parameters.add(OAuthParameter.oauth_callback.name() + "=" + mCallbackUrl);
    }
}
