package com.piusvelte.sonet.loader;

import android.content.Context;
import android.support.annotation.NonNull;

import com.piusvelte.sonet.social.Client;

/**
 * Created by bemmanuel on 3/17/15.
 */
public class OAuthLoginLoader extends BaseAsyncTaskLoader<OAuthLoginLoader.OAuthLoginLoaderResult> {

    Context mContext;
    int mNetwork;

    public OAuthLoginLoader(Context context, int network) {
        super(context);
        mContext = context.getApplicationContext();
        mNetwork = network;
    }

    @Override
    public OAuthLoginLoader.OAuthLoginLoaderResult loadInBackground() {
        OAuthLoginLoaderResult oAuthLoginLoaderResult = new OAuthLoginLoaderResult(mContext, mNetwork);
        oAuthLoginLoaderResult.loadAuthUrl();
        return oAuthLoginLoaderResult;
    }

    public class OAuthLoginLoaderResult {

        public OAuthLoginLoaderResult(@NonNull Context context, int network) {
            client = new Client.Builder(context)
                    .setNetwork(network)
                    .build();
        }

        void loadAuthUrl() {
            authUrl = client.getAuthUrl();
        }

        public Client client;
        public String authUrl;
    }
}
