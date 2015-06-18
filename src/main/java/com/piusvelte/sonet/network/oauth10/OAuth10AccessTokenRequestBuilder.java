package com.piusvelte.sonet.network.oauth10;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.SortedSet;

/**
 * Created by bemmanuel on 6/14/15.
 */
public class OAuth10AccessTokenRequestBuilder extends OAuth10SigningRequestBuilder {

    @Nullable
    private String mVerifier;

    public OAuth10AccessTokenRequestBuilder(@NonNull String consumerKey,
            @NonNull String consumerSecret,
            @NonNull String token,
            @NonNull String tokenSecret,
            @Nullable String verifier) {
        super(consumerKey, consumerSecret, token, tokenSecret);
        mVerifier = verifier;
    }

    @Override
    protected void onAddOAuthParameters(@NonNull SortedSet<String> parameters) {
        super.onAddOAuthParameters(parameters);

        if (!TextUtils.isEmpty(mVerifier)) {
            parameters.add(OAuthParameter.oauth_verifier.name() + "=" + mVerifier);
        }
    }
}
