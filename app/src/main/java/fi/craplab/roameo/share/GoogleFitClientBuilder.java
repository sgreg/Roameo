/*
 * Roameo - Your call for a healthier life
 *
 * Copyright (C) 2017 Sven Gregori <sven@craplab.fi>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package fi.craplab.roameo.share;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;

import java.util.Locale;

import fi.craplab.roameo.util.DebugLog;

/**
 *
 */
public class GoogleFitClientBuilder {
    private static final String TAG = GoogleFitClientBuilder.class.getSimpleName();
    private static GoogleFitClientBuilder sInstance;

    private GoogleApiClient mClient;
    private final FragmentActivity mActivity;

    private GoogleFitClientBuilder(FragmentActivity activity) {
        mActivity = activity;
    }

    public static GoogleApiClient getApiClient() {
        return (sInstance == null) ? null : sInstance.mClient;
    }

    public static void createApiClient(FragmentActivity activity) {
        if (sInstance == null) {
            sInstance = new GoogleFitClientBuilder(activity);
            sInstance.buildClient();
        }
    }

    public static void destroyApiClient() {
        if (sInstance != null) {
            // TODO check using .addApi(Fitness.CONFIG_API) and then Fitness.ConfigApi.disableFit()
            sInstance.mClient.stopAutoManage(sInstance.mActivity);
            sInstance = null;
        }
    }

    private void buildClient() {
        DebugLog.d(TAG, "Building Google Fit API client");
        mClient = new GoogleApiClient.Builder(mActivity)
                .addApi(Fitness.HISTORY_API)
                .addApi(Fitness.SESSIONS_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        DebugLog.d(TAG, "Connected with Google Fit: " + mClient.isConnected());
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        DebugLog.d(TAG, "Google Fit connection " + i + " suspended");
                    }
                })
                .enableAutoManage(mActivity, 0, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        DebugLog.e(TAG, String.format(Locale.US,
                                "Connection to Google Fit failed %s resolution: %d %s %s",
                                (connectionResult.hasResolution() ? "with" : "without"),
                                connectionResult.getErrorCode(),
                                connectionResult.getErrorMessage(),
                                connectionResult.toString()));
                    }
                })
                .build();
    }
}
