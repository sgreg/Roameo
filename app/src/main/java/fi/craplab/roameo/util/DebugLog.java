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

import android.util.Log;

import fi.craplab.roameo.BuildConfig;

/**
 *
 */
@SuppressWarnings("All")
public final class DebugLog {
    private static final boolean logEnabled = BuildConfig.DebugLogOutput;

    public static void d(String tag, String message) {
        if (logEnabled) {
            Log.d(tag, message);
        }
    }
    public static void d(String tag, String message, Throwable t) {
        if (logEnabled) {
            Log.d(tag, message, t);
        }
    }

    public static void i(String tag, String message) {
        if (logEnabled) {
            Log.i(tag, message);
        }
    }
    public static void i(String tag, String message, Throwable t) {
        if (logEnabled) {
            Log.i(tag, message, t);
        }
    }

    public static void w(String tag, String message) {
        Log.w(tag, message);
    }
    public static void w(String tag, String message, Throwable t) {
        Log.w(tag, message, t);
    }

    public static void e(String tag, String message) {
        Log.e(tag, message);
    }
    public static void e(String tag, String message, Throwable t) {
        Log.e(tag, message, t);
    }
}
