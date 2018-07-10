package com.package.name;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.ads.consent.AdProvider;
import com.google.ads.consent.ConsentForm;
import com.google.ads.consent.ConsentFormListener;
import com.google.ads.consent.ConsentInfoUpdateListener;
import com.google.ads.consent.ConsentInformation;
import com.google.ads.consent.ConsentStatus;
import com.google.ads.consent.DebugGeography;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class AdRequestBuilder extends AppCompatActivity
{
    private  String TAG ;
    private  Context context;
    private static final String PREFS = "PREFS";
    private static final String SHOW_PA = "SHOW_PA";
    SharedPreferences sharedPreferences;
    private ConsentForm form;
    private String[] publisherIds;
    private String privacyUrl ;
    private Boolean debug = false;
    private Boolean setEEA = false;
    private String testDevice;

    public AdRequestBuilder(Builder builder)
    {
        this.context      = builder.context;
        this.privacyUrl   = builder.privacyUrl;
        this.publisherIds = builder.publisherIds;
        this.debug        = builder.debug;
        this.testDevice   = builder.testDevice;
        this.TAG          = builder.TAG;
        sharedPreferences = context.getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    public AdRequest getAdRequest()
    {
        AdRequest adRequest;
        Boolean showpPersonnalizedAds = false;
        sharedPreferences = context.getSharedPreferences(PREFS, MODE_PRIVATE);
        if (sharedPreferences.contains(SHOW_PA))
        {
            showpPersonnalizedAds = sharedPreferences.getBoolean(SHOW_PA, false);

        }
        else {
            sharedPreferences
                    .edit()
                    .putBoolean(SHOW_PA, false)
                    .apply();
        }

        // test personnalized
        if (showpPersonnalizedAds)
        {
            adRequest = new AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter.class, getNonPersonalizedAdsBundle())
                    .build();
            if(debug) Log.d(TAG,"PA");
            return adRequest;
        }
        else
        {
            adRequest = new AdRequest.Builder()
                    .build();
            if(debug) Log.d(TAG,"NPA");
            return adRequest;
        }

    }
        public Bundle getNonPersonalizedAdsBundle() {
                Bundle extras = new Bundle();
                extras.putString("npa", "1");

                return extras;
            }
    /*
check for consent
 */
    public void checkForConsent(final NeedToRequestCallback needToRequestCallback)
    {
        final boolean needToRequest = true;
        ConsentInformation consentInformation = ConsentInformation.getInstance(context);
        List<AdProvider> adProviders = ConsentInformation.getInstance(context).getAdProviders();
        // Geography appears as in EEA for test devices.
        if(setEEA) ConsentInformation.getInstance(context).setDebugGeography(DebugGeography.DEBUG_GEOGRAPHY_EEA);
        if(this.testDevice.length() > 0) ConsentInformation.getInstance(context).addTestDevice(this.testDevice);
        if(debug)
            Log.d(TAG,"adProviders : " +adProviders.size());

        if(debug)
            Log.d(TAG,"Consent EEA: " + ConsentInformation.getInstance(context).isRequestLocationInEeaOrUnknown());

            consentInformation.requestConsentInfoUpdate(publisherIds, new ConsentInfoUpdateListener() {
                @Override
                public void onConsentInfoUpdated(ConsentStatus consentStatus) {
                    // User's consent status successfully updated.
                    switch (consentStatus) {
                        case PERSONALIZED:
                            if(debug) Log.d(TAG, "Showing Personalized ads");
                            sharedPreferences
                                    .edit()
                                    .putBoolean(SHOW_PA, true)
                                    .apply();
                            if(needToRequestCallback != null)
                                needToRequestCallback.requestConsent(false);
                            break;
                        case NON_PERSONALIZED:
                            if(debug) Log.d(TAG, "Showing Non-Personalized ads");
                            sharedPreferences
                                    .edit()
                                    .putBoolean(SHOW_PA, false)
                                    .apply();
                            if(needToRequestCallback != null)
                                needToRequestCallback.requestConsent(false);
                            break;
                        case UNKNOWN:
                            if(debug) Log.d(TAG, "Requesting Consent");
                            if (ConsentInformation.getInstance(getBaseContext())
                                    .isRequestLocationInEeaOrUnknown()) {
                                if(needToRequestCallback != null)
                                    needToRequestCallback.requestConsent(true);
                            } else {
                                sharedPreferences
                                        .edit()
                                        .putBoolean(SHOW_PA, true)
                                        .apply();
                                if(needToRequestCallback != null)
                                    needToRequestCallback.requestConsent(false);
                                break;
                            }
                            break;
                        default:
                            break;
                    }
                }

                @Override
                public void onFailedToUpdateConsentInfo(String errorDescription) {
                    // User's consent status failed to update.
                    if(debug) Log.d(TAG, "User's consent status failed to update.");
                    if(needToRequestCallback != null)
                        needToRequestCallback.requestConsent(false);
                }
            });

    }

    public void requestConsent(final AfterRequestCallback afterRequestCallback) {
        URL privacyUrl = null;
        try {
            // TODO: Replace with your app's privacy policy URL.
            privacyUrl = new URL(this.privacyUrl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            // Handle error.
        }
        form = new ConsentForm.Builder(context, privacyUrl)
                .withListener(new ConsentFormListener() {
                    @Override
                    public void onConsentFormLoaded() {
                        // Consent form loaded successfully.
                        if(debug) Log.d(TAG, "Requesting Consent: onConsentFormLoaded");
                        showForm();
                    }

                    @Override
                    public void onConsentFormOpened() {
                        // Consent form was displayed.
                        if(debug) Log.d(TAG, "Requesting Consent: onConsentFormOpened");
                       // afterRequestCallback.afterRequestConsent(true);
                    }

                    @Override
                    public void onConsentFormClosed(
                            ConsentStatus consentStatus, Boolean userPrefersAdFree) {
                        if(debug) if(debug) Log.d(TAG, "Requesting Consent: onConsentFormClosed");
                        if (userPrefersAdFree) {
                            // Buy or Subscribe
                            if(debug) Log.d(TAG, "Requesting Consent: User prefers AdFree");
                            afterRequestCallback.afterRequestConsent(true);
                        } else {
                            if(debug) Log.d(TAG, "Requesting Consent: Requesting consent again");
                            switch (consentStatus) {
                                case PERSONALIZED:
                                    sharedPreferences
                                            .edit()
                                            .putBoolean(SHOW_PA, true)
                                            .apply();
                                    afterRequestCallback.afterRequestConsent(true);
                                    break;
                                case NON_PERSONALIZED:
                                    sharedPreferences
                                            .edit()
                                            .putBoolean(SHOW_PA, false)
                                            .apply();
                                    afterRequestCallback.afterRequestConsent(true);
                                    break;
                                case UNKNOWN:
                                    sharedPreferences
                                            .edit()
                                            .putBoolean(SHOW_PA, false)
                                            .apply();
                                    afterRequestCallback.afterRequestConsent(true);
                                    break;
                            }

                        }
                        // Consent form was closed.
                    }

                    @Override
                    public void onConsentFormError(String errorDescription) {
                        if(debug) Log.d(TAG, "Requesting Consent: onConsentFormError. Error - " + errorDescription);
                        // Consent form error.
                        afterRequestCallback.afterRequestConsent(true);
                    }
                })
                .withPersonalizedAdsOption()
                .withNonPersonalizedAdsOption()
                .build();
        form.load();
    }

    private void showForm() {
        if (form == null) {
            if(debug) Log.d(TAG, "Consent form is null");
        }
        if (form != null) {
            if(debug) Log.d(TAG, "Showing consent form");
            form.show();
        } else {
            if(debug) Log.d(TAG, "Not Showing consent form");
        }
    }

    /**
     * Creates a new {@link Builder} for constructing a {@link AdRequestBuilder}.
     */
    public static class Builder {

     private final Context context;
     private String TAG;
     private String privacyUrl;
     private String[] publisherIds;
     private String testDevice;
     private Boolean debug;
     private Boolean setEEA;

        public Builder(Context context) {
            this.context = context;
            this.TAG = "TAG";
            this.debug = false;
            this.testDevice="";
            this.setEEA = false;



        }

        public Builder withPrivacyUrl(String privacyUrl)
        {
            if (privacyUrl == null) {
                throw new IllegalArgumentException("Must provide valid app privacy policy url"
                        + " to create a ConsentForm");
            }

            this.privacyUrl = privacyUrl;
            return this;
        }

        public Builder withPublisherIds(String[] publisherIds)
        {
            this.publisherIds = publisherIds;
            return this;
        }
        public Builder withTestDevice(String device)
        {
            this.testDevice = device;
            return this;
        }

        public Builder withDebuggin(Boolean debug) {
            this.debug = debug;
            return this;
        }
        public Builder withTAG(String TAG)
        {
            this.TAG = TAG;
            return this;
        }
        public Builder withSetEEA(Boolean setEEA)
        {
            this.setEEA = setEEA;
            return this;
        }

        public AdRequestBuilder build()
        {
            return new AdRequestBuilder(this);
        }
    }
    public interface NeedToRequestCallback {
        public void requestConsent(boolean needRequest);
    }
    public interface AfterRequestCallback {
        public void afterRequestConsent(boolean afterRequest);
    }
}
