package com.piusvelte.sonet.loader;

import android.content.Context;
import android.support.annotation.NonNull;

import com.piusvelte.sonet.social.Client;

/**
 * Created by bemmanuel on 4/21/15.
 */
public class LikeCommentLoader extends BaseAsyncTaskLoader {

    private Client mClient;
    private String mSid;
    private String mEsid;
    private boolean mDoLike;

    public LikeCommentLoader(Context context, @NonNull Client client, @NonNull String sid, @NonNull String esid, boolean doLike) {
        super(context);
        mClient = client;
        mSid = sid;
        mEsid = esid;
        mDoLike = doLike;
    }

    @Override
    public Object loadInBackground() {
        Result result = new Result();
        result.wasSuccessful = mClient.likeStatus(mSid, mEsid, mDoLike);
        result.isLiked = mDoLike == result.wasSuccessful;
        return result;
    }

    public static class Result {
        public boolean isLiked;
        public boolean wasSuccessful;
    }
}
