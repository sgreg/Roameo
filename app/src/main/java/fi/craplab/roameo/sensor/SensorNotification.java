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

package fi.craplab.roameo.sensor;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;

import fi.craplab.roameo.R;
import fi.craplab.roameo.model.CallSession;
import fi.craplab.roameo.ui.DetailsDataFragment;
import fi.craplab.roameo.ui.SessionDetailsActivity;
import fi.craplab.roameo.util.DebugLog;
import fi.craplab.roameo.util.ShareUtils;
import fi.craplab.roameo.util.Utils;

/**
 * Call session notifications.
 *
 * Depending on notification settings, display
 * <ul>
 * <li> real-time information of ongoing call's step count
 * <li> session summary after the call ended
 * </ul>
 */
class SensorNotification {
    private static final String TAG = SensorNotification.class.getSimpleName();
    private static final String NOTIFICATION_TAG = "RoameoStepCount";
    private static final int NOTIFICATION_DEFAULT_ID = 0;

    private static int sTagId = NOTIFICATION_DEFAULT_ID;


    static void setId(int id) {
        sTagId = id;
    }

    /**
     * Show notification about ongoing call.
     * Displays the current step count and call start time in the notification bar.
     *
     * @param context Context
     * @param startTime Call start time string
     * @param steps Current step count
     */
    static void notifyOngoing(final Context context, final long startTime, final int steps) {
        NotificationCompat.Builder builder = getCommonBuilder(context);
        builder.setContentTitle(context.getString(R.string.notify_title_ongoing));
        String stepString = context.getResources().getQuantityString(R.plurals.steps, steps, steps);
        builder.setContentText(context.getString(R.string.notify_details_ongoing,
                stepString, Utils.millisToDateTimeString(startTime)));
        sendNotification(context, builder.build());
    }

    /**
     * Show notification of call summary after it ended.
     *
     * @param context Context
     * @param callSession Stored {@link CallSession}
     */
    static void notifyFinal(final Context context, final CallSession callSession) {
        DebugLog.d(TAG, "Notification for CallSession " + callSession);
        final Resources res = context.getResources();
        NotificationCompat.Builder builder = getCommonBuilder(context);

        if (callSession.getId() != null) {
            builder.setContentIntent(getDetailsIntent(context, callSession.getId()));
            builder.addAction(
                    R.drawable.ic_action_stat_reply,
                    res.getString(R.string.action_open),
                    getDetailsIntent(context, callSession.getId()));

            Intent shareIntent = ShareUtils.getShareIntent(context, callSession);
            builder.addAction(
                    R.drawable.ic_action_stat_share,
                    res.getString(R.string.action_share),
                    PendingIntent.getActivity(
                            context,
                            0,
                            Intent.createChooser(shareIntent, context.getString(R.string.app_name)),
                            PendingIntent.FLAG_UPDATE_CURRENT));

            builder.setContentTitle(context.getString(R.string.notify_title_final));
            String stepString = context.getResources().getQuantityString(R.plurals.steps,
                    (int) callSession.stepCount, callSession.stepCount);
            builder.setContentText(context.getString(R.string.notify_details_final_preview,
                    stepString, Utils.millisToTimeString(callSession.duration)));

            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(context.getString(
                            R.string.notify_details_final,
                            Utils.millisToDateTimeString(callSession.timestamp),
                            callSession.stepCount,
                            Utils.millisToTimeString(callSession.duration)))
                    .setBigContentTitle(context.getString(R.string.notify_title_final)));
        }
        sendNotification(context, builder.build());
    }

    /**
     * Create a common {@link NotificationCompat.Builder} for either type of notification.
     *
     * @param context Context
     * @return Common builder
     */
    private static NotificationCompat.Builder getCommonBuilder(final Context context) {
        return new NotificationCompat.Builder(context)
                // set all defaults to no light, sound or vibration and nullify alert sound
                .setDefaults(0)
                .setSound(null)
                // set icons
                .setSmallIcon(R.drawable.ic_roameo)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.roameo_circle))
                // set auto dismiss on touch
                .setAutoCancel(true);
    }

    private static PendingIntent getDetailsIntent(Context context, long id) {
        Intent intent = new Intent(context, SessionDetailsActivity.class);
        intent.putExtra(DetailsDataFragment.ARG_SECTION_NUMBER, id);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Send a notification to the {@link NotificationManager} to actually display it.
     *
     * @param context Context
     * @param notification Notification to display
     */
    private static void sendNotification(Context context, Notification notification) {
        final NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_TAG, sTagId, notification);
    }

    /**
     * Cancel any previous notification
     * @param context Context
     */
    static void cancel(final Context context) {
        final NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_TAG, sTagId);
        sTagId = NOTIFICATION_DEFAULT_ID;
    }
}
