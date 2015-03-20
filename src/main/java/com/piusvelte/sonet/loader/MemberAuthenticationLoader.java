package com.piusvelte.sonet.loader;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.piusvelte.sonet.social.SocialClient;

/**
 * Created by bemmanuel on 3/19/15.
 */
public class MemberAuthenticationLoader extends BaseAsyncTaskLoader {

    @NonNull
    OAuthLoginLoader.OAuthLoginLoaderResult mOAuthLoginLoaderResult;
    @NonNull
    String mAuthenticatedUrl;
    @Nullable
    SocialClient.MemberAuthentication mMemberAuthentication;

    public MemberAuthenticationLoader(@NonNull Context context, @NonNull OAuthLoginLoader.OAuthLoginLoaderResult oAuthLoginLoaderResult, @NonNull String authenticatedUrl) {
        super(context);
        mOAuthLoginLoaderResult = oAuthLoginLoaderResult;
        mAuthenticatedUrl = authenticatedUrl;
    }

    @Override
    public Object loadInBackground() {
        mMemberAuthentication = mOAuthLoginLoaderResult.socialClient.getMemberAuthentication(mOAuthLoginLoaderResult.sonetOAuth, mAuthenticatedUrl);
        return mMemberAuthentication;
    }
}
