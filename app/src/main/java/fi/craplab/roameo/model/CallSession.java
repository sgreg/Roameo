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

import android.database.Cursor;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.google.gson.annotations.Expose;

import java.util.List;

import fi.craplab.roameo.share.GoogleFitUploadTask;
import fi.craplab.roameo.util.Utils;

/**
 * Call session database model
 */
@Table(name="call_session")
public class CallSession extends Model {
    /** Call session timestamp in millis */
    @Column(name="timestamp")
    @Expose
    public long timestamp;

    /** Phone number of incoming or outgoing call - if enabled */
    @Column(name="phone_number")
    @Expose
    public String phoneNumber;

    /** True if call was incoming, false if call was outgoing */
    @Column(name="incoming")
    @Expose
    public boolean isIncoming;

    /** Call duration in millis */
    @Column(name="duration")
    @Expose
    public long duration;

    /** Step count */
    @Column(name="step_count")
    @Expose
    public long stepCount;

    /** Google Fit identifier set by {@link GoogleFitUploadTask} */
    @Column(name="google_fit_id")
    public String googleFitIdentifier;

    @Override
    public String toString() {
        return "CallSession{" +
                "id=" + super.getId() +
                ", timestamp=" + timestamp +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", isIncoming=" + isIncoming +
                ", duration=" + duration +
                ", stepCount=" + stepCount +
                ", googleFitIdentifier='" + googleFitIdentifier + '\'' +
                '}';
    }

    /*
     * TODO store distance based on current step size setting? Or calculate distance on the fly?
     * keep in mind that Google Fit data might become inconsistent or needs to be updated this way.
     */

    public CallSession() {
        /* default constructor for ActiveAndroid */
    }

    /**
     * Get {@code CallSession}'s list of stored {@link MinuteSteps}.
     *
     * @return CallSession's {@link MinuteSteps} list
     */
    public List<MinuteSteps> getMinuteSteps() {
        return getMany(MinuteSteps.class, "call_session");
    }


    private static List<CallSession> getSessions(boolean ascending) {
        return new Select()
                .from(CallSession.class)
                .orderBy("timestamp " + ((ascending) ? "ASC" : "DESC"))
                .execute();
    }

    public static List<CallSession> getSessions() {
        return getSessions(true);
    }

    public static List<CallSession> getSessionsReverse() {
        return getSessions(false);
    }

    public static List<CallSession> getSessionsForDay(long timestamp) {
        return getSessions(timestamp, timestamp + Utils.MILLIS_PER_DAY - 1);
    }

    public static List<CallSession> getSessionsForWeek(long timestamp) {
        return getSessions(timestamp, timestamp + Utils.MILLIS_PER_WEEK - 1);
    }

    public static List<CallSession> getSessions(long startTimestamp, long endTimestamp) {
        return new Select()
                .from(CallSession.class)
                .where("timestamp >= ?", startTimestamp)
                .and("timestamp <= ?", endTimestamp)
                .orderBy("timestamp ASC")
                .execute();
    }

    public static CallSession getById(long id) {
        return new Select().from(CallSession.class).where("ID = ?", id).executeSingle();
    }

    public static CallSession getFirstTimestamp() {
        return new Select()
                .from(CallSession.class)
                .orderBy("timestamp ASC")
                .limit(1)
                .executeSingle();
    }

    private static long getAggregateOfColumn(String function, String column) {
        Cursor c = ActiveAndroid.getDatabase().rawQuery(
                "SELECT " + function + "(" + column + ") FROM call_session", null);

        return getSingleValueFromCursor(c);
    }

    private static long getAggregateOfColumn(String function, String column,
                                             long startTimestamp, long duration) {
        Cursor c = ActiveAndroid.getDatabase().rawQuery(
                "SELECT " + function + "(" + column + ") FROM call_session "+
                        "WHERE timestamp >= ? AND timestamp < ?",
                new String[] {
                        String.valueOf(startTimestamp),
                        String.valueOf(startTimestamp + duration)
                }
        );

        return getSingleValueFromCursor(c);
    }

    private static int getSingleValueFromCursor(Cursor c) {
        int value = 0;
        if (c.moveToFirst()) {
            value = c.getInt(0);
        }
        c.close();

        return value;
    }

    private static long getSumOfColumn(String column) {
        return getAggregateOfColumn("SUM", column);
    }

    private static long getSumOfColumn(String column, long startTimestamp, long duration) {
        return getAggregateOfColumn("SUM", column, startTimestamp, duration);
    }

    private static long getMaxOfColumn(String column) {
        return getAggregateOfColumn("MAX", column);
    }

    private static long getMaxOfColumn(String column, long startTimestamp, long duration) {
        return getAggregateOfColumn("MAX", column, startTimestamp, duration);
    }

    /**
     * Get total amount of steps ever recorded.
     *
     * @return Total amount of recorded steps
     */
    public static long getTotalSteps() {
        return getSumOfColumn("step_count");
    }

    /**
     * Get total sum of steps for a given day.
     *
     * @param timestamp Timestamp of given day (preferably midnight)
     * @return Total number of steps recorded for that day
     */
    public static long getStepsForDay(long timestamp) {
        return getSumOfColumn("step_count", timestamp, Utils.MILLIS_PER_DAY);
    }

    /**
     * Get total sum of steps for a given week.
     *
     * @param timestamp Timestamp of given week's start day (preferably midnight)
     * @return Total number of steps recorded for that week
     */
    public static long getStepsForWeek(long timestamp) {
        return getSumOfColumn("step_count", timestamp, Utils.MILLIS_PER_WEEK);
    }

    /**
     * Get total amount of call durations in milliseconds ever recorded.
     *
     * @return Total amount of recorded call durations in milliseconds
     */
    public static long getTotalDurations() {
        return getSumOfColumn("duration");
    }

    /**
     * Get total sum of steps for a given day.
     *
     * @param timestamp Timestamp of given day (preferably midnight)
     * @return Total number of steps recorded for that day
     */
    public static long getDurationsForDay(long timestamp) {
        return getSumOfColumn("duration", timestamp, Utils.MILLIS_PER_DAY);
    }

    /**
     * Get total sum of steps for a given week.
     *
     * @param timestamp Timestamp of given week's start day (preferably midnight)
     * @return Total number of steps recorded for that week
     */
    public static long getDurationsForWeek(long timestamp) {
        return getSumOfColumn("duration", timestamp, Utils.MILLIS_PER_WEEK);
    }

    public static long getMaxSteps() {
        return getMaxOfColumn("step_count");
    }

    public static long getMaxStepsForWeek(long timestamp) {
        return getMaxOfColumn("step_count", timestamp, Utils.MILLIS_PER_WEEK);
    }

    public static long getMaxDuration() {
        return getMaxOfColumn("duration");
    }

    public static long getMaxDurationForWeek(long timestamp) {
        return getMaxOfColumn("duration", timestamp, Utils.MILLIS_PER_WEEK);
    }

}
