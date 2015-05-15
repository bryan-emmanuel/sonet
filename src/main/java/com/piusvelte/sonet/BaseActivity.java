package com.piusvelte.sonet;

import android.support.v7.app.AppCompatActivity;
import android.widget.FrameLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.piusvelte.sonet.fragment.BaseDialogFragment;

import static com.piusvelte.sonet.Sonet.PRO;

/**
 * Created by bemmanuel on 5/8/15.
 */
public abstract class BaseActivity extends AppCompatActivity implements BaseDialogFragment.OnResultListener {

    protected void setupAd() {
        if (!getPackageName().toLowerCase().contains(PRO)) {
            AdView adView = new AdView(this);
            ((FrameLayout) findViewById(R.id.ad)).addView(adView);
            adView.setAdUnitId(BuildConfig.GOOGLEAD_ID);
            adView.setAdSize(com.google.android.gms.ads.AdSize.BANNER);
            adView.loadAd(new AdRequest.Builder().build());
        }
    }
}
