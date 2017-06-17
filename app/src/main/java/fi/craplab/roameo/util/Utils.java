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
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;

import org.joda.time.DateTime;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class Utils {
    private static final String TAG = Utils.class.getSimpleName();

    public static final long MILLIS_PER_DAY = 24 * 3600 * 1000;
    public static final long MILLIS_PER_WEEK = MILLIS_PER_DAY * 7;

    /**
     * Converts the {@link android.hardware.SensorEvent#timestamp} to an actual timestamp.
     * {@link android.hardware.SensorEvent#timestamp} is based on {@link System#nanoTime()}, so
     * the hardware timer offset and therefore corresponds to system uptime and not actual time.
     *
     * @param sensorTimestamp Sensor timestamp based on nanosecond hardware timer
     * @return Sensor timestamp as UNIX timestamp
     */
    public static long getSensorTimestamp(long sensorTimestamp) {
        return (System.currentTimeMillis() + (sensorTimestamp - System.nanoTime())) / 1000000L;
    }

    public static String millisToTimeString(long millis) {
        int seconds = (int) (millis / 1000) % 60;
        int minutes = (int) ((millis / (1000*60)) % 60);
        int hours   = (int) ((millis / (1000*60*60)));

        return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
    }

    public static String secondsToTimeString(long seconds) {
        return millisToTimeString(seconds * 1000);
    }

    public static String millisToDateString(long millis) {
        DateTime dt = new DateTime(millis);
        return dt.toDate().toString();
    }

    public static String millisToDateTimeString(long millis) {
        return DateFormat
                .getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM)
                .format(new Date(millis));
    }

    public static float millisToMinutes(long millis) {
        return millis / (60 * 1000.0f);
    }

    /**
     * Return the given number rounded up to the next 10s.
     * If the number is already a multiple of 10, it returns the same number.
     *
     * Examples:
     *     8 ->  10
     *    57 ->  60
     *    80 ->  80
     *   123 -> 130
     *
     * @param number Number to be rounded
     * @return Number rounded up to the next 10s
     */
    public static int roundedUpTen(int number) {
        return ((number + 9) / 10) * 10;
    }

    public static int roundedUpHundred(int number) {
        return ((number + 99) / 100) * 100;
    }

    public static boolean hasRuntimePermission(Context context, String permission) {
        if (Build.VERSION.SDK_INT < 23) {
            // permissions are granted during installation before Marshmallow
            return true;
        }

        int permissionStatus = ActivityCompat.checkSelfPermission(context, permission);

        return permissionStatus == PackageManager.PERMISSION_GRANTED;
    }
}
