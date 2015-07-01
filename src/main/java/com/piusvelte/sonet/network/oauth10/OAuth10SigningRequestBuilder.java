package com.piusvelte.sonet.network.oauth10;

import android.support.annotation.NonNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Set;

import javax.crypto.spec.SecretKeySpec;

/**
 * Created by bemmanuel on 6/14/15.
 */
public class OAuth10SigningRequestBuilder extends OAuth10RequestBuilder {

    @NonNull
    private String mToken;
    @NonNull
    private String mTokenSecret;

    public OAuth10SigningRequestBuilder(@NonNull String consumerKey,
            @NonNull String consumerSecret,
            @NonNull String token,
            @NonNull String tokenSecret) {
        super(consumerKey, consumerSecret);
        mToken = token;
        mTokenSecret = tokenSecret;
    }

    @Override
    protected void onAddOAuthParameters(@NonNull Set<String> parameters) {
        parameters.add(OAuthParameter.oauth_token.name() + "=" + mToken);
    }

    @Override
    protected SecretKeySpec createKey() throws UnsupportedEncodingException {
        String keyData = URLEncoder.encode(mConsumerSecret, "UTF-8") + "&" + URLEncoder.encode(mTokenSecret, "UTF-8");
        byte[] keyBytes = keyData.getBytes("UTF-8");
        return new SecretKeySpec(keyBytes, "HmacSHA1");
    }
}
