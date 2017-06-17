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

package fi.craplab.roameo.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import fi.craplab.roameo.R;
import fi.craplab.roameo.RoameoEvents;
import fi.craplab.roameo.model.CallSession;
import fi.craplab.roameo.util.DebugLog;
import fi.craplab.roameo.util.Utils;

/**
 * General summary fragment.
 * This is the default fragment on application start.
 *
 * Show summary of all collected data.
 */
// TODO also show summary of all general states (Google Fit, notifications enabled, sensor found)
public class SummaryFragment extends Fragment {
    private static final String TAG = SummaryFragment.class.getSimpleName();

    private TextView mSessionCountTextView;
    private TextView mStepCountTextView;
    private TextView mDurationTextView;
    private TextView mAvgStepCountTextView;
    private TextView mAvgDurationTextView;
    private TextView mAvgPaceTextView;
    private TextView mMaxStepCountTextView;
    private TextView mMaxDurationTextView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getContext();
        boolean notificationsEnabled = NotificationManagerCompat.from(ctx).areNotificationsEnabled();
        int notificationMode = SettingsActivity.showNotificationMode(ctx);

        if (notificationMode != SettingsActivity.NOTIFICATION_MODE_OFF && !notificationsEnabled) {
            // TODO show warning about this here and "touch here to resolve" link opening dialog
            DebugLog.d(TAG, "notifications enabled but system settings disabled them");
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RoameoEvents.ACTION_CALL_DATA_UPDATED);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mDataUpdateBroadcastReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mDataUpdateBroadcastReceiver);
        super.onDestroy();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_summary, container, false);

        TextView startTimeTextView = (TextView) rootView.findViewById(R.id.start_time);
        mSessionCountTextView = (TextView) rootView.findViewById(R.id.session_count);
        mStepCountTextView = (TextView) rootView.findViewById(R.id.total_step_count);
        mDurationTextView = (TextView) rootView.findViewById(R.id.total_duration);
        mAvgStepCountTextView = (TextView) rootView.findViewById(R.id.average_step_count);
        mAvgDurationTextView = (TextView) rootView.findViewById(R.id.average_duration);
        mAvgPaceTextView = (TextView) rootView.findViewById(R.id.average_pace);
        mMaxStepCountTextView = (TextView) rootView.findViewById(R.id.max_step_count);
        mMaxDurationTextView = (TextView) rootView.findViewById(R.id.max_duration);

        CallSession firstSession = CallSession.getFirstTimestamp();
        long timestamp = (firstSession != null) ? firstSession.timestamp : System.currentTimeMillis();
        startTimeTextView.setText(DateFormat.getDateInstance(DateFormat.LONG).format(new Date(timestamp)));

        setData();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        DebugLog.d(TAG, "onResume()");
        // TODO re-check anything that might have changed now
        // returning from AppInfo settings will call this
    }

    private final BroadcastReceiver mDataUpdateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setData();
        }
    };

    private void setData() {
        int sessionCount = CallSession.getSessions().size();
        long totalSteps = CallSession.getTotalSteps();
        long totalDuration = CallSession.getTotalDurations();

        mSessionCountTextView.setText(String.valueOf(sessionCount));
        mStepCountTextView.setText(String.valueOf(totalSteps));
        mDurationTextView.setText(Utils.millisToTimeString(totalDuration));

        long avgSteps = (sessionCount > 0) ? totalSteps / sessionCount : 0;
        long avgDuration = (sessionCount > 0) ? totalDuration / sessionCount : 0;
        mAvgStepCountTextView.setText(String.valueOf(avgSteps));
        mAvgDurationTextView.setText(Utils.millisToTimeString(avgDuration));

        double minutes = Utils.millisToMinutes(avgDuration);
        double avgPace = (minutes > 0) ? avgSteps / minutes : 0;
        mAvgPaceTextView.setText(String.format(Locale.US, "%.2f", avgPace));

        long maxSteps = CallSession.getMaxSteps();
        long maxDuration = CallSession.getMaxDuration();
        mMaxStepCountTextView.setText(String.valueOf(maxSteps));
        mMaxDurationTextView.setText(Utils.millisToTimeString(maxDuration));
    }

    private void putMeInOnClickCallbackLater() {
        /*
         * this works, but uses internal/hidden stuff, therefore hard coded strings. pretty much sucks..
         * http://stackoverflow.com/questions/32366649/any-way-to-link-to-the-android-notification-settings-for-my-app
         *
        Intent intent = new Intent();
        intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
        intent.putExtra("app_package", getContext().getPackageName());
        intent.putExtra("app_uid", getContext().getApplicationInfo().uid);
        startActivity(intent);
         */

        /*
         * this will open system app settings instead
         */
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getContext().getPackageName()));
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        startActivity(intent);
    }
}
