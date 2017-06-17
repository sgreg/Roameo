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

package fi.craplab.roameo.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import fi.craplab.roameo.R;
import fi.craplab.roameo.model.CallSession;
import fi.craplab.roameo.util.DebugLog;
import fi.craplab.roameo.util.UiUtils;
import fi.craplab.roameo.util.Utils;

/**
 * Statistic items.
 */
class StatItemAdapter extends BaseAdapter {
    private static final String TAG = StatItemAdapter.class.getSimpleName();

    private static class ItemHolder {
        TextView phoneNumber;
        TextView callTime;
        TextView callDuration;
        TextView steps;
        TextView pace;
    }

    private final Context mContext;
    private final LayoutInflater mInflater;
    private List<CallSession> mCallSessions;

    private StatItemAdapter(Context context, @NonNull List<CallSession> callSessions) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mCallSessions = callSessions;
    }

    StatItemAdapter(Context context) {
        this(context, new ArrayList<CallSession>());
    }

    void updateData(List<CallSession> callSessions) {
        mCallSessions = callSessions;
        super.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mCallSessions.size();
    }

    @Override
    public CallSession getItem(int position) {
        return mCallSessions.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DebugLog.d(TAG, String.format(Locale.US, "getView(%d, %s, %s)", position, convertView, parent));
        CallSession session = mCallSessions.get(position);
        ItemHolder itemHolder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.session_stat_item, parent, false);
            itemHolder = new ItemHolder();
            itemHolder.phoneNumber = (TextView) convertView.findViewById(R.id.phone_number);
            itemHolder.callTime = (TextView) convertView.findViewById(R.id.call_time);
            itemHolder.callDuration = (TextView) convertView.findViewById(R.id.call_duration);
            itemHolder.steps = (TextView) convertView.findViewById(R.id.call_steps);
            itemHolder.pace = (TextView) convertView.findViewById(R.id.call_pace);

            convertView.setTag(itemHolder);
        } else {
            itemHolder = (ItemHolder) convertView.getTag();
        }

        UiUtils.setPhoneNumber(mContext, itemHolder.phoneNumber, session);
        itemHolder.callTime.setText(DateFormat.getTimeInstance(DateFormat.MEDIUM)
                .format(new Date(session.timestamp)));
        itemHolder.callDuration.setText(Utils.millisToTimeString(session.duration));
        itemHolder.steps.setText(mContext.getResources().getQuantityString(R.plurals.steps,
                (int) session.stepCount, session.stepCount));

        float pace = (session.duration > 0)
                ? (session.stepCount / Utils.millisToMinutes(session.duration))
                : 0;
        itemHolder.pace.setText(mContext.getString(R.string.call_pace, pace));

        return convertView;
    }
}
