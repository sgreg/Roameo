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
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.api.GoogleApiClient;

import fi.craplab.roameo.R;
import fi.craplab.roameo.RoameoEvents;
import fi.craplab.roameo.model.CallSession;
import fi.craplab.roameo.share.GoogleFitClientBuilder;
import fi.craplab.roameo.share.GoogleFitDeleteTask;
import fi.craplab.roameo.share.GoogleFitUploadTask;
import fi.craplab.roameo.ui.view.DeleteDialog;
import fi.craplab.roameo.util.DebugLog;
import fi.craplab.roameo.util.ShareUtils;

import java.text.DateFormat;
import java.util.Date;

/**
 *
 */
public class SessionDetailsActivity extends AppCompatActivity
        implements DeleteDialog.DeleteDialogListener {

    private static final String TAG = SessionDetailsActivity.class.getSimpleName();

    private CallSession mCallSession;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        long dbId = intent.getLongExtra(DetailsDataFragment.ARG_SECTION_NUMBER, 1);

        DetailsDataFragment fragment = DetailsDataFragment.newInstance(dbId);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commit();

        mCallSession = CallSession.getById(dbId);
        setTitle(DateFormat.getDateInstance(DateFormat.LONG).format(new Date(mCallSession.timestamp)));

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RoameoEvents.ACTION_GOOGLE_FIT_DATA_UPLOADED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mGoogleFitReceiver, intentFilter);
    }

    // FIXME this is all duplicated from DetailsFragment
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_details, menu);
        MenuItem gFitShare = menu.findItem(R.id.action_upload_google_fit);

        gFitShare.setVisible(SettingsActivity.isGoogleFitEnabled(this));
        gFitShare.setEnabled(mCallSession.googleFitIdentifier == null);
        return true;
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mGoogleFitReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_upload_google_fit) {
            GoogleApiClient gFitClient = GoogleFitClientBuilder.getApiClient();

            if (gFitClient != null && mCallSession != null) {
                GoogleFitUploadTask uploadTask = new GoogleFitUploadTask(gFitClient, mCallSession);
                uploadTask.execute();
            }
            return true;

        } else if (id == R.id.action_share) {
            Intent shareIntent = ShareUtils.getShareIntent(this, mCallSession);
            startActivity(Intent.createChooser(shareIntent, "Roameo"));
            return true;

        } else if (id == R.id.action_delete) {
            DeleteDialog dialog = DeleteDialog.newInstance(mCallSession.getId());
            dialog.show(getSupportFragmentManager(), "deleteDialog");
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfirmDelete(long callSessionId, boolean deleteFromGoogleFit) {
        DebugLog.i(TAG, "Deleting CallSession with id " + callSessionId);

        GoogleApiClient gFitClient = GoogleFitClientBuilder.getApiClient();
        if (gFitClient != null && deleteFromGoogleFit) {
            CallSession session = CallSession.getById(callSessionId);
            if (session != null) {
                GoogleFitDeleteTask deleteTask = new GoogleFitDeleteTask(gFitClient, session);
                deleteTask.execute();
            }
        }

        CallSession.delete(CallSession.class, callSessionId);
        RoameoEvents.send(this, RoameoEvents.ACTION_CALL_DATA_UPDATED);
        finish();
    }

    private final BroadcastReceiver mGoogleFitReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mCallSession = CallSession.getById(mCallSession.getId());
            invalidateOptionsMenu();
        }
    };
}
