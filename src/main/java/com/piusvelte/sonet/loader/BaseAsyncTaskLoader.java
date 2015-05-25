package com.piusvelte.sonet.loader;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

/**
 * Created by bemmanuel on 3/17/15.
 */
abstract public class BaseAsyncTaskLoader<ResultType> extends AsyncTaskLoader<ResultType> {

    ResultType mData;

    public BaseAsyncTaskLoader(Context context) {
        super(context);
    }

    @Override
    public void deliverResult(ResultType data) {
        mData = data;

        if (isStarted()) {
            super.deliverResult(data);
        }
    }

    @Override
    protected void onStartLoading() {
        if (mData != null) {
            deliverResult(mData);
        } else {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    protected void onReset() {
        onStopLoading();
        mData = null;
    }
}
