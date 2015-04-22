package com.piusvelte.sonet.loader;

import android.content.Context;
import android.support.annotation.NonNull;

import com.piusvelte.sonet.social.Client;

/**
 * Created by bemmanuel on 4/21/15.
 */
public class SendCommentLoader extends BaseAsyncTaskLoader {

    private Client mClient;
    private String mSid;
    private String mMessage;

    public SendCommentLoader(Context context, @NonNull Client client, @NonNull String sid, @NonNull String message) {
        super(context);
        mClient = client;
        mSid = sid;
        mMessage = message;
    }

    @Override
    public Object loadInBackground() {
        return mClient.sendComment(mSid, mMessage);
    }
}
