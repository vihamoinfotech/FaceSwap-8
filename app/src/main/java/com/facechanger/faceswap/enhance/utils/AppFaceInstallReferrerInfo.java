package com.facechanger.faceswap.enhance.utils;

import android.app.Activity;
import android.util.Log;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;

public class AppFaceInstallReferrerInfo {

    public interface ReferrerCallback {
        void onReferrerReceived(String referrer);
    }

    private InstallReferrerClient referrerClient;

    public void getInstallReferrer(Activity activity, ReferrerCallback callback) {

        if (activity == null || activity.isDestroyed() || activity.isFinishing()) {
            callback.onReferrerReceived("");
            return;
        }

        referrerClient = InstallReferrerClient.newBuilder(activity).build();

        referrerClient.startConnection(new InstallReferrerStateListener() {

            @Override
            public void onInstallReferrerSetupFinished(int responseCode) {

                switch (responseCode) {

                    case InstallReferrerClient.InstallReferrerResponse.OK:

                        try {

                            ReferrerDetails response =
                                    referrerClient.getInstallReferrer();

                            String referrerUrl =
                                    response.getInstallReferrer();

                            long clickTime =
                                    response.getReferrerClickTimestampSeconds();

                            long installTime =
                                    response.getInstallBeginTimestampSeconds();

                            Log.d("REFERRER", "Referrer URL: " + referrerUrl);

                            callback.onReferrerReceived(referrerUrl);

                            referrerClient.endConnection();

                        } catch (Exception e) {

                            e.printStackTrace();

                            callback.onReferrerReceived("");
                        }

                        break;

                    case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:

                        Log.d("REFERRER", "Feature not supported");

                        callback.onReferrerReceived("");

                        break;

                    case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:

                        Log.d("REFERRER", "Service unavailable");

                        callback.onReferrerReceived("");

                        break;
                }
            }

            @Override
            public void onInstallReferrerServiceDisconnected() {

                Log.d("REFERRER", "Service disconnected");
            }
        });
    }
}
