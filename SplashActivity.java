package com.package.name;

/**
 * Created by Abdelaali on 24/09/2017.
 */

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.boosting.audiobooster.R;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;

import static com.package.name.AdRequestBuilder.AfterRequestCallback;
import static com.package.name.AdRequestBuilder.NeedToRequestCallback;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG ="TAG_APP" ;
    private AdRequestBuilder ad;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bg_splash);
        Log.d(TAG,"on create ");

        /* init ad request and consent form */
        ad =    new AdRequestBuilder.Builder(this)
                .withDebuggin(true)
                .withPrivacyUrl("http://privacypolicy.com")
                .withPublisherIds( new String[]{"pub-1234"})
                .withTAG("LIBBB")
                .withTestDevice("112233445566778899")
                .withSetEEA(true)
                .build();

        final NeedToRequestCallback needToRequestCallback  = new NeedToRequestCallback() {
            @Override
            public void requestConsent(boolean needRequest) {
                Log.d(TAG,"need request call ! " + needRequest);
                if(needRequest)
                {
                    Log.d(TAG,"requesting ");
                    AfterRequestCallback afterRequestCallback = new AfterRequestCallback() {
                        @Override
                        public void afterRequestConsent(boolean afterRequest)
                        {
                            if(afterRequest)
                            {
                                Log.d(TAG,"after request " );
                            }
                            else
                            {
                                Log.d(TAG,"false after resuest" );
                            }
                        }
                    };
                    ad.requestConsent(afterRequestCallback);
                }
                else
                {
                    Log.d(TAG,"starting main activity");
                }

            }
        };


        ad.checkForConsent(needToRequestCallback);
    }
}