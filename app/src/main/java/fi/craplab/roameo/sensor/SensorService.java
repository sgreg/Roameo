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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.SparseIntArray;

import java.util.List;
import java.util.Locale;

import fi.craplab.roameo.RoameoEvents;
import fi.craplab.roameo.model.CallSession;
import fi.craplab.roameo.model.MinuteSteps;
import fi.craplab.roameo.ui.SettingsActivity;
import fi.craplab.roameo.util.DebugLog;

/**
 * Sensor service to handle step count sensors during ongoing calls.
 *
 * NOTE: Sensor.TYPE_STEP_COUNTER requires min API 19
 */
/*
 * TODO consider splitting sensor and phone call handling in separate files?
 */
public class SensorService extends Service implements SensorEventListener {
    private static final String TAG = SensorService.class.getSimpleName();

    private SensorManager mSensorManager;
    private Sensor mStepCounter;

    private PowerManager.WakeLock mWakeLock;

    private boolean mCountStarted = false;
    private int mCounterOffset = -1;
    private int mLastCounterValue = -1;
    private long mStartTime = 0;
    private boolean mCallIncoming = false;
    private CallSession mCallSession;

    private final SparseIntArray mTimestampList = new SparseIntArray();
    private SharedPreferences mSharedPrefs;

    public SensorService() {
    }

    @Override
    public void onCreate() {
        DebugLog.d(TAG, "onCreate()");
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        setupStepCounter();
        setupTelephonyHandler();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RoameoSensor");
    }

    @Override
    public IBinder onBind(Intent intent) {
        DebugLog.d(TAG, "onBind()");
        return new SensorBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        DebugLog.d(TAG, "onStartCommand()");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        DebugLog.d(TAG, "onDestroy()");
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }

        TelephonyManager telMan = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telMan.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);

        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    private void setupStepCounter() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mStepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (mStepCounter == null) {
            // TODO without step counter, this app is useless. Notify user about that.
            DebugLog.e(TAG, "Cannot get step counter sensor");
        }
    }

    private void setupTelephonyHandler() {
        TelephonyManager telMan = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telMan.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            if (!mCountStarted) {
                DebugLog.w(TAG, "step counter sensor event, but counting hasn't started");
                return;
            }

            int counterValue = (int) event.values[0];

            if (counterValue == mLastCounterValue) {
                // ignore multiple reports of same counter value (might happen every now and then)
                return;
            }

            if (mCounterOffset == -1) {
                /*
                 * First step count event in this session.
                 * This is triggered after registering the sensor listener and contains the
                 * previous step count information. Using the step count value to initialize
                 * this session's counter offset and return - this is invalid data otherwise.
                 */
                mCounterOffset = counterValue;
                mLastCounterValue = counterValue;
                return;
            }

            int relativeValue = counterValue - mCounterOffset;
            long timestamp = System.currentTimeMillis() - mCallSession.timestamp;
            DebugLog.d(TAG, String.format(Locale.US, "%8d  %5d  %3d  delta %d",
                    timestamp, counterValue, relativeValue, (counterValue - mLastCounterValue)));

            mTimestampList.append((int) timestamp, counterValue - mLastCounterValue);

            mLastCounterValue = counterValue;

            int notifyMode = SettingsActivity.showNotificationMode(this);
            if (relativeValue > 0 && notifyMode == SettingsActivity.NOTIFICATION_MODE_REAL_TIME) {
                SensorNotification.notifyOngoing(this, mStartTime, relativeValue);
            } else if (relativeValue <= 0){
                // XXX this shouldn't happen anymore[TM]
                DebugLog.w(TAG, "relativeValue is " + relativeValue);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // required by SensorEventListener, but we don't have use for this with our step counter.
    }

    public boolean hasSensor() {
        return mStepCounter != null;
    }

    private void startCounting() {
        mWakeLock.acquire();
        // register step counter sensor listener
        mSensorManager.registerListener(this, mStepCounter, SensorManager.SENSOR_DELAY_FASTEST);

        // set new notification id or clear previous one, depending on settings.
        if (SettingsActivity.showNotificationMode(this) != SettingsActivity.NOTIFICATION_MODE_OFF) {
            if (SettingsActivity.keepNotifications(this)) {
                SensorNotification.setId((int) (System.currentTimeMillis() / 1000));
            } else {
                SensorNotification.cancel(this);
            }
        }

        // clear and reset all internal data
        mTimestampList.clear();
        mCallSession = new CallSession();
        mCallSession.timestamp = System.currentTimeMillis();
        mCounterOffset = -1;
        mLastCounterValue = -1;
        mStartTime = mCallSession.timestamp;
        mCountStarted = true;
    }

    private void stopCounting() {
        if (mCountStarted) {
            mSensorManager.unregisterListener(this);
            if (saveCallSession()) {
                saveTimestampList();
                RoameoEvents.send(this, RoameoEvents.ACTION_CALL_DATA_UPDATED);

                if (SettingsActivity.showNotificationMode(this)
                        != SettingsActivity.NOTIFICATION_MODE_OFF) {
                    SensorNotification.notifyFinal(this, mCallSession);
                }
            }
        }
        mCallIncoming = false;
        mCountStarted = false;
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    private boolean saveCallSession() {
        boolean storeSession = true;
        long stepCount = mLastCounterValue - mCounterOffset;

        if (stepCount == 0) {
            // Check if empty counts should be stored.
            storeSession = mSharedPrefs.getBoolean(SettingsActivity.STORE_EMPTY_COUNTS, false);
            DebugLog.d(TAG, "Session without steps, keep it: " + storeSession);
        }

        if (storeSession) {
            mCallSession.duration = System.currentTimeMillis() - mCallSession.timestamp;
            mCallSession.stepCount = stepCount;
            long id = mCallSession.save();
            DebugLog.d(TAG, "Inserted session " + mCallSession + " to db, id " + id);
            return true;

        } else {
            DebugLog.i(TAG, "Discarding empty session");
        }

        return false;
    }

    private void saveTimestampList() {
        SparseIntArray minutesList = new SparseIntArray();

        int sum = 0;
        int previousMinute = 0;

        for (int index = 0; index < mTimestampList.size(); index++) {
            int seconds = mTimestampList.keyAt(index) / 1000;
            int minute = seconds / 60;

            if (minute != previousMinute) {
                //DebugLog.d(TAG, String.format(Locale.US, "storing %d steps to minute %d", sum, previousMinute));
                minutesList.append(previousMinute, sum);
                sum = 0;
            }

            sum += mTimestampList.valueAt(index);
            //DebugLog.d(TAG, String.format(Locale.US, "adding %d steps to minute %d, now %d", mTimestampList.valueAt(index), minute, sum));
            previousMinute = minute;
        }

        if (sum > 0) {
            //DebugLog.d(TAG, String.format(Locale.US, "storing final %d steps to last minute %d", sum, previousMinute));
            minutesList.append(previousMinute, sum);
        }

        for (int index = 0; index < minutesList.size(); index++) {
            int minute = minutesList.keyAt(index);
            int steps  = minutesList.valueAt(index);
            //DebugLog.d(TAG, String.format(Locale.US, "minute %3d: %3d", minute, steps));
            MinuteSteps minuteSteps = new MinuteSteps();
            minuteSteps.callSession = mCallSession;
            minuteSteps.minute = minute;
            minuteSteps.steps = steps;
            minuteSteps.save();
        }

        DebugLog.d(TAG, String.format(Locale.US, "Stored %d MinuteSteps entries for CallSession %d",
                minutesList.size(), mCallSession.getId()));
    }

    public class SensorBinder extends Binder {
        public SensorService getService() {
            return SensorService.this;
        }
    }


    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            DebugLog.d(TAG, String.format(Locale.US, "onCallStateChanged(%d, %s)", state, incomingNumber));

            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    DebugLog.d(TAG, "PhoneStateListener CALL_STATE_RINGING");
                    if (!mCountStarted) {
                        /*
                         * Incoming call. This state is received before the CALL_STATE_OFFHOOK
                         * change, which is starting the actual counting.
                         * If we're here, it means a call is incoming, otherwise not.
                         * Ignore if we're already on a call.
                         *
                         * TODO: handle incoming call when already on the phone / put on hold etc.
                         */
                        mCallIncoming = true;
                    }
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    DebugLog.d(TAG, "PhoneStateListener CALL_STATE_OFFHOOK");
                    if (!mCountStarted) {
                        startCounting();
                        mCallSession.isIncoming = mCallIncoming;
                        if (mSharedPrefs.getBoolean(SettingsActivity.STORE_PHONE_NUMBER, false)) {
                            mCallSession.phoneNumber = incomingNumber;
                        }
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    DebugLog.d(TAG, "PhoneStateListener CALL_STATE_IDLE");
                    stopCounting();
                    break;
            }
        }
    };


    public void testingStartCounter() {
        startCounting();
    }

    public void testingStopCounter() {
        stopCounting();
    }

    public void testingDumpLogCallSessions() {
        DebugLog.d(TAG, "Getting sessions");
        List<CallSession> sessions = CallSession.getSessions();
        for (CallSession session : sessions) {
            DebugLog.d(TAG, "Session: " + session);
        }
    }
}
