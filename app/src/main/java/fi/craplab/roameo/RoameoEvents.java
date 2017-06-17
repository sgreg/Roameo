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

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

/**
 *
 */
public class RoameoEvents {
    public static final String ACTION_CALL_DATA_UPDATED = "Roameo.action.callDataUpdated";

    public static final String ACTION_WEEK_START_CHANGED = "Roameo.action.weekStartChanged";

    public static final String ACTION_GOOGLE_FIT_ENABLED = "Roameo.action.GoogleFitEnabled";
    public static final String ACTION_GOOGLE_FIT_DISABLED = "Roameo.action.GoogleFitDisabled";

    public static final String ACTION_GOOGLE_FIT_DATA_UPLOADED = "Roameo.action.gFitUploaded";
    public static final String ACTION_GOOGLE_FIT_DATA_UPLOAD_FAIL = "Roameo.action.gFitUploadFail";
    public static final String ACTION_GOOGLE_FIT_DATA_DELETED = "Roameo.action.gFitDeleted";
    public static final String ACTION_GOOGLE_FIT_DATA_DELETE_FAIL = "Roameo.action.gFitDeleteFail";

    public static void send(Context context, String action) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(action));
    }
}
