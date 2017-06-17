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

package fi.craplab.roameo.util;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.data.Session;

import java.util.concurrent.TimeUnit;

import fi.craplab.roameo.R;
import fi.craplab.roameo.model.CallSession;

/**
 * Utility class for share operations, both general sharing and third party (Google Fit) sharing.
 */
public class ShareUtils {

    /**
     * Get share {@link Intent} for given {@link CallSession}.
     *
     * @param context Context
     * @param callSession CallSession to share
     * @return Share Intent
     */
    public static Intent getShareIntent(Context context, CallSession callSession) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, getShareText(context, callSession));

        return shareIntent;
    }

    /**
     * Get text for sharing CallSession
     * @param context Context
     * @param callSession CallSession to share
     * @return Share string
     */
    private static String getShareText(Context context, CallSession callSession) {
        // TODO use different texts for short/medium/long duration and pace
        String stepString = context.getResources().getQuantityString(R.plurals.steps,
                (int) callSession.stepCount, callSession.stepCount);
        return String.format(context.getString(R.string.share_details_text),
                stepString, Utils.millisToTimeString(callSession.duration));
    }

    /**
     * Create a Google Fit {@link Session} from a given {@link CallSession}.
     * This works for both uploading and deleting sessions. If the given {@link CallSession}
     * has no {@link CallSession#googleFitIdentifier} data stored, a new ID is created and
     * upload operation is presumed, otherwise the existing ID is taken.
     *
     * @param context Context
     * @param callSession CallSession to build Google Fit Session from
     * @return Google Fit Session
     */
    public static Session createGoogleFitSession(Context context,
                                                 @NonNull CallSession callSession) {

        String sessionId = (callSession.googleFitIdentifier == null)
                ? createGoogleFitSessionId(context, callSession.getId())
                : callSession.googleFitIdentifier;

        Session.Builder gFitSession = new Session.Builder()
                .setName(context.getString(R.string.google_fit_session_name))
                .setDescription(String.format(context.getString(
                        R.string.google_fit_session_description), callSession.getId()))
                .setIdentifier(sessionId)
                .setStartTime(callSession.timestamp, TimeUnit.MILLISECONDS)
                .setEndTime(callSession.timestamp + callSession.duration, TimeUnit.MILLISECONDS)
                .setActivity(FitnessActivities.WALKING);

        return gFitSession.build();
    }

    /**
     * Create a session id for Google Fit {@link Session}.
     * Session id will be of the form {@code Roameo-<database id>-<upload timestamp>}.
     *
     * @param context Context
     * @param callSessionId {@link CallSession} database id
     * @return Google Fit {@link Session} identifier string
     */
    private static String createGoogleFitSessionId(Context context, long callSessionId) {
        return String.format(context.getString(R.string.google_fit_session_identifier),
                callSessionId, System.currentTimeMillis());
    }
}
