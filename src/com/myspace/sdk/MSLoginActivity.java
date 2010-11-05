package com.myspace.sdk;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.myspace.sdk.MSWebView.IMSWebViewCallback;

public class MSLoginActivity extends Activity {

	private static final String TAG = "MSLoginActivity";
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        MSWebView msDialog = new MSLoginWebView(this, MSSession.getSession(), new MSWebViewCallback());
        msDialog.show();
    }
	
	private void showToast(String string) {
		Toast.makeText(this, string, Toast.LENGTH_SHORT).show();
	}
	
	private class MSWebViewCallback implements IMSWebViewCallback {

		public void webViewDidCancel(MSWebView webView) {
			webView.close();
			Log.w(TAG, "Login Cancel");
			showToast("Login Cancel");
		}

		public void webViewDidSucceed(MSWebView webView) {
			webView.close();
		}

		public void webViewDidFail(MSWebView webView, Throwable error) {
			webView.close();
			showToast("Login Fail: " + error);
		}
    }
}
