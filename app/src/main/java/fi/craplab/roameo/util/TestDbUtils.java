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

package fi.craplab.roameo.util;

import android.content.Context;

import com.activeandroid.query.Delete;

import java.util.Locale;

import fi.craplab.roameo.RoameoEvents;
import fi.craplab.roameo.model.CallSession;
import fi.craplab.roameo.model.MinuteSteps;

/**
 *
 */
public class TestDbUtils {

    private static String getRandomNumber() {
        return String.format(Locale.US, "%d%03d%04d",
                (int) (Math.random() * 9 + 1),
                (int) (Math.random() * 999),
                (int) (Math.random() * 9999));
    }

    private static long getSomeRandomTimeOffset() {
        long randomHour = (long) (Math.random() * 3600 * 1000); // something between 0 and 1h
        //return randomHour * 24 * 20; // random in last 20 days
        return randomHour * 24 * 15 + 4*24*3600*1000; // random in last 20 days
    }

    public static CallSession createRandomCallSession(Context context) {
        new Delete().from(CallSession.class).execute();
        for (int i = 0; i < 16; i++) {
            x(context);
        }
        return x(context);
    }

    private static CallSession x(Context context) {
        CallSession callSession = new CallSession();
        callSession.timestamp = System.currentTimeMillis() - getSomeRandomTimeOffset();
        int minutes = (int) (Math.random() * 25 + 5); // 5-25 minutes
        if (minutes % 6 < 2) {
            minutes += (int) (Math.random() * 30 + 10);
        }
        int seconds = (int) (Math.random() * 60); // 0-60 seconds
        callSession.duration = (minutes * 60 + seconds) * 1000;
        callSession.phoneNumber = getRandomNumber();
        //int index = (int) (Math.random() * 6);
        //callSession.phoneNumber = (index < names.length ? names[index] : getRandomNumber());
        callSession.isIncoming = ((int) (Math.random() * 2)) == 1;
        callSession.save();

        int stepSum = 0;
        for (int i = 0; i <= minutes; i++) {
            int steps = (int) (Math.random() * 50);
            if (steps % 2 == 1) {
                steps += (int) (Math.random() * 30);
            }
            if (steps % 10 > 7) {
                steps += (int) (Math.random() * 20 + 10);
            }
            MinuteSteps minuteSteps = new MinuteSteps();
            minuteSteps.callSession = callSession;
            minuteSteps.minute = i;
            minuteSteps.steps = steps;
            minuteSteps.save();
            stepSum += steps;
        }

        callSession.stepCount = stepSum;
        callSession.save();

        DebugLog.d("TEST UTIL", "saving CallSession " + callSession);
        RoameoEvents.send(context, RoameoEvents.ACTION_CALL_DATA_UPDATED);
        return callSession;
    }
}
