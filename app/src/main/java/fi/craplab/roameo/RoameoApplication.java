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

import com.activeandroid.app.Application;
import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.FontAwesomeModule;
import com.joanzapata.iconify.fonts.SimpleLineIconsModule;

import fi.craplab.roameo.util.DebugLog;

/**
 *
 */
public class RoameoApplication extends Application {
    private static final String TAG = RoameoApplication.class.getSimpleName();

    public static final int PERMISSION_REQUEST_READ_PHONE_STATE = 0xf000;
    public static final int PERMISSION_REQUEST_READ_CONTACTS = 0xf001;
    public static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 0xf002;

    @Override
    public void onCreate() {
        DebugLog.d(TAG, "Starting the application");
        super.onCreate();
        Iconify.with(new FontAwesomeModule()).with(new SimpleLineIconsModule())        ;
    }
}
