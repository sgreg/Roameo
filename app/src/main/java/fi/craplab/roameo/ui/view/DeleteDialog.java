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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.google.android.gms.common.api.GoogleApiClient;

import fi.craplab.roameo.R;
import fi.craplab.roameo.model.CallSession;
import fi.craplab.roameo.share.GoogleFitClientBuilder;

/**
 *
 */
public class DeleteDialog extends DialogFragment {
    private static final String CALL_SESSION_ID = "callSessionId";

    private DeleteDialogListener mDialogListener;

    public static DeleteDialog newInstance(long callSessionId) {
        DeleteDialog dialog = new DeleteDialog();

        Bundle args = new Bundle();
        args.putLong(CALL_SESSION_ID, callSessionId);
        dialog.setArguments(args);

        return dialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mDialogListener = (DeleteDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement DeleteDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final long callSessionId = getArguments().getLong(CALL_SESSION_ID, -1);
        final CallSession callSession = CallSession.getById(callSessionId);

        if (callSessionId == -1 || callSession == null) {
            throw new IllegalStateException("Missing or invalid CallSession ID " + callSessionId);
        }

        View rootView = getActivity().getLayoutInflater().inflate(R.layout.dialog_delete, null);
        final LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.google_fit_layout);
        final CheckBox checkBox = (CheckBox) rootView.findViewById(R.id.google_fit_checkbox);

        GoogleApiClient gFitClient = GoogleFitClientBuilder.getApiClient();

        boolean showCheckBox = callSession.googleFitIdentifier != null && gFitClient != null;
        layout.setVisibility(showCheckBox ? View.VISIBLE : View.GONE);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.delete_dialog_title);
        builder.setView(rootView);
        builder.setPositiveButton(R.string.delete_dialog_button_yes,
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mDialogListener.onConfirmDelete(callSessionId, checkBox.isChecked());
            }
        });
        builder.setNegativeButton(R.string.delete_dialog_button_no, null);

        return builder.create();
    }

    public interface DeleteDialogListener {
        void onConfirmDelete(long callSessionId, boolean deleteFromGoogleFit);
    }
}
