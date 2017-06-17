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

package fi.craplab.roameo.model;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.google.gson.annotations.Expose;

/**
 * Minute steps database model.
 *
 * Store number of steps for each phone call minute for easier data display later on.
 */
@Table(name="minute_steps")
public class MinuteSteps extends Model {
    @Column(name="call_session", onDelete = Column.ForeignKeyAction.CASCADE)
    public CallSession callSession;

    @Column(name="minute")
    @Expose
    public int minute;

    @Column(name="steps")
    @Expose
    public int steps;

    public MinuteSteps() {}

    @Override
    public String toString() {
        return "MinuteSteps{" +
                "callSession=" + callSession +
                ", minute=" + minute +
                ", steps=" + steps +
                '}';
    }
}
