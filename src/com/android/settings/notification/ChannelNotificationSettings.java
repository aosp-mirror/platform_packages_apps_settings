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

import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_MIN;
import static android.app.NotificationManager.IMPORTANCE_NONE;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.DimmableIconPreference;
import com.android.settings.R;
import com.android.settings.RingtonePreference;
import com.android.settings.applications.AppHeaderController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedSwitchPreference;

import java.util.ArrayList;
import java.util.List;

public class ChannelNotificationSettings extends NotificationSettingsBase {
    private static final String TAG = "ChannelSettings";

    protected static final String KEY_LIGHTS = "lights";
    protected static final String KEY_VIBRATE = "vibrate";
    protected static final String KEY_RINGTONE = "ringtone";

    protected RestrictedSwitchPreference mLights;
    protected RestrictedSwitchPreference mVibrate;
    protected NotificationSoundPreference mRingtone;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.NOTIFICATION_TOPIC_NOTIFICATION;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mUid < 0 || TextUtils.isEmpty(mPkg) || mPkgInfo == null || mChannel == null) {
            Log.w(TAG, "Missing package or uid or packageinfo or channel");
            finish();
            return;
        }

        if (getPreferenceScreen() != null) {
            getPreferenceScreen().removeAll();
        }
        addPreferencesFromResource(R.xml.channel_notification_settings);
        getPreferenceScreen().setOrderingAsAdded(true);

        // load settings intent
        ArrayMap<String, NotificationBackend.AppRow> rows = new ArrayMap<String, NotificationBackend.AppRow>();
        rows.put(mAppRow.pkg, mAppRow);
        collectConfigActivities(rows);

        mBlock = (RestrictedSwitchPreference) getPreferenceScreen().findPreference(KEY_BLOCK);
        mBadge = (RestrictedSwitchPreference) getPreferenceScreen().findPreference(KEY_BADGE);
        mImportance = (RestrictedDropDownPreference) findPreference(KEY_IMPORTANCE);
        mPriority =
                (RestrictedSwitchPreference) findPreference(KEY_BYPASS_DND);
        mVisibilityOverride =
                (RestrictedDropDownPreference) findPreference(KEY_VISIBILITY_OVERRIDE);
        mLights = (RestrictedSwitchPreference) findPreference(KEY_LIGHTS);
        mVibrate = (RestrictedSwitchPreference) findPreference(KEY_VIBRATE);
        mRingtone = (NotificationSoundPreference) findPreference(KEY_RINGTONE);

        if (mPkgInfo != null && mChannel != null) {
            setupPriorityPref(mChannel.canBypassDnd());
            setupVisOverridePref(mChannel.getLockscreenVisibility());
            setupLights();
            setupVibrate();
            setupRingtone();
            setupBlockAndImportance();
            updateDependents();
        }
        final Activity activity = getActivity();
        final Preference pref = FeatureFactory.getFactory(activity)
                .getApplicationFeatureProvider(activity)
                .newAppHeaderController(this /* fragment */, null /* appHeader */)
                .setIcon(mAppRow.icon)
                .setLabel(mChannel.getName())
                .setSummary(mAppRow.label)
                .setPackageName(mAppRow.pkg)
                .setUid(mAppRow.uid)
                .setButtonActions(AppHeaderController.ActionType.ACTION_APP_INFO,
                        AppHeaderController.ActionType.ACTION_NOTIF_PREFERENCE)
                .done(activity, getPrefContext());
        getPreferenceScreen().addPreference(pref);

        if (mAppRow.settingsIntent != null) {
            Preference intentPref = new Preference(getPrefContext());
            intentPref.setIntent(mAppRow.settingsIntent);
            intentPref.setTitle(mContext.getString(R.string.app_settings_link));
            getPreferenceScreen().addPreference(intentPref);
        }

        if (!TextUtils.isEmpty(mChannel.getDescription())) {
            DimmableIconPreference descPref = new DimmableIconPreference(getPrefContext());
            descPref.setSelectable(false);
            descPref.setSummary(mChannel.getDescription());
            descPref.setIcon(R.drawable.ic_info);
            getPreferenceScreen().addPreference(descPref);
        }
    }

    private void setupLights() {
        mLights.setDisabledByAdmin(mSuspendedAppsAdmin);
        mLights.setChecked(mChannel.shouldShowLights());
        mLights.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean lights = (Boolean) newValue;
                mChannel.enableLights(lights);
                mChannel.lockFields(NotificationChannel.USER_LOCKED_LIGHTS);
                mBackend.updateChannel(mPkg, mUid, mChannel);
                return true;
            }
        });
    }

    private void setupVibrate() {
        mVibrate.setDisabledByAdmin(mSuspendedAppsAdmin);
        mVibrate.setChecked(mChannel.shouldVibrate());
        mVibrate.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean vibrate = (Boolean) newValue;
                mChannel.enableVibration(vibrate);
                mChannel.lockFields(NotificationChannel.USER_LOCKED_VIBRATION);
                mBackend.updateChannel(mPkg, mUid, mChannel);
                return true;
            }
        });
    }

    private void setupRingtone() {
        mRingtone.setRingtone(mChannel.getSound());
        mRingtone.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mChannel.setSound((Uri) newValue, mChannel.getAudioAttributes());
                mChannel.lockFields(NotificationChannel.USER_LOCKED_SOUND);
                mBackend.updateChannel(mPkg, mUid, mChannel);
                return false;
            }
        });
    }

    protected void setupBlockAndImportance() {
        if (mAppRow.systemApp && mChannel.getImportance() != NotificationManager.IMPORTANCE_NONE) {
            setVisible(mBlock, false);
        } else {
            mBlock.setEnabled(mAppRow.systemApp);
            mBlock.setDisabledByAdmin(mSuspendedAppsAdmin);
            mBlock.setChecked(mChannel.getImportance() == NotificationManager.IMPORTANCE_NONE);
            mBlock.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final boolean value = (Boolean) newValue;
                    int importance = value ? IMPORTANCE_NONE : IMPORTANCE_LOW;
                    mImportance.setValue(String.valueOf(importance));
                    mChannel.setImportance(importance);
                    mChannel.lockFields(NotificationChannel.USER_LOCKED_IMPORTANCE);
                    mBackend.updateChannel(mPkg, mUid, mChannel);
                    updateDependents();
                    return true;
                }
            });
        }
        mBadge.setDisabledByAdmin(mSuspendedAppsAdmin);
        mBadge.setEnabled(mAppRow.showBadge);
        mBadge.setChecked(mChannel.canShowBadge());
        mBadge.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean value = (Boolean) newValue;
                mChannel.setShowBadge(value);
                mChannel.lockFields(NotificationChannel.USER_LOCKED_SHOW_BADGE);
                mBackend.updateChannel(mPkg, mUid, mChannel);
                return true;
            }
        });

        mImportance.setDisabledByAdmin(mSuspendedAppsAdmin);
        final int numImportances = IMPORTANCE_HIGH - IMPORTANCE_MIN + 1;
        List<String> summaries = new ArrayList<>();
        List<String> values = new ArrayList<>();

        for (int i = 0; i < numImportances; i++) {
            int importance = i + 1;
            summaries.add(getImportanceSummary(importance));
            values.add(String.valueOf(importance));
        }
        if (NotificationChannel.DEFAULT_CHANNEL_ID.equals(mChannel.getId())) {
            // Add option to reset to letting the app decide
            summaries.add(getImportanceSummary(NotificationManager.IMPORTANCE_UNSPECIFIED));
            values.add(String.valueOf(NotificationManager.IMPORTANCE_UNSPECIFIED));
        }
        mImportance.setEntryValues(values.toArray(new String[0]));
        mImportance.setEntries(summaries.toArray(new String[0]));
        mImportance.setValue(String.valueOf(mChannel.getImportance()));
        mImportance.setSummary(getImportanceSummary(mChannel.getImportance()));
        if (mAppRow.lockedImportance) {
            mImportance.setEnabled(false);
        } else {
            mImportance.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int importance = Integer.parseInt((String) newValue);
                    mChannel.setImportance(importance);
                    mChannel.lockFields(NotificationChannel.USER_LOCKED_IMPORTANCE);
                    mBackend.updateChannel(mPkg, mUid, mChannel);
                    updateDependents();
                    return true;
                }
            });
        }
    }

    private boolean isLockScreenSecure() {
        LockPatternUtils utils = new LockPatternUtils(getActivity());
        boolean lockscreenSecure = utils.isSecure(UserHandle.myUserId());
        UserInfo parentUser = mUm.getProfileParent(UserHandle.myUserId());
        if (parentUser != null){
            lockscreenSecure |= utils.isSecure(parentUser.id);
        }

        return lockscreenSecure;
    }

    protected boolean checkCanBeVisible(int minImportanceVisible) {
        int importance = mChannel.getImportance();
        if (importance == NotificationManager.IMPORTANCE_UNSPECIFIED) {
            return true;
        }
        return importance >= minImportanceVisible;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof RingtonePreference) {
            mRingtone.onPrepareRingtonePickerIntent(mRingtone.getIntent());
            startActivityForResult(preference.getIntent(), 200);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mRingtone != null) {
            mRingtone.onActivityResult(requestCode, resultCode, data);
        }
    }

    private boolean canPulseLight() {
        if (!getResources()
                .getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed)) {
            return false;
        }
        return Settings.System.getInt(getContentResolver(),
                Settings.System.NOTIFICATION_LIGHT_PULSE, 0) == 1;
    }

    private void updateDependents() {
        setVisible(mBadge, checkCanBeVisible(NotificationManager.IMPORTANCE_MIN));
        setVisible(mImportance, checkCanBeVisible(NotificationManager.IMPORTANCE_MIN));
        setVisible(mLights, checkCanBeVisible(
                NotificationManager.IMPORTANCE_LOW) && canPulseLight());
        setVisible(mVibrate, checkCanBeVisible(NotificationManager.IMPORTANCE_DEFAULT));
        setVisible(mRingtone, checkCanBeVisible(NotificationManager.IMPORTANCE_DEFAULT));
        setVisible(mPriority, checkCanBeVisible(NotificationManager.IMPORTANCE_DEFAULT)
                || (checkCanBeVisible(NotificationManager.IMPORTANCE_LOW)
                        && mDndVisualEffectsSuppressed));
        setVisible(mVisibilityOverride, checkCanBeVisible(NotificationManager.IMPORTANCE_MIN)
                && isLockScreenSecure());
    }
}
