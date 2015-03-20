package com.piusvelte.sonet.loader;

import android.content.Context;
import android.support.annotation.NonNull;

import com.piusvelte.sonet.SonetOAuth;
import com.piusvelte.sonet.social.SocialClient;

/**
 * Created by bemmanuel on 3/17/15.
 */
public class OAuthLoginLoader extends BaseAsyncTaskLoader {

    Context mContext;
    int mNetwork;
    OAuthLoginLoaderResult mOAuthLoginLoaderResult;

    public OAuthLoginLoader(Context context, int network) {
        super(context);
        mContext = context.getApplicationContext();
        mNetwork = network;
    }

    @Override
    public Object loadInBackground() {
        mOAuthLoginLoaderResult = new OAuthLoginLoaderResult(mContext, mNetwork);
        mOAuthLoginLoaderResult.loadAuthUrl();
        return mOAuthLoginLoaderResult;
    }

    public class OAuthLoginLoaderResult {

        public OAuthLoginLoaderResult(@NonNull Context context, int network) {
            socialClient = new SocialClient.Builder(context)
                    .setNetwork(network)
                    .build();
            sonetOAuth = socialClient.getLoginOAuth();
        }

        void loadAuthUrl() {
            authUrl = socialClient.getAuthUrl(sonetOAuth);
        }

        public SocialClient socialClient;
        public SonetOAuth sonetOAuth;
        public String authUrl;

    }
}
