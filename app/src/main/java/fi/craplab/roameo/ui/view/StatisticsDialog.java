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

package fi.craplab.roameo.ui.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.TextView;

import org.joda.time.DateTime;

import java.util.Locale;

import fi.craplab.roameo.R;
import fi.craplab.roameo.model.CallSession;
import fi.craplab.roameo.ui.SettingsActivity;
import fi.craplab.roameo.util.Utils;

/**
 *
 */
public class StatisticsDialog extends DialogFragment {
    private static final String ARG_WEEK_NUMBER = "week_number";
    private static final String ARG_WEEK_YEAR   = "week_year";

    private int mWeekNumber;
    private int mWeekYear;

    private TextView mSessionCountTextView;
    private TextView mStepCountTextView;
    private TextView mDurationTextView;
    private TextView mAvgStepCountTextView;
    private TextView mAvgDurationTextView;
    private TextView mAvgPaceTextView;
    private TextView mMaxStepCountTextView;
    private TextView mMaxDurationTextView;

    public static StatisticsDialog newInstance(int week, int year) {
        StatisticsDialog dialog = new StatisticsDialog();

        Bundle args = new Bundle();
        args.putInt(ARG_WEEK_NUMBER, week);
        args.putInt(ARG_WEEK_YEAR, year);
        dialog.setArguments(args);

        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        DateTime dateTime = DateTime.now();
        mWeekNumber = getArguments().getInt(ARG_WEEK_NUMBER, dateTime.getWeekOfWeekyear());
        mWeekYear   = getArguments().getInt(ARG_WEEK_YEAR, dateTime.getYear());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View rootView = getActivity().getLayoutInflater().inflate(R.layout.dialog_statistics, null);

        mSessionCountTextView = (TextView) rootView.findViewById(R.id.session_count);
        mStepCountTextView = (TextView) rootView.findViewById(R.id.total_step_count);
        mDurationTextView = (TextView) rootView.findViewById(R.id.total_duration);
        mAvgStepCountTextView = (TextView) rootView.findViewById(R.id.average_step_count);
        mAvgDurationTextView = (TextView) rootView.findViewById(R.id.average_duration);
        mAvgPaceTextView = (TextView) rootView.findViewById(R.id.average_pace);
        mMaxStepCountTextView = (TextView) rootView.findViewById(R.id.max_step_count);
        mMaxDurationTextView = (TextView) rootView.findViewById(R.id.max_duration);

        setData();
        builder.setNegativeButton(getString(R.string.action_close), null);
        builder.setView(rootView);

        Dialog dialog = builder.create();
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    private void setData() {
        long weekStartTimestamp = new DateTime()
                .withYear(mWeekYear)
                .withWeekOfWeekyear(mWeekNumber)
                .weekOfWeekyear()
                .roundFloorCopy()
                .plusDays(SettingsActivity.weekStartDayOffset(getContext()))
                .getMillis();

        int sessionCount = CallSession.getSessionsForWeek(weekStartTimestamp).size();
        long totalSteps = CallSession.getStepsForWeek(weekStartTimestamp);
        long totalDuration = CallSession.getDurationsForWeek(weekStartTimestamp);

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

        long maxSteps = CallSession.getMaxStepsForWeek(weekStartTimestamp);
        long maxDuration = CallSession.getMaxDurationForWeek(weekStartTimestamp);
        mMaxStepCountTextView.setText(String.valueOf(maxSteps));
        mMaxDurationTextView.setText(Utils.millisToTimeString(maxDuration));
    }
}
