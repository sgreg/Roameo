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

package fi.craplab.roameo.data;

import fi.craplab.roameo.model.CallSession;
import fi.craplab.roameo.model.MinuteSteps;
import fi.craplab.roameo.util.Utils;

import java.util.List;

/**
 * Data Cruncher helper class for a {@link CallSession} and its {@link MinuteSteps}.
 *
 * Transforms {@link MinuteSteps} information stored in the database of a given {@link CallSession}
 * into a {@link MinuteStepData} object.
 *
 */
public class SessionCruncher {
    private final CallSession mCallSession;
    private MinuteStepData mMinuteStepData;

    /**
     * Create a new {@code SessionCruncher} for the given {@link CallSession}.
     * @param callSession Main data base.
     */
    public SessionCruncher(CallSession callSession) {
        mCallSession = callSession;
    }

    /**
     * Transform the internal {@link CallSession}'s {@link MinuteSteps} into {@link MinuteStepData}.
     *
     * @return Transformed {@link MinuteStepData}
     */
    public MinuteStepData getMinuteSteps() {
        if (mMinuteStepData == null) {
            createMinuteStepData();
        }

        return mMinuteStepData;
    }

    /**
     * The actual number crunching.
     *
     * When recording a {@link CallSession}, the minute information (i.e. amount of counted steps
     * for each minute of the call) is stored one entry per minute - if the step count value is
     * greater than zero. If no steps are walked within a minute (or several minutes), no entry
     * is written to the database.
     *
     * Here, all the data is read back from the database and stored in a convenient
     * {@link java.util.ArrayList}, filling the gaps of zero-count minutes, so the whole call
     * duration can be easily iterated later on.
     *
     * In addition, the {@link CallSession}'s total step count and average steps per minute are
     * calculated, and the session's min and max step count values are stored.
     */
    private void createMinuteStepData() {
        mMinuteStepData = new MinuteStepData();
        List<MinuteSteps> minuteStepsList = mCallSession.getMinuteSteps();

        long minutes = (long) Utils.millisToMinutes(mCallSession.duration) + 1; // ignore float part

        for (int index = 0, minute = 0; minute < minutes; minute++) {
            MinuteSteps minuteSteps = (index < minuteStepsList.size()) ? minuteStepsList.get(index) : null;

            if (minuteSteps != null && minute == minuteSteps.minute) {
                mMinuteStepData.minutes.add(minuteSteps.steps);
                mMinuteStepData.stepSum += minuteSteps.steps;

                if (minuteSteps.steps > mMinuteStepData.maxStep) {
                    mMinuteStepData.maxStep = minuteSteps.steps;
                }

                if (minuteSteps.steps < mMinuteStepData.minStep) {
                    mMinuteStepData.minStep = minuteSteps.steps;
                }

                index++;

            } else {
                mMinuteStepData.minutes.add(0);
            }
        }

        mMinuteStepData.avgStep = mMinuteStepData.stepSum / minutes;
    }
}
