package com.piusvelte.sonet.loader;

import android.content.Context;
import android.support.annotation.NonNull;

import com.piusvelte.sonet.SonetOAuth;
import com.piusvelte.sonet.social.Client;

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
            client = new Client.Builder(context)
                    .setNetwork(network)
                    .build();
            sonetOAuth = client.getLoginOAuth();
        }

        void loadAuthUrl() {
            authUrl = client.getAuthUrl(sonetOAuth);
        }

        public Client client;
        public SonetOAuth sonetOAuth;
        public String authUrl;

    }
}
