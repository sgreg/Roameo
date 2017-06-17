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

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import fi.craplab.roameo.BuildConfig;
import fi.craplab.roameo.R;
import fi.craplab.roameo.RoameoApplication;
import fi.craplab.roameo.RoameoEvents;
import fi.craplab.roameo.ui.view.ReadContactsDialog;
import fi.craplab.roameo.util.DebugLog;
import fi.craplab.roameo.util.Utils;

import java.util.Locale;

/**
 *
 */
// TODO lots of here should probably go in a bit more appropriate class name, like simply "Settings"
@SuppressWarnings({"WeakerAccess", "unused"})
public class SettingsActivity extends AppCompatActivity
        implements ReadContactsDialog.ReadContactsDialogListener {
    private static final String TAG = SettingsActivity.class.getSimpleName();
    private static final String FRAGMENT_TAG = SettingsFragment.class.getSimpleName();

    public static final String WEEK_STARTS_AT_DAY = "weekStartDay";
    public static final String CONNECT_GOOGLE_FIT = "connectGoogleFit";
    public static final String CALCULATE_DISTANCE = "calculateDistance";
    public static final String DISTANCE_STEP_SIZE = "distanceStepSize";
    public static final String SHOW_NOTIFICATIONS = "showNotifications";
    public static final String KEEP_NOTIFICATIONS = "keepNotifications";
    public static final String STORE_PHONE_NUMBER = "storePhoneNumber";
    public static final String LOOKUP_CALLCONTACT = "lookupContacts";
    public static final String STORE_EMPTY_COUNTS = "storeEmptyCounts";
    public static final String FIRST_START_TSTAMP = "firstStartTimestamp";

    public static final int NOTIFICATION_MODE_OFF = 1;
    public static final int NOTIFICATION_MODE_SUMMARY = 2;
    public static final int NOTIFICATION_MODE_REAL_TIME = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SettingsFragment(), FRAGMENT_TAG)
                .commit();
    }

    @Override
    public void onPermissionContinue() {
        ActivityCompat.requestPermissions(
                this,
                new String[] {Manifest.permission.READ_CONTACTS},
                RoameoApplication.PERMISSION_REQUEST_READ_CONTACTS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == RoameoApplication.PERMISSION_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // set shared pref value true
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                prefs.edit().putBoolean(LOOKUP_CALLCONTACT, true).apply();
                // force refresh by detaching and re-attaching fragment
                Fragment fragment = getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
                getFragmentManager().beginTransaction().detach(fragment).attach(fragment).commit();

                // TODO this could send a broadcast to refresh data views?
            }
        }
    }

    public static class SettingsFragment extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        private Preference mDistanceFootSizePref;
        private boolean mDistanceWasEnabled = false;

        private Preference mKeepNotificationPref;
        private CheckBoxPreference mLookupContactsPref;

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

            if (!BuildConfig.TestFeatures) {
                PreferenceCategory category = (PreferenceCategory) findPreference("distanceSettings");
                if (category != null) {
                    getPreferenceScreen().removePreference(category);
                }

            } else {
                mDistanceFootSizePref = findPreference(DISTANCE_STEP_SIZE);
                mDistanceWasEnabled = prefs.getBoolean(CALCULATE_DISTANCE, false);
                mDistanceFootSizePref.setEnabled(mDistanceWasEnabled);
                if (mDistanceWasEnabled) {
                    setFootSize(prefs, false);
                }
            }

            mKeepNotificationPref = findPreference(KEEP_NOTIFICATIONS);
            int notificationMode = Integer.parseInt(prefs.getString(SHOW_NOTIFICATIONS, "0"));
            mKeepNotificationPref.setEnabled(notificationMode != NOTIFICATION_MODE_OFF);

            mLookupContactsPref = (CheckBoxPreference) findPreference(LOOKUP_CALLCONTACT);
            boolean numberSetting = prefs.getBoolean(STORE_PHONE_NUMBER, false);
            boolean lookupContactsSetting = prefs.getBoolean(LOOKUP_CALLCONTACT, false);
            mLookupContactsPref.setChecked(lookupContactsSetting && readContactsPermitted());
            mLookupContactsPref.setEnabled(numberSetting);

            return super.onCreateView(inflater, container, savedInstanceState);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            DebugLog.d(TAG, "onSharedPreferenceChanged " + key);
            switch (key) {
                case WEEK_STARTS_AT_DAY:
                    RoameoEvents.send(getActivity(), RoameoEvents.ACTION_WEEK_START_CHANGED);
                    break;

                case CONNECT_GOOGLE_FIT:
                    if (sharedPreferences.getBoolean(key, false)) {
                        RoameoEvents.send(getActivity(), RoameoEvents.ACTION_GOOGLE_FIT_ENABLED);
                        //GoogleFitClientBuilder.createApiClient(getActivity(), this).connect();
                    } else {
                        RoameoEvents.send(getActivity(), RoameoEvents.ACTION_GOOGLE_FIT_DISABLED);
                    }
                    break;

                case STORE_EMPTY_COUNTS:
                    break;
                case SHOW_NOTIFICATIONS:
                    int notificationMode = Integer.parseInt(
                            sharedPreferences.getString(SHOW_NOTIFICATIONS, "0"));
                    mKeepNotificationPref.setEnabled(notificationMode != NOTIFICATION_MODE_OFF);
                    break;

                case CALCULATE_DISTANCE:
                    if (sharedPreferences.getBoolean(key, false)) {
                        mDistanceFootSizePref.setEnabled(true);
                        setFootSize(sharedPreferences, !mDistanceWasEnabled);
                    } else {
                        mDistanceWasEnabled = true;
                        mDistanceFootSizePref.setEnabled(false);
                    }
                    break;

                case DISTANCE_STEP_SIZE:
                    setFootSize(sharedPreferences, false);
                    break;

                case STORE_PHONE_NUMBER:
                    boolean numberSetting = sharedPreferences.getBoolean(STORE_PHONE_NUMBER, false);
                    mLookupContactsPref.setEnabled(numberSetting);
                    break;

                case LOOKUP_CALLCONTACT:
                    if (sharedPreferences.getBoolean(key, false)) {
                        if (!readContactsPermitted()) {
                            sharedPreferences.edit().putBoolean(key, false).apply();
                            mLookupContactsPref.setChecked(false);
                            ReadContactsDialog dialog = new ReadContactsDialog();
                            dialog.show(
                                    ((AppCompatActivity) getActivity()).getSupportFragmentManager(),
                                    "readContactsDialog");
                        }
                    }
                    break;
            }
        }

        private void setFootSize(SharedPreferences sharedPreferences, boolean justEnabled) {
            int footSizeValue = sharedPreferences.getInt(DISTANCE_STEP_SIZE, 0);
            if (justEnabled) {
                mDistanceFootSizePref.setSummary(String.format(Locale.US, "%s (%d cm)",
                        mDistanceFootSizePref.getSummary(), footSizeValue));
            } else {
                mDistanceFootSizePref.setSummary(String.format(Locale.US, "%d cm", footSizeValue));
            }
        }

        private boolean readContactsPermitted() {
            return Utils.hasRuntimePermission(getActivity(), Manifest.permission.READ_CONTACTS);
        }
    }

    private static SharedPreferences getPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
    public static int showNotificationMode(Context context) {
        // TODO notifications might be disabled, consider this (tell user, ask to enable or whatever)
        return Integer.parseInt(getPrefs(context).getString(SHOW_NOTIFICATIONS, "0"));
    }

    public static boolean keepNotifications(Context context) {
        return getPrefs(context).getBoolean(KEEP_NOTIFICATIONS, false);
    }

    /**
     * Return if contact name should and can be used instead of phone number
     * @param context Context
     * @return true if lookup setting is set and {@code READ_CONTACTS} permission is granted,
     *         false otherwise
     */
    public static boolean mapNumberToContact(Context context) {
        int phoneStatePermission = ActivityCompat.checkSelfPermission(
                context, Manifest.permission.READ_CONTACTS);

        return getPrefs(context).getBoolean(LOOKUP_CALLCONTACT, false)
                && (phoneStatePermission == PackageManager.PERMISSION_GRANTED);
    }

    /**
     * Get the week start offset in number of days from JodaTime start of week day (i.e Monday)
     *
     * @param context Context
     * @return Week start day offset from Monday
     */
    public static int weekStartDayOffset(Context context) {
        return Integer.parseInt(getPrefs(context).getString(WEEK_STARTS_AT_DAY, "0"));
    }

    public static boolean isGoogleFitEnabled(Context context) {
        return getPrefs(context).getBoolean(CONNECT_GOOGLE_FIT, false);
    }
}