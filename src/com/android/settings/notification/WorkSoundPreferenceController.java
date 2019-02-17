/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.notification;

import android.annotation.UserIdInt;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.DefaultRingtonePreference;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import java.util.List;

public class WorkSoundPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, OnPreferenceChangeListener, LifecycleObserver,
        OnResume, OnPause {

    private static final String TAG = "WorkSoundPrefController";
    private static final String KEY_WORK_CATEGORY = "sound_work_settings_section";
    private static final String KEY_WORK_USE_PERSONAL_SOUNDS = "work_use_personal_sounds";
    private static final String KEY_WORK_PHONE_RINGTONE = "work_ringtone";
    private static final String KEY_WORK_NOTIFICATION_RINGTONE = "work_notification_ringtone";
    private static final String KEY_WORK_ALARM_RINGTONE = "work_alarm_ringtone";

    private final boolean mVoiceCapable;
    private final UserManager mUserManager;
    private final SoundSettings mParent;
    private final AudioHelper mHelper;

    private PreferenceGroup mWorkPreferenceCategory;
    private TwoStatePreference mWorkUsePersonalSounds;
    private Preference mWorkPhoneRingtonePreference;
    private Preference mWorkNotificationRingtonePreference;
    private Preference mWorkAlarmRingtonePreference;

    @UserIdInt
    private int mManagedProfileId;

    public WorkSoundPreferenceController(Context context, SoundSettings parent,
            Lifecycle lifecycle) {
        this(context, parent, lifecycle, new AudioHelper(context));
    }

    @VisibleForTesting
    WorkSoundPreferenceController(Context context, SoundSettings parent, Lifecycle lifecycle,
            AudioHelper helper) {
        super(context);
        mUserManager = UserManager.get(context);
        mVoiceCapable = Utils.isVoiceCapable(mContext);
        mParent = parent;
        mHelper = helper;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mWorkPreferenceCategory = screen.findPreference(KEY_WORK_CATEGORY);
    }

    @Override
    public void onResume() {
        IntentFilter managedProfileFilter = new IntentFilter();
        managedProfileFilter.addAction(Intent.ACTION_MANAGED_PROFILE_ADDED);
        managedProfileFilter.addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED);
        mContext.registerReceiver(mManagedProfileReceiver, managedProfileFilter);

        mManagedProfileId = mHelper.getManagedProfileId(mUserManager);
        updateWorkPreferences();
    }

    @Override
    public void onPause() {
        mContext.unregisterReceiver(mManagedProfileReceiver);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_WORK_CATEGORY;
    }

    @Override
    public boolean isAvailable() {
        return mHelper.getManagedProfileId(mUserManager) != UserHandle.USER_NULL
                && shouldShowRingtoneSettings();
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        return false;
    }

    /**
     * Updates the summary of work preferences
     *
     * This controller listens to changes on the work ringtone preferences, identified by keys
     * "work_ringtone", "work_notification_ringtone" and "work_alarm_ringtone".
     */
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int ringtoneType;
        if (KEY_WORK_PHONE_RINGTONE.equals(preference.getKey())) {
            ringtoneType = RingtoneManager.TYPE_RINGTONE;
        } else if (KEY_WORK_NOTIFICATION_RINGTONE.equals(preference.getKey())) {
            ringtoneType = RingtoneManager.TYPE_NOTIFICATION;
        } else if (KEY_WORK_ALARM_RINGTONE.equals(preference.getKey())) {
            ringtoneType = RingtoneManager.TYPE_ALARM;
        } else {
            return true;
        }

        preference.setSummary(updateRingtoneName(getManagedProfileContext(), ringtoneType));
        return true;
    }

    @Override
    public void updateNonIndexableKeys(List<String> keys) {
        if (isAvailable()) {
            return;
        }
        keys.add(KEY_WORK_CATEGORY);
        keys.add(KEY_WORK_USE_PERSONAL_SOUNDS);
        keys.add(KEY_WORK_NOTIFICATION_RINGTONE);
        keys.add(KEY_WORK_PHONE_RINGTONE);
        keys.add(KEY_WORK_ALARM_RINGTONE);
    }

    // === Phone & notification ringtone ===

    private boolean shouldShowRingtoneSettings() {
        return !mHelper.isSingleVolume();
    }

    private CharSequence updateRingtoneName(Context context, int type) {
        if (context == null || !mHelper.isUserUnlocked(mUserManager, context.getUserId())) {
            return mContext.getString(R.string.managed_profile_not_available_label);
        }
        Uri ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, type);
        return Ringtone.getTitle(context, ringtoneUri, false /* followSettingsUri */,
                true /* allowRemote */);
    }

    private Context getManagedProfileContext() {
        if (mManagedProfileId == UserHandle.USER_NULL) {
            return null;
        }
        return mHelper.createPackageContextAsUser(mManagedProfileId);
    }

    private DefaultRingtonePreference initWorkPreference(PreferenceGroup root, String key) {
        DefaultRingtonePreference pref =
                (DefaultRingtonePreference) root.findPreference(key);
        pref.setOnPreferenceChangeListener(this);

        // Required so that RingtonePickerActivity lists the work profile ringtones
        pref.setUserId(mManagedProfileId);
        return pref;
    }

    private void updateWorkPreferences() {
        if (mWorkPreferenceCategory == null) {
            return;
        }
        final boolean isAvailable = isAvailable();
        mWorkPreferenceCategory.setVisible(isAvailable);
        if (!isAvailable) {
            return;
        }
        if (mWorkUsePersonalSounds == null) {
            mWorkUsePersonalSounds = (TwoStatePreference)
                    mWorkPreferenceCategory.findPreference(KEY_WORK_USE_PERSONAL_SOUNDS);
            mWorkUsePersonalSounds.setOnPreferenceChangeListener((Preference p, Object value) -> {
                if ((boolean) value) {
                    UnifyWorkDialogFragment.show(mParent);
                    return false;
                } else {
                    disableWorkSync();
                    return true;
                }
            });
        }
        if (mWorkPhoneRingtonePreference == null) {
            mWorkPhoneRingtonePreference = initWorkPreference(mWorkPreferenceCategory,
                    KEY_WORK_PHONE_RINGTONE);
        }
        if (mWorkNotificationRingtonePreference == null) {
            mWorkNotificationRingtonePreference = initWorkPreference(mWorkPreferenceCategory,
                    KEY_WORK_NOTIFICATION_RINGTONE);
        }
        if (mWorkAlarmRingtonePreference == null) {
            mWorkAlarmRingtonePreference = initWorkPreference(mWorkPreferenceCategory,
                    KEY_WORK_ALARM_RINGTONE);
        }
        if (!mVoiceCapable) {
            mWorkPhoneRingtonePreference.setVisible(false);
            mWorkPhoneRingtonePreference = null;
        }

        final Context managedProfileContext = getManagedProfileContext();
        if (Settings.Secure.getIntForUser(managedProfileContext.getContentResolver(),
                Settings.Secure.SYNC_PARENT_SOUNDS, 0, mManagedProfileId) == 1) {
            enableWorkSyncSettings();
        } else {
            disableWorkSyncSettings();
        }
    }

    void enableWorkSync() {
        RingtoneManager.enableSyncFromParent(getManagedProfileContext());
        enableWorkSyncSettings();
    }

    private void enableWorkSyncSettings() {
        mWorkUsePersonalSounds.setChecked(true);

        if (mWorkPhoneRingtonePreference != null) {
            mWorkPhoneRingtonePreference.setSummary(R.string.work_sound_same_as_personal);
        }
        mWorkNotificationRingtonePreference.setSummary(R.string.work_sound_same_as_personal);
        mWorkAlarmRingtonePreference.setSummary(R.string.work_sound_same_as_personal);
    }

    private void disableWorkSync() {
        RingtoneManager.disableSyncFromParent(getManagedProfileContext());
        disableWorkSyncSettings();
    }

    private void disableWorkSyncSettings() {
        if (mWorkPhoneRingtonePreference != null) {
            mWorkPhoneRingtonePreference.setEnabled(true);
        }
        mWorkNotificationRingtonePreference.setEnabled(true);
        mWorkAlarmRingtonePreference.setEnabled(true);

        updateWorkRingtoneSummaries();
    }

    private void updateWorkRingtoneSummaries() {
        Context managedProfileContext = getManagedProfileContext();

        if (mWorkPhoneRingtonePreference != null) {
            mWorkPhoneRingtonePreference.setSummary(
                    updateRingtoneName(managedProfileContext, RingtoneManager.TYPE_RINGTONE));
        }
        mWorkNotificationRingtonePreference.setSummary(
                updateRingtoneName(managedProfileContext, RingtoneManager.TYPE_NOTIFICATION));
        mWorkAlarmRingtonePreference.setSummary(
                updateRingtoneName(managedProfileContext, RingtoneManager.TYPE_ALARM));
    }

    public void onManagedProfileAdded(@UserIdInt int profileId) {
        if (mManagedProfileId == UserHandle.USER_NULL) {
            mManagedProfileId = profileId;
            updateWorkPreferences();
        }
    }

    public void onManagedProfileRemoved(@UserIdInt int profileId) {
        if (mManagedProfileId == profileId) {
            mManagedProfileId = mHelper.getManagedProfileId(mUserManager);
            updateWorkPreferences();
        }
    }

    private final BroadcastReceiver mManagedProfileReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int userId = ((UserHandle) intent.getExtra(Intent.EXTRA_USER)).getIdentifier();
            switch (intent.getAction()) {
                case Intent.ACTION_MANAGED_PROFILE_ADDED: {
                    onManagedProfileAdded(userId);
                    return;
                }
                case Intent.ACTION_MANAGED_PROFILE_REMOVED: {
                    onManagedProfileRemoved(userId);
                    return;
                }
            }
        }
    };

    public static class UnifyWorkDialogFragment extends InstrumentedDialogFragment
            implements DialogInterface.OnClickListener {
        private static final String TAG = "UnifyWorkDialogFragment";
        private static final int REQUEST_CODE = 200;

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.DIALOG_UNIFY_SOUND_SETTINGS;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.work_sync_dialog_title)
                    .setMessage(R.string.work_sync_dialog_message)
                    .setPositiveButton(R.string.work_sync_dialog_yes, UnifyWorkDialogFragment.this)
                    .setNegativeButton(android.R.string.no, null)
                    .create();
        }

        public static void show(SoundSettings parent) {
            FragmentManager fm = parent.getFragmentManager();
            if (fm.findFragmentByTag(TAG) == null) {
                UnifyWorkDialogFragment fragment = new UnifyWorkDialogFragment();
                fragment.setTargetFragment(parent, REQUEST_CODE);
                fragment.show(fm, TAG);
            }
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            SoundSettings soundSettings = (SoundSettings) getTargetFragment();
            if (soundSettings.isAdded()) {
                soundSettings.enableWorkSync();
            }
        }
    }

}
