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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.widget.TextView;

import fi.craplab.roameo.R;
import fi.craplab.roameo.model.CallSession;
import fi.craplab.roameo.ui.SettingsActivity;

/**
 *
 */
public class UiUtils {
    private static ContentResolver sContentResolver = null;

    public static void setPhoneNumber(Context context, TextView view, CallSession session) {
        String number = phoneNumberString(context, session);
        int stringRes = (session.isIncoming) ? R.string.call_number_in : R.string.call_number_out;

        view.setText(context.getString(stringRes, number));
    }

    private static String phoneNumberString(Context context, CallSession session) {
        if (session.phoneNumber == null || session.phoneNumber.isEmpty()) {
            return context.getString(R.string.call_unknown_number);
        }

        String numberString = session.phoneNumber;

        if (SettingsActivity.mapNumberToContact(context)) {

            if (sContentResolver == null) {
                sContentResolver = context.getContentResolver();
            }

            Uri uri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(session.phoneNumber));

            Cursor cursor = sContentResolver.query(uri,
                    new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                numberString = cursor.getString(cursor.getColumnIndex(
                        ContactsContract.PhoneLookup.DISPLAY_NAME));

                if (!cursor.isClosed()) {
                    cursor.close();
                }
            }
        }

        return numberString;
    }
}
