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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioSystem;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.TwoStatePreference;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.DefaultRingtonePreference;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceController;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.core.lifecycle.Lifecycle;
import com.android.settings.core.lifecycle.LifecycleObserver;
import com.android.settings.core.lifecycle.events.OnResume;


public class WorkSoundPreferenceController extends PreferenceController implements
    OnPreferenceChangeListener, LifecycleObserver, OnResume {

    private static final String TAG = "WorkSoundPrefController";
    private static final String KEY_WORK_CATEGORY = "sound_work_settings_section";
    private static final String KEY_WORK_USE_PERSONAL_SOUNDS = "work_use_personal_sounds";
    private static final String KEY_WORK_PHONE_RINGTONE = "work_ringtone";
    private static final String KEY_WORK_NOTIFICATION_RINGTONE = "work_notification_ringtone";
    private static final String KEY_WORK_ALARM_RINGTONE = "work_alarm_ringtone";

    private PreferenceGroup mWorkPreferenceCategory;
    private TwoStatePreference mWorkUsePersonalSounds;
    private Preference mWorkPhoneRingtonePreference;
    private Preference mWorkNotificationRingtonePreference;
    private Preference mWorkAlarmRingtonePreference;
    private boolean mVoiceCapable;
    private UserManager mUserManager;
    private SoundSettings mParent;
    private AudioHelper mHelper;

    private @UserIdInt
    int mManagedProfileId;

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
        // do nothing
    }

    @Override
    public void onResume() {
        if (isAvailable()) {
            if ((mWorkPreferenceCategory == null)) {
                // Work preferences not yet set
                mParent.addPreferencesFromResource(R.xml.sound_work_settings);
                initWorkPreferences();
            }
            if (!mWorkUsePersonalSounds.isChecked()) {
                updateWorkRingtoneSummaries();
            }
        } else {
            maybeRemoveWorkPreferences();
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_WORK_CATEGORY;
    }

    @Override
    public boolean isAvailable() {
        mManagedProfileId = mHelper.getManagedProfileId(mUserManager);
        return mManagedProfileId != UserHandle.USER_NULL && shouldShowRingtoneSettings();
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

    // === Phone & notification ringtone ===

    private boolean shouldShowRingtoneSettings() {
        return !mHelper.isSingleVolume();
    }

    private CharSequence updateRingtoneName(Context context, int type) {
        if (context == null || !UserManager.get(context).isUserUnlocked(context.getUserId())) {
            return context.getString(R.string.managed_profile_not_available_label);
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

    private DefaultRingtonePreference initWorkPreference(String key) {
        DefaultRingtonePreference pref =
            (DefaultRingtonePreference) mParent.getPreferenceScreen().findPreference(key);
        pref.setOnPreferenceChangeListener(this);

        // Required so that RingtonePickerActivity lists the work profile ringtones
        pref.setUserId(mManagedProfileId);
        return pref;
    }

    private void initWorkPreferences() {
        mWorkPreferenceCategory = (PreferenceGroup) mParent.getPreferenceScreen()
            .findPreference(KEY_WORK_CATEGORY);
        mWorkUsePersonalSounds = (TwoStatePreference) mParent.getPreferenceScreen()
            .findPreference(KEY_WORK_USE_PERSONAL_SOUNDS);
        mWorkPhoneRingtonePreference = initWorkPreference(KEY_WORK_PHONE_RINGTONE);
        mWorkNotificationRingtonePreference = initWorkPreference(KEY_WORK_NOTIFICATION_RINGTONE);
        mWorkAlarmRingtonePreference = initWorkPreference(KEY_WORK_ALARM_RINGTONE);

        if (!mVoiceCapable) {
            mWorkPreferenceCategory.removePreference(mWorkPhoneRingtonePreference);
            mWorkPhoneRingtonePreference = null;
        }

        Context managedProfileContext = getManagedProfileContext();
        if (Settings.Secure.getIntForUser(managedProfileContext.getContentResolver(),
            Settings.Secure.SYNC_PARENT_SOUNDS, 0, mManagedProfileId) == 1) {
            enableWorkSyncSettings();
        } else {
            disableWorkSyncSettings();
        }

        mWorkUsePersonalSounds.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((boolean) newValue) {
                    UnifyWorkDialogFragment.show(mParent);
                    return false;
                } else {
                    disableWorkSync();
                    return true;
                }
            }
        });
    }

    void enableWorkSync() {
        RingtoneManager.enableSyncFromParent(getManagedProfileContext());
        enableWorkSyncSettings();
    }

    private void enableWorkSyncSettings() {
        mWorkUsePersonalSounds.setChecked(true);

        if (mWorkPhoneRingtonePreference != null) {
            mWorkPhoneRingtonePreference.setSummary(
                com.android.settings.R.string.work_sound_same_as_personal);
        }
        mWorkNotificationRingtonePreference.setSummary(
            com.android.settings.R.string.work_sound_same_as_personal);
        mWorkAlarmRingtonePreference.setSummary(
            com.android.settings.R.string.work_sound_same_as_personal);
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

    private void maybeRemoveWorkPreferences() {
        if (mWorkPreferenceCategory == null) {
            // No work preferences to remove
            return;
        }
        mParent.getPreferenceScreen().removePreference(mWorkPreferenceCategory);
        mWorkPreferenceCategory = null;
        mWorkPhoneRingtonePreference = null;
        mWorkNotificationRingtonePreference = null;
        mWorkAlarmRingtonePreference = null;
    }

    public static class UnifyWorkDialogFragment extends InstrumentedDialogFragment
        implements DialogInterface.OnClickListener {
        private static final String TAG = "UnifyWorkDialogFragment";
        private static final int REQUEST_CODE = 200;

        @Override
        public int getMetricsCategory() {
            return MetricsEvent.DIALOG_UNIFY_SOUND_SETTINGS;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                .setTitle(com.android.settings.R.string.work_sync_dialog_title)
                .setMessage(com.android.settings.R.string.work_sync_dialog_message)
                .setPositiveButton(com.android.settings.R.string.work_sync_dialog_yes,
                    UnifyWorkDialogFragment.this)
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
