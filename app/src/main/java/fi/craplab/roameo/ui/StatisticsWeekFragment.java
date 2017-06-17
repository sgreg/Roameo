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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lecho.lib.hellocharts.animation.ChartAnimationListener;
import lecho.lib.hellocharts.formatter.LineChartValueFormatter;
import lecho.lib.hellocharts.formatter.SimpleAxisValueFormatter;
import lecho.lib.hellocharts.formatter.ValueFormatterHelper;
import lecho.lib.hellocharts.listener.LineChartOnValueSelectListener;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;
import fi.craplab.roameo.R;
import fi.craplab.roameo.RoameoEvents;
import fi.craplab.roameo.model.CallSession;
import fi.craplab.roameo.util.DebugLog;
import fi.craplab.roameo.util.Utils;

/**
 * Draw statistic graphs about steps, duration and pace for each day of a week and display
 * each day's call sessions.
 */
public class StatisticsWeekFragment extends Fragment {
    private static final String TAG = StatisticsWeekFragment.class.getSimpleName();

    private static final String ARG_WEEK_NUMBER = "week_number";
    private static final String ARG_WEEK_YEAR   = "week_year";

    private static final int MODE_STEPS = 0;
    private static final int MODE_DURATION = 1;
    private static final int MODE_PACE = 2;

    private static final long ANIMATION_DURATION_MS = 75;

    private int mWeekNumber;
    private int mWeekYear;
    private int mDisplayMode = MODE_STEPS;

    private final List<PointValue> mStepValues     = new ArrayList<>();
    private final List<PointValue> mDurationValues = new ArrayList<>();
    private final List<PointValue> mPaceValues     = new ArrayList<>();
    private final List<AxisValue>  mDayNameValues  = new ArrayList<>();
    private final List<AxisValue>  mDateValues     = new ArrayList<>();

    private static final StepValueFormatter sStepValueFormatter = new StepValueFormatter();
    private static final DurationValueFormatter sDurationValueFormatter = new DurationValueFormatter();
    private static final PaceValueFormatter sPaceValueFormatter = new PaceValueFormatter();

    private TextView mInstructionsTextView;
    private TextView mDayDetailsTextView;
    private TextView mNoSessionsTextView;
    private LineChartView mChartView;
    private ListView mListView;

    private StatItemAdapter mItemAdapter;

    public StatisticsWeekFragment() {}

    public static StatisticsWeekFragment newInstance(int weekNumber, int year) {
        StatisticsWeekFragment fragment = new StatisticsWeekFragment();

        Bundle args = new Bundle();
        args.putInt(ARG_WEEK_NUMBER, weekNumber);
        args.putInt(ARG_WEEK_YEAR, year);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DateTime dateTime = DateTime.now();
        mWeekNumber = getArguments().getInt(ARG_WEEK_NUMBER, dateTime.getWeekOfWeekyear());
        mWeekYear   = getArguments().getInt(ARG_WEEK_YEAR, dateTime.getYear());

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RoameoEvents.ACTION_WEEK_START_CHANGED);
        intentFilter.addAction(RoameoEvents.ACTION_CALL_DATA_UPDATED);

        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(mDataUpdateBroadcastReceiver, intentFilter);

        setupData();
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getActivity())
                .unregisterReceiver(mDataUpdateBroadcastReceiver);
        super.onDestroy();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_statistics_week, container, false);

        mChartView = (LineChartView) rootView.findViewById(R.id.week_graph);
        mInstructionsTextView = (TextView) rootView.findViewById(R.id.sessions_instructions);
        mDayDetailsTextView = (TextView) rootView.findViewById(R.id.day_details);
        mNoSessionsTextView = (TextView) rootView.findViewById(R.id.no_sessions_text);

        mItemAdapter = new StatItemAdapter(getContext());
        mListView = (ListView) rootView.findViewById(R.id.session_list);
        mListView.setFocusable(false); // or else ScrollView jumps to first item
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CallSession session = (CallSession) parent.getAdapter().getItem(position);
                Intent intent = new Intent(getActivity(), SessionDetailsActivity.class);
                intent.putExtra(DetailsDataFragment.ARG_SECTION_NUMBER, session.getId());
                startActivity(intent);
            }
        });
        mListView.setAdapter(mItemAdapter);

        TabLayout tabLayout = (TabLayout) rootView.findViewById(R.id.tab_layout);
        // make sure to add tabs in same order as MODE_* values ..yeah, there could be a better way.
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.title_steps)));
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.title_duration)));
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.title_pace)));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mDisplayMode = tab.getPosition();
                updateData();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        mChartView.setLineChartData(getData());
        updateData();

        mChartView.setDataAnimationListener(new ChartAnimationListener() {
            @Override
            public void onAnimationStarted() {
            }

            @Override
            public void onAnimationFinished() {
                adjustViewPort(mChartView);
            }
        });

        mChartView.setOnValueTouchListener(new LineChartOnValueSelectListener() {
            @Override
            public void onValueSelected(int lineIndex, int pointIndex, PointValue pointValue) {
                DebugLog.d(TAG, String.format(Locale.US, "onValueSelected(%d, %d, %s)",
                        lineIndex, pointIndex, pointValue));

                DateTime dateTime = getFirstDayOfWeek().plusDays(pointIndex);

                mInstructionsTextView.setVisibility(View.GONE);
                mDayDetailsTextView.setText(
                        DateFormat.getDateInstance(DateFormat.FULL).format(dateTime.toDate()));
                List<CallSession> sessions = CallSession.getSessionsForDay(dateTime.getMillis());

                mNoSessionsTextView.setVisibility(sessions.size() == 0 ? View.VISIBLE : View.GONE);
                mItemAdapter.updateData(sessions);
                setListViewHeightBasedOnChildren(mListView);
            }

            @Override
            public void onValueDeselected() {
            }
        });

        return rootView;
    }

    private final BroadcastReceiver mDataUpdateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case RoameoEvents.ACTION_WEEK_START_CHANGED:
                    DebugLog.d(TAG, "Week start setting has changed");
                    break;
                case RoameoEvents.ACTION_CALL_DATA_UPDATED:
                    DebugLog.d(TAG, "Call data has updated");
                    break;
            }

            // redraw statistics
            clearData();
            setupData();
            updateData();
        }
    };

    /**
     * Get {@link DateTime} of week's first day at midnight, considering "week starts at" setting.
     *
     * @return {@link DateTime} at first day of week.
     */
    private DateTime getFirstDayOfWeek() {
        return new DateTime()
                .withYear(mWeekYear)
                .withWeekOfWeekyear(mWeekNumber)
                .weekOfWeekyear()
                .roundFloorCopy()
                .plusDays(SettingsActivity.weekStartDayOffset(getContext()));
    }

    private void clearData() {
        mDayNameValues.clear();
        mDateValues.clear();
        mStepValues.clear();
        mDurationValues.clear();
        mPaceValues.clear();
    }

    private void setupData() {
        DateTime dateTime = getFirstDayOfWeek();
        DebugLog.d(TAG, String.format(Locale.US,
                "Setting up data for week %2d %d starting %s",
                mWeekNumber, mWeekYear, dateTime));

        for (int day = 0; day < DateTimeConstants.DAYS_PER_WEEK; day++) {
            DateTime dt = dateTime.plusDays(day);

            long steps = CallSession.getStepsForDay(dt.getMillis());
            float duration = CallSession.getDurationsForDay(dt.getMillis()) / 1000;
            float pace = (duration > 0) ? (steps / (duration / 60f)) : 0.0f;

            mStepValues.add(new PointValue(day, steps));
            mDurationValues.add(new PointValue(day, duration));
            mPaceValues.add(new PointValue(day, pace));

            mDayNameValues.add(day, new AxisValue(day).setLabel(dt.dayOfWeek().getAsShortText()));
            mDateValues.add(day, new AxisValue(day).setLabel(
                    String.format(Locale.US,"%02d/%02d", dt.getDayOfMonth(), dt.getMonthOfYear())));
        }
    }

    private void updateData() {
        LineChartData data = mChartView.getLineChartData();
        List<PointValue> newValues;
        int color;
        LineChartValueFormatter valueFormatter;

        switch (mDisplayMode) {
            case MODE_STEPS:
                newValues = mStepValues;
                color = ContextCompat.getColor(getContext(), R.color.week_stat_steps);
                valueFormatter = sStepValueFormatter;
                break;
            case MODE_DURATION:
                newValues = mDurationValues;
                color = ContextCompat.getColor(getContext(), R.color.week_stat_duration);
                valueFormatter = sDurationValueFormatter;
                break;
            case MODE_PACE:
                newValues = mPaceValues;
                color = ContextCompat.getColor(getContext(), R.color.week_stat_pace);
                valueFormatter = sPaceValueFormatter;
                break;
            default:
                return;
        }

        for (Line line : data.getLines()) {
            for (PointValue value : line.getValues()) {
                value.setTarget(value.getX(), newValues.get((int) value.getX()).getY());
            }
            line.setColor(color);
            line.setFormatter(valueFormatter);
        }

        mChartView.startDataAnimation(ANIMATION_DURATION_MS);

    }

    private void adjustViewPort(LineChartView chart) {
        Viewport viewport = new Viewport(chart.getMaximumViewport());
        if (viewport.top < 1) {
            /*
             * If there's no data at all to display for this week, i.e. all values are zero
             * (or rather 1.4E-45 or whatever due to the nature of float), viewport will be
             * practically non-existing and graph is sorta missing.
             * Adjust viewport to range from 0..10 in this case.
             */
            viewport.top = 10;
            viewport.bottom = 0;
        } else {
            // Add 10% to top and bottom to make sure cubic lines will (in most cases) fit.
            viewport.top *= 1.1;
            viewport.bottom -= (viewport.top / 10.0);
        }
        chart.setMaximumViewport(viewport);
        chart.setCurrentViewport(viewport);
    }

    private LineChartData getData() {
        Line line = new Line();

        List<PointValue> nullValues = new ArrayList<>();
        for (int i = 0; i < DateTimeConstants.DAYS_PER_WEEK; i++) {
            nullValues.add(new PointValue(i, 0));
        }
        line.setValues(nullValues);

        line.setColor(ContextCompat.getColor(getContext(), R.color.week_stat_chart_lines));
        line.setHasPoints(true);
        line.setHasLabels(true);
        line.setFilled(false);
        line.setPointRadius(8);
        line.setStrokeWidth(4);
        line.setCubic(true);

        List<Line> lines = new ArrayList<>();
        lines.add(line);

        LineChartData data = new LineChartData();
        data.setLines(lines);

        Axis dayNameAxis = new Axis();
        dayNameAxis.setName("");
        dayNameAxis.setTextColor(ContextCompat.getColor(getContext(), R.color.week_stat_axes));
        dayNameAxis.setMaxLabelChars(3);
        dayNameAxis.setValues(mDayNameValues);
        dayNameAxis.setHasLines(false);
        dayNameAxis.setHasSeparationLine(false);
        dayNameAxis.setHasTiltedLabels(true);
        data.setAxisXTop(dayNameAxis);

        Axis dateAxis = new Axis();
        dateAxis.setName("");
        dateAxis.setTextColor(ContextCompat.getColor(getContext(), R.color.week_stat_axes));
        dateAxis.setMaxLabelChars(5);
        dateAxis.setValues(mDateValues);
        dateAxis.setHasLines(false);
        dateAxis.setHasSeparationLine(false);
        dateAxis.setHasTiltedLabels(true);
        data.setAxisXBottom(dateAxis);

        Axis valueAxis = new Axis();
        valueAxis.setName("");
        valueAxis.setTextColor(ContextCompat.getColor(getContext(), R.color.week_stat_axes));
        valueAxis.setMaxLabelChars(5);
        valueAxis.setHasSeparationLine(false);
        valueAxis.setHasLines(true);
        valueAxis.setFormatter(new ValueAxisFormatter());
        data.setAxisYLeft(valueAxis);

        return data;
    }

    private class ValueAxisFormatter extends SimpleAxisValueFormatter {
        @Override
        public int formatValueForAutoGeneratedAxis(char[] formattedValue,
                                                   float value,
                                                   int autoDecimalDigits) {

            if (StatisticsWeekFragment.this.mDisplayMode == MODE_DURATION) {
                /*
                 * FIXME this is not ideal for two reasons:
                 *  1.  Separator lines are drawn based on original value in seconds,
                 *      so dividing by 60 will result in odd values
                 *  2.  If week has no values at all, the adjusted viewport values
                 *      ranging 0..10 are also divided resulting in 10 separator
                 *      lines with each on having "0" as value.
                 */
                value = value / 60.0f;
            }
            return super.formatValueForAutoGeneratedAxis(formattedValue, value, autoDecimalDigits);
        }
    }

    private static class StepValueFormatter implements LineChartValueFormatter {
        private final ValueFormatterHelper valueFormatterHelper = new ValueFormatterHelper();

        @Override
        public int formatChartValue(char[] chars, PointValue pointValue) {
            return valueFormatterHelper.formatFloatValue(chars, pointValue.getY(), 0);
        }
    }

    private static class DurationValueFormatter implements LineChartValueFormatter {
        /**
         * Manually create the label for durations.
         *
         * Formatting and helper classes shipped with HelloCharts offer basically only float
         * and integer formatting. Anything else requires manual work, as it's the case here.
         *
         * Now, {@link LineChartValueFormatter#formatChartValue(char[], PointValue)} gives
         * the {code char[]} parameter as buffer to write into, and the {@link PointValue}
         * to read the value from. Main idea is to just copy whatever text you want as label
         * into the {@code char[]} and return the number of characters to read from it.
         *
         * Only pitfall is that {@code chars} is read back to front when rendering the actual
         * label, so writing into it also needs to happen this way.
         *
         * @param chars         Buffer to store the label value given to the renderer
         * @param pointValue    {@link PointValue} with the actual value to format
         * @return Number of characters written into {@code chars}
         */
        @Override
        public int formatChartValue(char[] chars, PointValue pointValue) {
            char[] val = Utils.secondsToTimeString((long) (pointValue.getY())).toCharArray();
            int length = (val.length >= chars.length) ? (chars.length - 1): val.length;

            // NOTE, as mentioned, copy from the back
            System.arraycopy(val, 0, chars, chars.length - length, length);

            return length;
        }
    }

    private static class PaceValueFormatter implements LineChartValueFormatter {
        private final ValueFormatterHelper valueFormatterHelper = new ValueFormatterHelper();

        @Override
        public int formatChartValue(char[] chars, PointValue pointValue) {
            return valueFormatterHelper.formatFloatValue(chars, pointValue.getY(), 2);
        }
    }

    /*
     *
     *
     * http://vardhan-justlikethat.blogspot.fi/2014/04/android-listview-inside-scrollview.html
     * referred to by http://stackoverflow.com/questions/26269009/baseadapter-gets-me-only-the-first-position-in-listview-android
     */
    private void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }
}
