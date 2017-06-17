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
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import fi.craplab.roameo.R;

/**
 * Permission request dialog.
 *
 * Handles user notification to explain which permissions are used why and how.
 * This doubles as welcoming screen during very first start.
 */
// TODO should probably be renamed to something like ReadPhoneStateDialog
public class PermissionDialog extends DialogFragment {
    private static final String FIRST_TIME_START = "firstTimeStart";
    private static final String RETRY_DIALOG = "retryDialog";

    private PermissionDialogListener mDialogListener;


    /**
     * Create a {@code PermissionDialog} to explain the permission requirements.
     *
     * If the app is started for the very first time, a welcoming hello is displayed as well.
     *
     * @param isFirstTimeStart {@code true} if app is started for the first time,
     *                         {@code false} otherwise.
     * @return Permission dialog
     */
    public static PermissionDialog newInstance(boolean isFirstTimeStart) {
        return newDialog(isFirstTimeStart, false);
    }

    /**
     * Create a {@code PermissionDialog} to retry asking for permissions.
     *
     * Roameo <b>needs</b> {@link android.Manifest.permission#READ_PHONE_STATE} permissions
     * granted or otherwise it's simply a useless app that cannot do anything.
     *
     * @return Permission retry dialog
     */
    public static PermissionDialog retryInstance() {
        return newDialog(false, true);
    }

    /**
     * Create a basic {@code PermissionDialog} and set its arguments.
     * Android discourages non-standard constructors for Fragments and prefers Bundle arguments.
     *
     * @param isFirstTimeStart {@code true} if app is started for the first time,
     *                         {@code false} otherwise.
     * @param isRetryDialog {@code true} if the created dialog should be a retry dialog,
     *                      {@code false} otherwise
     * @return Permission dialog
     */
    private static PermissionDialog newDialog(boolean isFirstTimeStart, boolean isRetryDialog) {
        PermissionDialog dialog = new PermissionDialog();

        Bundle args = new Bundle();
        args.putBoolean(FIRST_TIME_START, isFirstTimeStart);
        args.putBoolean(RETRY_DIALOG, isRetryDialog);
        dialog.setArguments(args);

        return dialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // make sure calling class is implementing PermissionDialogListener
        try {
            mDialogListener = (PermissionDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                    context.toString() + " must implement PermissionDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        boolean firstTimeStart = getArguments().getBoolean(FIRST_TIME_START, false);
        boolean retryDialog = getArguments().getBoolean(RETRY_DIALOG, false);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        int titleStringRes;
        int messageStringRes;
        int extraStringRes = -1;

        if (firstTimeStart) {
            titleStringRes   = R.string.permission_title_first_start;
            if (Build.VERSION.SDK_INT >= 23) {
                messageStringRes = R.string.permission_message_first_start;
                extraStringRes = R.string.permission_message_normal;
            } else {
                messageStringRes = R.string.permission_message_pre_runtime_perms;
            }
        } else if (retryDialog) {
            titleStringRes   = R.string.permission_title_retry;
            messageStringRes = R.string.permission_message_retry;
        } else {
            titleStringRes   = R.string.permission_title_normal;
            messageStringRes = R.string.permission_message_normal;
        }

        builder.setTitle(titleStringRes);

        if (extraStringRes == -1) {
            builder.setMessage(messageStringRes);
        } else {
            builder.setMessage(getString(messageStringRes, getString(extraStringRes)));
        }

        if (retryDialog) {
            builder.setPositiveButton(R.string.permission_button_retry_okay, mOkayListener);
            builder.setNegativeButton(R.string.permission_button_retry_exit, mExitListener);
        } else {
            builder.setPositiveButton(R.string.permission_button_okay, mOkayListener);
        }

        Dialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        return dialog;
    }

    private final DialogInterface.OnClickListener mOkayListener =
            new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            mDialogListener.onPermissionDialogOkay();
        }
    };

    private final DialogInterface.OnClickListener mExitListener =
            new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            mDialogListener.onPermissionDialogExit();
        }
    };

    public interface PermissionDialogListener {
        void onPermissionDialogOkay();
        void onPermissionDialogExit();
    }
}
