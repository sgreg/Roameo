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

import java.util.ArrayList;
import java.util.List;

/**
 * Internal data storage for minute information of a call session
 */
public class MinuteStepData {
    final List<Integer> minutes = new ArrayList<>();
    int maxStep = 0;
    int minStep = 0;
    int stepSum = 0;
    float avgStep = 0;

    MinuteStepData() {
        // package private constructor to prevent instantiations from random places
    }

    public List<Integer> getMinuteList() {
        return minutes;
    }

    public float getAvgStep() {
        return avgStep;
    }

    public int getMinuteCount() {
        return minutes.size();
    }
}
