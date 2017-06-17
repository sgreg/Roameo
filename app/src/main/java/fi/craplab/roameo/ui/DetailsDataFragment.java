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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.ColumnChartView;
import fi.craplab.roameo.R;
import fi.craplab.roameo.data.MinuteStepData;
import fi.craplab.roameo.data.SessionCruncher;
import fi.craplab.roameo.model.CallSession;
import fi.craplab.roameo.util.UiUtils;
import fi.craplab.roameo.util.Utils;

/**
 * Call Session details.
 *
 * This is displayed either in {@link DetailsFragment} if opened from the menu,
 * or in {@link SessionDetailsActivity} if either opened from a session detail
 * in {@link StatisticsWeekFragment} or from the call summary notification.
 */
public class DetailsDataFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    public static final String ARG_SECTION_NUMBER = "section_number";

    private static final int MODE_ABSOLUTE = 0;
    private static final int MODE_RELATIVE = 1;

    private ColumnChartView mMinutesGraph;
    private int mChartMode = MODE_ABSOLUTE;
    private float mRelativeBaseValue = 0;

    public DetailsDataFragment() {
    }

    public static DetailsDataFragment newInstance(long databaseId) {
        DetailsDataFragment fragment = new DetailsDataFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_SECTION_NUMBER, databaseId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_details, container, false);

        TextView dateTextView = (TextView) rootView.findViewById(R.id.counter_date);
        TextView numberTextView = (TextView) rootView.findViewById(R.id.counter_number);
        TextView countTextView = (TextView) rootView.findViewById(R.id.counter_value);
        TextView durationTextView = (TextView) rootView.findViewById(R.id.counter_duration);
        TextView paceTextView = (TextView) rootView.findViewById(R.id.counter_pace);
        mMinutesGraph = (ColumnChartView) rootView.findViewById(R.id.minutes_graph);

        long callSessionId = getArguments().getLong(ARG_SECTION_NUMBER);
        CallSession callSession = CallSession.getById(callSessionId);

        if (callSession != null) {
            dateTextView.setText(DateFormat.getTimeInstance(
                    DateFormat.MEDIUM).format(new Date(callSession.timestamp)));
            UiUtils.setPhoneNumber(getContext(), numberTextView, callSession);
            countTextView.setText(String.valueOf(callSession.stepCount));
            durationTextView.setText(Utils.millisToTimeString(callSession.duration));

            float durationMinutes = Utils.millisToMinutes(callSession.duration);
            float pace = (durationMinutes > 0) ? (callSession.stepCount / durationMinutes) : 0.0f;
            paceTextView.setText(String.format(Locale.US, "%.2f", pace));

            createColumnChartData(callSession);
            mMinutesGraph.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ColumnChartData data = mMinutesGraph.getChartData();
                    updateData(data);
                    mMinutesGraph.setColumnChartData(data);
                    mChartMode = (mChartMode == MODE_ABSOLUTE) ? MODE_RELATIVE : MODE_ABSOLUTE;
                }
            });
        }

        return rootView;
    }

    private void createColumnChartData(CallSession callSession) {
        List<Column> columns = new ArrayList<>();
        int color = ContextCompat.getColor(getContext(), R.color.detail_data_steps);

        SessionCruncher sessionCruncher = new SessionCruncher(callSession);
        MinuteStepData minuteStepData = sessionCruncher.getMinuteSteps();

        // Fill column data from minuteStepData
        for (Integer minuteStep : minuteStepData.getMinuteList()) {
            List<SubcolumnValue> values = new ArrayList<>();
            values.add(new SubcolumnValue(minuteStep).setColor(color));
            Column column = new Column(values);
            column.setHasLabelsOnlyForSelected(true);
            columns.add(column);
        }

        ColumnChartData data = new ColumnChartData(columns);

        // TODO add baseline changes on pressing somewhere
            /*
             * sets average steps/minute of all sessions as data base line.
             * would need axis labeling adjustment, or better yet, double axis labels,
             * left side absolute values, right side base line as zero line
              */
        //data.setBaseValue(CallSession.getTotalSteps() / (CallSession.getTotalDurations() / 1000.0f / 60.0f));
            /*
             * same but session average steps/minute base line
             */
        mRelativeBaseValue = minuteStepData.getAvgStep();

        // Create and set axes to ColumnChartData
        Axis xAxis = Axis.generateAxisFromRange(0, minuteStepData.getMinuteCount(), 1);
        xAxis.setName("Minutes");
        xAxis.setTextColor(ContextCompat.getColor(getContext(), R.color.detail_data_chart_lines));

        Axis yAxis = new Axis();
        yAxis.setName("Steps");
        yAxis.setHasLines(true);
        yAxis.setHasSeparationLine(false);
        yAxis.setTextColor(ContextCompat.getColor(getContext(), R.color.detail_data_chart_lines));

        data.setAxisXBottom(xAxis);
        data.setAxisYLeft(yAxis);

        // set all data to graph
        mMinutesGraph.setColumnChartData(data);

        // adjust graph's viewport with some extra space to the top
        // TODO this probably needs also adjustment if baseline changes are used
        Viewport viewport = mMinutesGraph.getMaximumViewport();
        viewport.top = Utils.roundedUpTen((int) viewport.top);
        mMinutesGraph.setMaximumViewport(viewport);
        mMinutesGraph.setCurrentViewport(viewport);
    }

    private void updateData(ColumnChartData data) {
        if (mChartMode == MODE_ABSOLUTE) {
            data.setBaseValue(mRelativeBaseValue);
        } else {
            data.setBaseValue(0);
        }
    }
}
