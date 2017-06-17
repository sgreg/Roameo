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

package fi.craplab.roameo;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.design.widget.NavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.joanzapata.iconify.Icon;
import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.fonts.FontAwesomeIcons;

import org.joda.time.DateTime;
import fi.craplab.roameo.model.CallSession;
import fi.craplab.roameo.sensor.SensorService;
import fi.craplab.roameo.share.GoogleFitClientBuilder;
import fi.craplab.roameo.share.GoogleFitDeleteTask;
import fi.craplab.roameo.ui.CompassActivity;
import fi.craplab.roameo.ui.DetailsFragment;
import fi.craplab.roameo.ui.SettingsActivity;
import fi.craplab.roameo.ui.StatisticsFragment;
import fi.craplab.roameo.ui.SummaryFragment;
import fi.craplab.roameo.ui.view.AboutDialog;
import fi.craplab.roameo.ui.view.DeleteDialog;
import fi.craplab.roameo.ui.view.ExitDialog;
import fi.craplab.roameo.ui.view.ExportDataDialog;
import fi.craplab.roameo.ui.view.ExternalStorageDialog;
import fi.craplab.roameo.ui.view.PermissionDialog;
import fi.craplab.roameo.util.DebugLog;
import fi.craplab.roameo.util.TestDbUtils;
import fi.craplab.roameo.util.Utils;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        ExitDialog.ExitDialogListener, PermissionDialog.PermissionDialogListener,
        DeleteDialog.DeleteDialogListener, ExternalStorageDialog.ExternalStorageDialogListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private NavigationView mNavigationView;
    private SharedPreferences mSharedPrefs;
    private Intent mSensorServiceIntent;
    private SensorService mSensorService;
    private boolean mSensorBound = false;
    private boolean mGoogleFitReceiverRegistered = false;
    private boolean mPendingPermissionRetryDialog = false;
    private boolean mPendingExportDataDialog = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DebugLog.d(TAG, "onCreate()");
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle); // TODO does this need a removeDrawerListener() call?
        toggle.syncState();

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);

        if (BuildConfig.TestFeatures) {
            mNavigationView.inflateMenu(R.menu.activity_main_drawer_testing);
        }

        setNavigationItemIcon(R.id.nav_summary, FontAwesomeIcons.fa_user);
        setNavigationItemIcon(R.id.nav_statistics, FontAwesomeIcons.fa_area_chart);
        setNavigationItemIcon(R.id.nav_details, FontAwesomeIcons.fa_bar_chart_o);

        // set default navigation item (Summary)
        mNavigationView.setCheckedItem(R.id.nav_summary);
        setContentFragment(new SummaryFragment());

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        boolean firstTimeStart = false;
        if (mSharedPrefs.getLong(SettingsActivity.FIRST_START_TSTAMP, -1) == -1) {
            // First start timestamp isn't set, so this is the first time the app is started.
            mSharedPrefs.edit().putLong(
                    SettingsActivity.FIRST_START_TSTAMP, System.currentTimeMillis()).apply();

            // make sure default values are stored to SharedPreferences
            PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

            firstTimeStart = true;
        }

        handlePermissions(firstTimeStart);
    }

    private void initServices() {
        // start sensor service
        mSensorServiceIntent = new Intent(this, SensorService.class);
        startService(mSensorServiceIntent);
        bindService(mSensorServiceIntent, mSensorServiceConnection, Context.BIND_AUTO_CREATE);

        // check if Google Fit is enabled and create client object
        if (mSharedPrefs.getBoolean(SettingsActivity.CONNECT_GOOGLE_FIT, false)) {
            GoogleFitClientBuilder.createApiClient(this);
        }

        // create and register Google Fit enable/disable intent filter
        IntentFilter googleFitSettingsFilter = new IntentFilter();
        googleFitSettingsFilter.addAction(RoameoEvents.ACTION_GOOGLE_FIT_ENABLED);
        googleFitSettingsFilter.addAction(RoameoEvents.ACTION_GOOGLE_FIT_DISABLED);

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mGoogleFitSettingsReceiver, googleFitSettingsFilter);

        mGoogleFitReceiverRegistered = true;

    }

    @Override
    protected void onDestroy() {
        DebugLog.d(TAG, "onDestroy()");
        if (mSensorBound) {
            unbindService(mSensorServiceConnection);
            mSensorBound = false;
        }

        if (mGoogleFitReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mGoogleFitSettingsReceiver);
            mGoogleFitReceiverRegistered = false;
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPendingPermissionRetryDialog) {
            mPendingPermissionRetryDialog = false;
            showPermissionRetryDialog();
        }

        if (mPendingExportDataDialog) {
            mPendingExportDataDialog = false;
            showExportDataDialog();
        }
    }

    private void setContentFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_framelayout, fragment)
                .commit();
    }

    private void setNavigationItemIcon(int resId, Icon icon) {
        MenuItem menuItem = mNavigationView.getMenu().findItem(resId);
        if (menuItem != null) {
            menuItem.setIcon(new IconDrawable(this, icon).color(Color.GRAY));
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_summary) {
            setTitle(getString(R.string.app_name));
            setContentFragment(new SummaryFragment());

        } else if (id == R.id.nav_statistics) {
            setContentFragment(new StatisticsFragment());

        } else if (id == R.id.nav_details) {
            setContentFragment(new DetailsFragment());

        } else if (id == R.id.nav_export) {
            if (!Utils.hasRuntimePermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ExternalStorageDialog dialog = new ExternalStorageDialog();
                dialog.show(getSupportFragmentManager(), "externalStorageDialog");
            } else {
                showExportDataDialog();
            }

        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));

        } else if (id == R.id.nav_about) {
            AboutDialog dialog = new AboutDialog();
            dialog.show(getSupportFragmentManager(), "aboutDialog");

        } else if (id == R.id.nav_exit) {
            ExitDialog dialog = new ExitDialog();
            dialog.show(getSupportFragmentManager(), "exitDialog");

        } else if (BuildConfig.TestFeatures) {
            if (id == R.id.testing_start_recording) {
                if (mSensorBound) {
                    mSensorService.testingStartCounter();
                    Toast.makeText(this, "Counting started", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Sensor service not bound!", Toast.LENGTH_SHORT).show();
                    DebugLog.e(TAG, "Sensor service not bound!");
                }

            } else if (id == R.id.testing_stop_recording) {
                if (mSensorBound) {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mSensorService.testingStopCounter();
                        }
                    }, 2000);
                    //mSensorService.testingStopCounter();
                    Toast.makeText(this, "Counting stopped", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Sensor service not bound!", Toast.LENGTH_SHORT).show();
                    DebugLog.e(TAG, "Sensor service not bound!");
                }

            } else if (id == R.id.testing_dump_recording) {
                if (mSensorBound) {
                    mSensorService.testingDumpLogCallSessions();
                    Toast.makeText(this, "Dumped", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Sensor service not bound!", Toast.LENGTH_SHORT).show();
                    DebugLog.e(TAG, "Sensor service not bound!");
                }
            } else if (id == R.id.testing_create_random) {
                CallSession callSession = TestDbUtils.createRandomCallSession(this);
                Toast.makeText(this, "Random session created for "
                        + new DateTime(callSession.timestamp), Toast.LENGTH_SHORT).show();

            } else if (id == R.id.testing_start_movement_detection) {
                //Toast.makeText(this, "Gathering movement", Toast.LENGTH_SHORT).show();
                //mMovementGatherer.startGathering();
                startActivity(new Intent(this, CompassActivity.class));

            } else if (id == R.id.testing_stop_movement_detection) {
                Toast.makeText(this, "Stop gathering movement", Toast.LENGTH_SHORT).show();
                // XXX used to be MovementGatherer, that's deleted. Start over with it.
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private final ServiceConnection mSensorServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DebugLog.d(TAG, "onServiceConnected()");
            SensorService.SensorBinder binder = (SensorService.SensorBinder) service;
            mSensorService = binder.getService();

            mSensorBound = true;
            // TODO check if sensor is available and show warning otherwise
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mSensorBound = false;
        }
    };

    @Override
    public void onConfirmExit() {
        DebugLog.d(TAG, "Rosebud.");
        stopService(mSensorServiceIntent);
        finish();
    }

    private final BroadcastReceiver mGoogleFitSettingsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(RoameoEvents.ACTION_GOOGLE_FIT_ENABLED)) {
                DebugLog.d(TAG, "Google Fit was enabled");
                GoogleFitClientBuilder.createApiClient(MainActivity.this);
            } else if (intent.getAction().equals(RoameoEvents.ACTION_GOOGLE_FIT_DISABLED)) {
                DebugLog.d(TAG, "Google Fit was disabled");
                GoogleFitClientBuilder.destroyApiClient();
            }
        }
    };

    private void handlePermissions(boolean firstTimeStart) {
        int phoneStatePermission = ActivityCompat.checkSelfPermission(
                this, Manifest.permission.READ_PHONE_STATE);

        boolean permissionsGranted = phoneStatePermission == PackageManager.PERMISSION_GRANTED;
        DebugLog.i(TAG, "READ_PHONE_STATE permission granted: " + permissionsGranted);

        if (!permissionsGranted || firstTimeStart) {
            PermissionDialog dialog = PermissionDialog.newInstance(firstTimeStart);
            dialog.show(getSupportFragmentManager(), "permissionDialog");
        } else {
            initServices();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == RoameoApplication.PERMISSION_REQUEST_READ_PHONE_STATE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                DebugLog.d(TAG, "yay, permission");
                initServices();
            } else {
                /*
                 * Cannot show dialog directly or IllegalStateException is thrown.
                 * Instead, set flag that is checked in onResumeFragments() and show the dialog.
                 */
                mPendingPermissionRetryDialog = true;
            }
        } else if (requestCode == RoameoApplication.PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // again, cannot show dialog directly, so set flag
                mPendingExportDataDialog = true;
            }
        }
    }

    private void showPermissionRetryDialog() {
        PermissionDialog dialog = PermissionDialog.retryInstance();
        dialog.show(getSupportFragmentManager(), "permissionRetryDialog");
    }

    @Override
    public void onPermissionDialogOkay() {
        ActivityCompat.requestPermissions(
                this,
                new String[] {Manifest.permission.READ_PHONE_STATE},
                RoameoApplication.PERMISSION_REQUEST_READ_PHONE_STATE);
    }

    @Override
    public void onPermissionDialogExit() {
        finish();
    }

    @Override
    public void onConfirmDelete(long callSessionId, boolean deleteFromGoogleFit) {
        /*
         * Since DetailFragment is called from within here,
         * onConfirmDelete needs to be implemented here as well.
         */
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
        /*
         * Force DetailsFragment reload.
         * This works, but is probably not the most efficient way. Also, forcing reload
         * will jump to the first element, that might turn out annoying.
         */
        setContentFragment(new DetailsFragment());
    }

    @Override
    public void onPermissionContinue() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                RoameoApplication.PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
    }

    private void showExportDataDialog() {
        ExportDataDialog dialog = new ExportDataDialog();
        dialog.show(getSupportFragmentManager(), "exportDataDialog");
    }
}
