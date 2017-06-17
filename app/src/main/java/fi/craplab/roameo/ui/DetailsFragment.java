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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.api.GoogleApiClient;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import fi.craplab.roameo.R;
import fi.craplab.roameo.RoameoEvents;
import fi.craplab.roameo.model.CallSession;
import fi.craplab.roameo.share.GoogleFitClientBuilder;
import fi.craplab.roameo.share.GoogleFitUploadTask;
import fi.craplab.roameo.ui.view.DeleteDialog;
import fi.craplab.roameo.util.DebugLog;
import fi.craplab.roameo.util.ShareUtils;

/**
 * {@link DetailsDataFragment} (call session) container accessed through menu.
 */
/* FIXME all Fragments are created at once, this will suck on performance sooner or later */
public class DetailsFragment extends Fragment implements ViewPager.OnPageChangeListener {
    private static final String TAG = DetailsFragment.class.getSimpleName();

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RoameoEvents.ACTION_GOOGLE_FIT_ENABLED);
        intentFilter.addAction(RoameoEvents.ACTION_GOOGLE_FIT_DISABLED);
        intentFilter.addAction(RoameoEvents.ACTION_GOOGLE_FIT_DATA_UPLOADED);
        intentFilter.addAction(RoameoEvents.ACTION_GOOGLE_FIT_DATA_DELETED);
        LocalBroadcastManager.getInstance(getContext())
                .registerReceiver(mGoogleFitReceiver, intentFilter);

        // TODO add separate IntentFilter for ACTION_CALL_DATA_UPDATED and re-create everything
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_details_container, container, false);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());

        mViewPager = (ViewPager) rootView.findViewById(R.id.details_view_pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(this);

        if (mSectionsPagerAdapter.getCount() == 0) {
            DebugLog.d(TAG, "NOPE!");
            getActivity().setTitle(getString(R.string.no_sessions_recorded));
        } else {
            getActivity().setTitle(mSectionsPagerAdapter.getPageTitle(0));
        }

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mViewPager.removeOnPageChangeListener(this);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mGoogleFitReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //super.onCreateOptionsMenu(menu, inflater);

        DebugLog.d(TAG, "creating option menu");
        if (mSectionsPagerAdapter.getCount() == 0) {
            return;
        }

        CallSession callSession = mSectionsPagerAdapter.getCallSession(mViewPager.getCurrentItem());

        inflater.inflate(R.menu.menu_details, menu);
        MenuItem gFitShare = menu.findItem(R.id.action_upload_google_fit);

        gFitShare.setVisible(SettingsActivity.isGoogleFitEnabled(getContext()));
        gFitShare.setEnabled(callSession.googleFitIdentifier == null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        CallSession callSession = mSectionsPagerAdapter.getCallSession(mViewPager.getCurrentItem());

        if (id == R.id.action_upload_google_fit) {
            GoogleApiClient gFitClient = GoogleFitClientBuilder.getApiClient();

            if (gFitClient != null && callSession != null) {
                GoogleFitUploadTask uploadTask = new GoogleFitUploadTask(gFitClient, callSession);
                uploadTask.execute();
            }
            return true;

        } else if (id == R.id.action_share) {
            Intent shareIntent = ShareUtils.getShareIntent(getContext(), callSession);
            startActivity(Intent.createChooser(shareIntent, "Roameo"));
            return true;

        } else if (id == R.id.action_delete) {
            DeleteDialog dialog = DeleteDialog.newInstance(callSession.getId());
            dialog.show(getActivity().getSupportFragmentManager(), "deleteDialog");
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // force re-creating options menu
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onPageSelected(int position) {
        getActivity().setTitle(mSectionsPagerAdapter.getPageTitle(position));
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }


    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private final List<CallSession> mSessions;

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            mSessions = CallSession.getSessionsReverse();
        }

        @Override
        public Fragment getItem(int position) {
            DebugLog.d(TAG, "getItem() " + position);
            return DetailsDataFragment.newInstance(mSessions.get(position).getId());
        }

        @Override
        public int getCount() {
            DebugLog.d(TAG, "getCount() returning " + mSessions.size());
            return mSessions.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            DebugLog.d(TAG, "getPageTitle() " + position);
            if (mSessions.size() > 0) {
                return DateFormat.getDateInstance(DateFormat.LONG).format(
                        new Date(mSessions.get(position).timestamp));
            }
            return "";
        }

        CallSession getCallSession(int position) {
            return (position < mSessions.size()) ? mSessions.get(position) : null;
        }
    }

    private final BroadcastReceiver mGoogleFitReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getActivity().invalidateOptionsMenu();
        }
    };
}
