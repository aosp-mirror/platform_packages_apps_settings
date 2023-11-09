package com.android.settings.backup;

import android.app.Dialog;
import android.app.backup.IBackupManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SettingsMainSwitchBar;

/**
 * Fragment to display a bunch of text about backup and restore, and allow the user to enable/
 * disable it.
 */
public class ToggleBackupSettingFragment extends SettingsPreferenceFragment
        implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
    private static final String TAG = "ToggleBackupSettingFragment";

    private static final String BACKUP_TOGGLE = "toggle_backup";

    // System setting that governs whether the user is eligible for full app-data backup,
    // based on whether they have been presented with the details of what that backup entails
    // (usually surfaced somewhere like device setup)
    private static final String USER_FULL_DATA_BACKUP_AWARE = "user_full_data_backup_aware";

    private IBackupManager mBackupManager;

    protected SettingsMainSwitchBar mSwitchBar;

    private Preference mSummaryPreference;

    private Dialog mConfirmDialog;

    private boolean mWaitingForConfirmationDialog = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBackupManager = IBackupManager.Stub.asInterface(
                ServiceManager.getService(Context.BACKUP_SERVICE));

        PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(
                getActivity());
        setPreferenceScreen(preferenceScreen);
        mSummaryPreference = new Preference(getPrefContext()) {
            @Override
            public void onBindViewHolder(PreferenceViewHolder view) {
                super.onBindViewHolder(view);
                final TextView summaryView = (TextView) view.findViewById(android.R.id.summary);
                summaryView.setText(getSummary());
            }
        };
        mSummaryPreference.setPersistent(false);
        mSummaryPreference.setLayoutResource(R.layout.text_description_preference);
        preferenceScreen.addPreference(mSummaryPreference);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SettingsActivity activity = (SettingsActivity) getActivity();
        mSwitchBar = activity.getSwitchBar();

        // Set up UI.
        // If the user has not seen legal text for full data backup (if they OTA from L to M) then
        // full data backup will be off and here we want to show the old summary here that does
        // not mention full data backup
        if (Settings.Secure.getInt(getContentResolver(), USER_FULL_DATA_BACKUP_AWARE, 0) != 0) {
            mSummaryPreference.setSummary(R.string.fullbackup_data_summary);
        } else {
            mSummaryPreference.setSummary(R.string.backup_data_summary);
        }
        try {
            boolean backupEnabled = mBackupManager == null ?
                    false : mBackupManager.isBackupEnabled();
            mSwitchBar.setCheckedInternal(backupEnabled);
        } catch (RemoteException e) {
            // The world is aflame, turn it off.
            mSwitchBar.setEnabled(false);
        }
        getActivity().setTitle(R.string.backup_data_title);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mSwitchBar.setOnBeforeCheckedChangeListener(null);
        mSwitchBar.hide();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Set up toggle listener. We need this b/c we have to intercept the toggle event in order
        // to pop up the dialogue.
        mSwitchBar.setOnBeforeCheckedChangeListener(
                new SettingsMainSwitchBar.OnBeforeCheckedChangeListener() {
                    @Override
                    public boolean onBeforeCheckedChanged(boolean checked) {
                        if (!checked) {
                            // Don't change Switch status until user makes choice in dialog
                            // so return true here.
                            showEraseBackupDialog();
                            return true;
                        } else {
                            setBackupEnabled(true);
                            mSwitchBar.setCheckedInternal(true);
                            return true;
                        }
                    }
                });
        mSwitchBar.show();
    }

    /** Get rid of the dialog if it's still showing. */
    @Override
    public void onStop() {
        if (mConfirmDialog != null && mConfirmDialog.isShowing()) {
            mConfirmDialog.dismiss();
        }
        mConfirmDialog = null;
        super.onStop();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        // Accept turning off backup
        if (which == DialogInterface.BUTTON_POSITIVE) {
            mWaitingForConfirmationDialog = false;
            setBackupEnabled(false);
            mSwitchBar.setCheckedInternal(false);
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            // Reject turning off backup
            mWaitingForConfirmationDialog = false;
            setBackupEnabled(true);
            mSwitchBar.setCheckedInternal(true);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mWaitingForConfirmationDialog) {
            // dismiss turning off backup
            setBackupEnabled(true);
            mSwitchBar.setCheckedInternal(true);
        }
    }

    private void showEraseBackupDialog() {
        CharSequence msg;

        // If the user has not seen legal text for full data backup (if they OTA from L to M) then
        // full data backup will be off and here we want to show the old erase_dialog_message here
        // that does not mention full data backup
        if (Settings.Secure.getInt(getContentResolver(), USER_FULL_DATA_BACKUP_AWARE, 0) != 0) {
            msg = getResources().getText(R.string.fullbackup_erase_dialog_message);
        } else {
            msg = getResources().getText(R.string.backup_erase_dialog_message);
        }

        mWaitingForConfirmationDialog = true;

        // TODO: DialogFragment?
        mConfirmDialog = new AlertDialog.Builder(getActivity()).setMessage(msg)
                .setTitle(R.string.backup_erase_dialog_title)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this)
                .setOnDismissListener(this)
                .show();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PRIVACY;
    }

    /**
     * Informs the BackupManager of a change in backup state - if backup is disabled,
     * the data on the server will be erased.
     * @param enable whether to enable backup
     */
    private void setBackupEnabled(boolean enable) {
        if (mBackupManager != null) {
            try {
                mBackupManager.setBackupEnabled(enable);
            } catch (RemoteException e) {
                Log.e(TAG, "Error communicating with BackupManager", e);
                return;
            }
        }
    }
}
