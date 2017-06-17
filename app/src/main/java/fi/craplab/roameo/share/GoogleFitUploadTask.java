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
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.SessionInsertRequest;

import java.util.concurrent.TimeUnit;

import fi.craplab.roameo.R;
import fi.craplab.roameo.RoameoEvents;
import fi.craplab.roameo.model.CallSession;
import fi.craplab.roameo.util.DebugLog;
import fi.craplab.roameo.util.ShareUtils;

/**
 * Google Fit session upload {@link AsyncTask}.
 */
public class GoogleFitUploadTask extends AsyncTask<Void, Void, Void> {
    private static final String TAG = GoogleFitUploadTask.class.getSimpleName();

    private final GoogleApiClient mClient;
    private final CallSession mCallSession;

    public GoogleFitUploadTask(GoogleApiClient googleApiClient, CallSession callSession) {
        mClient = googleApiClient;
        mCallSession = callSession;
    }

    private SessionInsertRequest buildCallSession() {
        DebugLog.d(TAG, "Building Google Fit session from " + mCallSession);
        Context context = mClient.getContext();

        DataSource stepSource = new DataSource.Builder()
                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .setAppPackageName(context.getString(R.string.app_package))
                .setType(DataSource.TYPE_RAW)
                .build();

        DataSet stepDataSet = DataSet.create(stepSource);

        DataPoint stepDataPoint = stepDataSet.createDataPoint();
        stepDataPoint.setTimeInterval(
                mCallSession.timestamp,
                mCallSession.timestamp + mCallSession.duration,
                TimeUnit.MILLISECONDS);
        stepDataPoint.getValue(Field.FIELD_STEPS).setInt((int) mCallSession.stepCount);
        stepDataSet.add(stepDataPoint);

        Session gFitSession = ShareUtils.createGoogleFitSession(context, mCallSession);

        return new SessionInsertRequest.Builder()
                .setSession(gFitSession)
                .addDataSet(stepDataSet)
                .build();
    }

    @Override
    protected Void doInBackground(Void... params) {
        SessionInsertRequest sessionInsertRequest = buildCallSession();

        DebugLog.d(TAG, "Sending session insert request to Google Fit API");

        if (!mClient.isConnected()) {
            DebugLog.i(TAG, "Client not connected, connecting now");
            mClient.connect();
        }

        com.google.android.gms.common.api.Status insertStatus =
                Fitness.SessionsApi.insertSession(mClient, sessionInsertRequest)
                        .await(1, TimeUnit.MINUTES);

        if (insertStatus.isSuccess()) {
            String sessionId = sessionInsertRequest.getSession().getIdentifier();
            DebugLog.i(TAG, "Session successfully inserted, saving session id " + sessionId);
            mCallSession.googleFitIdentifier = sessionId;
            mCallSession.save();
            RoameoEvents.send(mClient.getContext(), RoameoEvents.ACTION_GOOGLE_FIT_DATA_UPLOADED);

        } else {
            DebugLog.e(TAG, "Failed to insert session: " + insertStatus.toString());
            RoameoEvents.send(mClient.getContext(), RoameoEvents.ACTION_GOOGLE_FIT_DATA_UPLOAD_FAIL);
        }

        return null;
    }
}
