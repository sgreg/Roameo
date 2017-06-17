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

import android.os.Environment;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.joda.time.DateTime;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

import fi.craplab.roameo.model.CallSession;
import fi.craplab.roameo.util.DebugLog;

/**
 *
 */
public class JsonExporter {
    private static final String TAG = JsonExporter.class.getSimpleName();

    private List <CallSession> mCallSessions;
    private final File mOutFile;
    private final boolean mExportPhoneNumbers;

    public JsonExporter(boolean exportPhoneNumbers) {
        mOutFile = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "Roameo.json");

        mExportPhoneNumbers = exportPhoneNumbers;
    }

    public String getOutputFileName() {
        return (mOutFile != null) ? mOutFile.getAbsolutePath() : "<unknown>";
    }

    public int exportAll() throws IOException {
        mCallSessions = CallSession.getSessions();
        return export();
    }

    public int export(long start, long end) throws IOException {
        mCallSessions = CallSession.getSessions(start, end);
        return export();
    }

    private int export() throws IOException {
        long startTimestamp = 0;
        long endTimestamp = System.currentTimeMillis();

        if (!mCallSessions.isEmpty()) {
            startTimestamp = mCallSessions.get(0).timestamp;
            endTimestamp = mCallSessions.get(mCallSessions.size() - 1).timestamp;
        }

        DebugLog.d(TAG, String.format("Exporting sessions from %s to %s",
                new DateTime(startTimestamp),
                new DateTime(endTimestamp)));

        Gson gson = createGson();

        JsonObject exportData = new JsonObject();
        exportData.addProperty("startDate", startTimestamp);
        exportData.addProperty("endData", endTimestamp);

        JsonArray sessions = new JsonArray();
        for (CallSession session : mCallSessions) {
            JsonObject json = gson.toJsonTree(session).getAsJsonObject();
            json.add("minuteSteps", gson.toJsonTree(session.getMinuteSteps()));
            sessions.add(json);
        }

        exportData.add("callSessions", sessions);

        Writer writer = new FileWriter(mOutFile);
        gson.toJson(exportData, writer); // here be IOExceptions
        writer.close();

        return mCallSessions.size();
    }

    private Gson createGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();

        gsonBuilder.setPrettyPrinting();
        gsonBuilder.excludeFieldsWithoutExposeAnnotation();

        if (!mExportPhoneNumbers) {
            gsonBuilder.setExclusionStrategies(new PhoneNumberExclusionStrategy());
        }

        return gsonBuilder.create();
    }


    private class PhoneNumberExclusionStrategy implements ExclusionStrategy {

        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            return f.getDeclaringClass() == CallSession.class && f.getName().equals("phoneNumber");
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    }
}
