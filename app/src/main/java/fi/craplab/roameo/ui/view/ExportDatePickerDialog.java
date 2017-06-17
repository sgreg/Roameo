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

package fi.craplab.roameo.ui.view;

import android.app.DatePickerDialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.DatePicker;

import org.joda.time.DateTime;

/**
 *
 */
public class ExportDatePickerDialog implements DatePickerDialog.OnDateSetListener {
    public static final int START_DATE_PICKER = 0;
    public static final int END_DATE_PICKER = 1;

    private final DatePickerDialog mDialog;
    private final OnDateSetListener mListener;
    private final int mPickerType;

    public ExportDatePickerDialog(@NonNull Context context,
                                  @NonNull OnDateSetListener listener,
                                  int datePickerType,
                                  DateTime dateTime) {

        mDialog = new DatePickerDialog(context, this,
                dateTime.getYear(), dateTime.getMonthOfYear() - 1, dateTime.getDayOfMonth());

        mListener = listener;
        mPickerType = datePickerType;
    }

    public void show() {
        mDialog.show();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        mListener.onDateSet(mPickerType, new DateTime(year, month + 1, dayOfMonth, 0, 0));
    }

    public interface OnDateSetListener {
        void onDateSet(int pickerType, DateTime dateTime);
    }
}
