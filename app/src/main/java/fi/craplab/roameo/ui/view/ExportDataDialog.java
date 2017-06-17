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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.DateTime;

import java.io.IOException;
import java.text.DateFormat;

import fi.craplab.roameo.R;
import fi.craplab.roameo.data.JsonExporter;
import fi.craplab.roameo.model.CallSession;
import fi.craplab.roameo.util.DebugLog;

/**
 *
 */
public class ExportDataDialog extends DialogFragment
        implements ExportDatePickerDialog.OnDateSetListener {

    private static final String TAG = ExportDataDialog.class.getSimpleName();

    private Context mContext;

    private TextView mStartDateTextView;
    private TextView mEndDateTextView;
    private Button mStartDateButton;
    private Button mEndDateButton;
    private CheckBox mExportAllCheckbox;
    private CheckBox mPhoneNumberCheckbox;

    private DateTime mStartDate;
    private DateTime mEndDate;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View rootView = getActivity().getLayoutInflater().inflate(R.layout.dialog_export_data, null);

        mStartDateTextView = (TextView) rootView.findViewById(R.id.start_date);
        mEndDateTextView = (TextView) rootView.findViewById(R.id.end_date);
        mStartDateButton = (Button) rootView.findViewById(R.id.start_date_button);
        mEndDateButton = (Button) rootView.findViewById(R.id.end_date_button);
        mExportAllCheckbox = (CheckBox) rootView.findViewById(R.id.export_all_checkbox);
        mPhoneNumberCheckbox = (CheckBox) rootView.findViewById(R.id.phone_number_checkbox);

        mStartDateButton.setOnClickListener(mDateButtonListener);
        mEndDateButton.setOnClickListener(mDateButtonListener);

        mExportAllCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int color = (isChecked
                        ? Color.GRAY
                        : ContextCompat.getColor(mContext, R.color.colorPrimaryDark));

                mStartDateTextView.setTextColor(color);
                mEndDateTextView.setTextColor(color);
                mStartDateButton.setEnabled(!isChecked);
                mEndDateButton.setEnabled(!isChecked);
            }
        });

        CallSession firstSession = CallSession.getFirstTimestamp();
        setStartDate((firstSession != null) ? new DateTime(firstSession.timestamp) : DateTime.now());
        setEndDate(DateTime.now());

        builder.setTitle(getString(R.string.export_title));
        builder.setView(rootView);
        builder.setPositiveButton(getString(R.string.export_button_export),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        writeExportData();
                    }
                });

        builder.setNegativeButton(getString(R.string.export_button_cancel), null);

        Dialog dialog = builder.create();
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    private final View.OnClickListener mDateButtonListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            int pickerType;
            DateTime dateTime;

            int id = v.getId();

            if (id == R.id.start_date_button) {
                pickerType = ExportDatePickerDialog.START_DATE_PICKER;
                dateTime = mStartDate;
            } else if (id == R.id.end_date_button) {
                pickerType = ExportDatePickerDialog.END_DATE_PICKER;
                dateTime = mEndDate;
            } else {
                DebugLog.e(TAG, "Unknown id " + id + " in onClickListener");
                return;
            }

            ExportDatePickerDialog dialog = new ExportDatePickerDialog(
                    mContext, ExportDataDialog.this, pickerType, dateTime);

            dialog.show();
        }
    };

    @Override
    public void onDateSet(int pickerType, DateTime dateTime) {
        if (pickerType == ExportDatePickerDialog.START_DATE_PICKER) {
            setStartDate(dateTime);
        } else {
            setEndDate(dateTime);
        }
    }

    private void setStartDate(DateTime dateTime) {
        mStartDate = dateTime.withTimeAtStartOfDay();
        mStartDateTextView.setText(DateFormat.getDateInstance(DateFormat.MEDIUM)
                .format(mStartDate.toDate()));
    }

    private void setEndDate(DateTime dateTime) {
        // set end date time to 23:59:59
        mEndDate = dateTime.withTimeAtStartOfDay().plusDays(1).minusSeconds(1);
        mEndDateTextView.setText(DateFormat.getDateInstance(DateFormat.MEDIUM)
                .format(mEndDate.toDate()));
    }

    private void writeExportData() {


        exportCallSessions();
    }

    private void exportCallSessions() {
        JsonExporter exporter = new JsonExporter(mPhoneNumberCheckbox.isChecked());

        try {
            int count = 0;

            if (mExportAllCheckbox.isChecked()) {
                count = exporter.exportAll();
            } else {
                count = exporter.export(mStartDate.getMillis(), mEndDate.getMillis());
            }
            String countString = mContext.getResources()
                    .getQuantityString(R.plurals.call_sessions, count, count);
            Toast.makeText(mContext,
                    getString(R.string.export_toast_success,
                            countString, exporter.getOutputFileName()),
                    Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(mContext, getString(R.string.export_toast_failure),
                    Toast.LENGTH_SHORT).show();

        } finally {
            getDialog().cancel();
        }
    }
}
