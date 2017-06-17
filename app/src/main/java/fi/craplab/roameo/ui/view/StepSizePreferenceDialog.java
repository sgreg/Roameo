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

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

import fi.craplab.roameo.R;

/**
 * Dialog to set step size from the {@link fi.craplab.roameo.ui.SettingsActivity}
 */
public class StepSizePreferenceDialog extends DialogPreference {
    private NumberPicker mNumberPicker;
    private Integer mDefaultValue;
    private int mCurrentValue;
    private final int mStepSizeMin;
    private final int mStepSizeMax;

    public StepSizePreferenceDialog(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray numberPickerType = context.obtainStyledAttributes(attrs, R.styleable.StepSizePreferenceDialog, 0, 0);
        mStepSizeMin = numberPickerType.getInt(R.styleable.StepSizePreferenceDialog_min, R.integer.step_size_min);
        mStepSizeMax = numberPickerType.getInt(R.styleable.StepSizePreferenceDialog_max, R.integer.step_size_max);
        numberPickerType.recycle();

        setDialogLayoutResource(R.layout.step_size_dialog);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValueObject) {
        Integer integerObjectValue = (Integer) defaultValueObject;
        if (restorePersistedValue) {
            int defaultInt = mDefaultValue != null ? mDefaultValue : R.integer.step_size_default;
            mCurrentValue = this.getPersistedInt(defaultInt);
        } else {
            mCurrentValue = integerObjectValue;
            persistInt(mCurrentValue);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        mDefaultValue = a.getInteger(index, R.integer.step_size_default);
        return mDefaultValue;
    }

    @Override
    public void onBindDialogView(View view) {
        mNumberPicker = (NumberPicker) view.findViewById(R.id.numberPicker);
        mNumberPicker.setMinValue(mStepSizeMin);
        mNumberPicker.setMaxValue(mStepSizeMax);
        mNumberPicker.setValue(mCurrentValue);
        mNumberPicker.setWrapSelectorWheel(false);
        super.onBindDialogView(view);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            int value = mNumberPicker.getValue();

            if (mCurrentValue == value) {
                persistInt(-1); // force value change (maybe not the best way, but it's a way)
            } else {
                mCurrentValue = value;
            }

            persistInt(value);
        }
    }
}
