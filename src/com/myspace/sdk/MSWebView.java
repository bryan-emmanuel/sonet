package com.myspace.sdk;

import android.app.Activity;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public abstract class MSWebView {

	protected MSSession mSession;
    protected Activity mContext;
    protected IMSWebViewCallback mCallback;
    
    private WebView mWebView;
    
    public MSWebView(Activity context, MSSession session, IMSWebViewCallback callback) {
    	mContext = context;
    	mSession = session;
    	mCallback = callback;
    	
    	mWebView = new WebView(context);
    	context.setContentView(mWebView);
		mWebView.setWebViewClient(new WebViewClientImpl());

    	WebSettings webSettings = mWebView.getSettings();
    	webSettings.setJavaScriptEnabled(true);
    	webSettings.setDefaultTextEncodingName("UTF-8");
    }
    
    public void open(String url) {
    	Log.v("MSWebView","open:"+url);
    	mWebView.loadUrl(url);
    }
    
    public abstract void show();
    public abstract void process(Uri uri);

    private final class WebViewClientImpl extends WebViewClient {
    	
    	@Override
    	public boolean shouldOverrideUrlLoading(WebView view, String url) {
    		Uri uri = Uri.parse(url);
    		if (uri != null && uri.toString().startsWith(mSession.getApiCallBackUrl().toLowerCase())) {
    			process(uri);
    		}	
    		return false;
    	}
    }
    
    protected void doSucceedCallback() {
    	if(mCallback != null) {
    		mCallback.webViewDidSucceed(this);
    	}
    }
    
    protected void doFailCallback(String source, Throwable error) {
    	Log.e(source, error.toString());
		if(mCallback != null) {
    		mCallback.webViewDidFail(this, error);
    	}
    }
    
    protected void doCancelCallback() {
    	if(mCallback != null) {
    		mCallback.webViewDidCancel(this);
    	}
    }
    
    public void close() {
    	mContext.finish();
    }
    
    public static interface IMSWebViewCallback {
    	void webViewDidSucceed(MSWebView webView);
    	void webViewDidCancel(MSWebView webView);
    	void webViewDidFail(MSWebView webView, Throwable error);
    }
}
