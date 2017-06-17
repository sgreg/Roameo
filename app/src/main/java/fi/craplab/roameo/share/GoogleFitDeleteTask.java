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

import android.content.Context;
import android.os.AsyncTask;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.DataDeleteRequest;

import java.util.concurrent.TimeUnit;

import fi.craplab.roameo.RoameoEvents;
import fi.craplab.roameo.model.CallSession;
import fi.craplab.roameo.util.DebugLog;
import fi.craplab.roameo.util.ShareUtils;

/**
 * Google Fit session deletion {@link AsyncTask}.
 */
public class GoogleFitDeleteTask extends AsyncTask<Void, Void, Void> {
    private static final String TAG = GoogleFitDeleteTask.class.getSimpleName();

    private final GoogleApiClient mClient;
    private final CallSession mCallSession;

    public GoogleFitDeleteTask(GoogleApiClient googleApiClient, CallSession callSession) {
        mClient = googleApiClient;
        mCallSession = callSession;
    }

    @Override
    protected Void doInBackground(Void... params) {
        Context context = mClient.getContext();

        Session gFitSession = ShareUtils.createGoogleFitSession(context, mCallSession);

        DataDeleteRequest deleteRequest = new DataDeleteRequest.Builder()
                .setTimeInterval(
                        mCallSession.timestamp,
                        mCallSession.timestamp + mCallSession.duration,
                        TimeUnit.MILLISECONDS)
                .deleteAllData()
                .addSession(gFitSession)
                .build();

        DebugLog.d(TAG, "Sending session delete request to Google Fit API");

        if (!mClient.isConnected()) {
            DebugLog.i(TAG, "Client not connected, connecting now");
            mClient.connect();
        }

        com.google.android.gms.common.api.Status deleteStatus =
                Fitness.HistoryApi.deleteData(mClient, deleteRequest).await(1, TimeUnit.MINUTES);

        if (deleteStatus.isSuccess()) {
            DebugLog.d(TAG, "Session deleted.");
            RoameoEvents.send(context, RoameoEvents.ACTION_GOOGLE_FIT_DATA_DELETED);
        } else {
            // TODO this should get some warning dialog if that really happens
            DebugLog.e(TAG, "Session deletion failed: " + deleteStatus.toString());
            RoameoEvents.send(context, RoameoEvents.ACTION_GOOGLE_FIT_DATA_DELETE_FAIL);
        }

        return null;
    }
}
