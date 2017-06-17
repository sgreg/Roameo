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

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.fonts.SimpleLineIconsIcons;

import org.joda.time.DateTime;

import fi.craplab.roameo.R;
import fi.craplab.roameo.model.CallSession;
import fi.craplab.roameo.ui.view.StatisticsDialog;

/**
 * Weekly statistics main fragment.
 *
 * Displays {@link StatisticsWeekFragment}s in its internal {@link ViewPager}.
 */
public class StatisticsFragment extends Fragment  implements ViewPager.OnPageChangeListener {
    private static final int MAX_FRAGMENTS_KEEP = 6;

    private ViewPager mViewPager;
    private StatisticsPagerAdapter mPagerAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_statistics, container, false);

        mViewPager = (ViewPager) rootView.findViewById(R.id.statistics_view_pager);
        mPagerAdapter = new StatisticsPagerAdapter(getChildFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.addOnPageChangeListener(this);
        mViewPager.setOffscreenPageLimit(MAX_FRAGMENTS_KEEP);

        getActivity().setTitle(mPagerAdapter.getPageTitle(0));
        mViewPager.setCurrentItem(mPagerAdapter.getCount() - 1);

        setHasOptionsMenu(true);

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mViewPager.removeOnPageChangeListener(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_statistics, menu);
        menu.findItem(R.id.action_info).setIcon(
                new IconDrawable(getActivity(), SimpleLineIconsIcons.icon_info)
                        .color(Color.WHITE)
                        .actionBarSize());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_info) {
            //mPagerAdapter.currentWeek
            WeekYear weekYear = mPagerAdapter.getWeekYearFromPosition(mViewPager.getCurrentItem());
            StatisticsDialog dialog = StatisticsDialog.newInstance(weekYear.week, weekYear.year);
            dialog.show(getActivity().getSupportFragmentManager(), "statisticsDialog");
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        getActivity().setTitle(mPagerAdapter.getPageTitle(position));
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    private class WeekYear {
        int week;
        int year;

        WeekYear(int week, int year) {
            this.week = week;
            this.year = year;
        }
    }

    public class StatisticsPagerAdapter extends FragmentPagerAdapter {

        private static final int WEEKS_PER_YEAR = 52; // yeah, 2020 will have 53, FIXME by then..

        private final int currentWeek;
        private final int firstWeek;
        private final int firstYear;

        StatisticsPagerAdapter(FragmentManager fm) {
            super(fm);

            CallSession callSession = CallSession.getFirstTimestamp();
            DateTime dtSession = (callSession != null)
                    ? new DateTime(callSession.timestamp)
                    : null;

            DateTime dtNow = DateTime.now();

            firstYear   = (dtSession != null) ? dtSession.getYear() : dtNow.getYear();
            currentWeek = dtNow.getWeekOfWeekyear() + getYearWeekOffset(dtNow.getYear());
            firstWeek   = (dtSession != null) ? dtSession.getWeekOfWeekyear() : currentWeek;
        }

        private WeekYear getWeekYearFromPosition(int position) {
            int absoluteWeek = firstWeek + position;
            int year = getYearByAbsoluteWeek(absoluteWeek);
            int week = absoluteWeek - getYearWeekOffset(year);

            return new WeekYear(week, year);
        }

        @Override
        public Fragment getItem(int position) {
            WeekYear weekYear = getWeekYearFromPosition(position);
            return StatisticsWeekFragment.newInstance(weekYear.week, weekYear.year);
        }

        @Override
        public int getCount() {
            return currentWeek - firstWeek + 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            WeekYear weekYear = getWeekYearFromPosition(position);
            return getString(R.string.stats_week_title, weekYear.week, weekYear.year);
        }

        private int getYearWeekOffset(int year) {
            int offset = year - firstYear;
            return (offset > 0) ? offset * WEEKS_PER_YEAR : 0;
        }

        private int getYearByAbsoluteWeek(int week) {
            return firstYear + ((week - 1) / WEEKS_PER_YEAR);
        }
    }
}
