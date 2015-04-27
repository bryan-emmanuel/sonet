package com.piusvelte.sonet.loader;

import android.content.Context;
import android.support.annotation.NonNull;

/**
 * Created by bemmanuel on 3/19/15.
 */
public class MemberAuthenticationLoader extends BaseAsyncTaskLoader {

    @NonNull
    OAuthLoginLoader.OAuthLoginLoaderResult mOAuthLoginLoaderResult;
    @NonNull
    String mAuthenticatedUrl;

    public MemberAuthenticationLoader(@NonNull Context context,
            @NonNull OAuthLoginLoader.OAuthLoginLoaderResult oAuthLoginLoaderResult,
            @NonNull String authenticatedUrl) {
        super(context);
        mOAuthLoginLoaderResult = oAuthLoginLoaderResult;
        mAuthenticatedUrl = authenticatedUrl;
    }

    @Override
    public Object loadInBackground() {
        return mOAuthLoginLoaderResult.client.getMemberAuthentication(mOAuthLoginLoaderResult.sonetOAuth, mAuthenticatedUrl);
    }
}
