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

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService.Ranking;
import android.support.v7.preference.Preference;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.AppHeader;
import com.android.settings.R;
import com.android.settings.RingtonePreference;
import com.android.settings.applications.AppHeaderController;
import com.android.settings.dashboard.DashboardFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;

public class ChannelNotificationSettings extends NotificationSettingsBase {
    protected static final String KEY_LIGHTS = "lights";
    protected static final String KEY_VIBRATE = "vibrate";
    protected static final String KEY_RINGTONE = "ringtone";

    protected RestrictedSwitchPreference mLights;
    protected RestrictedSwitchPreference mVibrate;
    protected DefaultNotificationTonePreference mRingtone;

    private DashboardFeatureProvider mDashboardFeatureProvider;
    private int mMaxImportance;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mAppRow == null || mChannel == null) return;
        if (!mDashboardFeatureProvider.isEnabled()) {
            AppHeader.createAppHeader(
                    this, mAppRow.icon, mChannel.getName(), mAppRow.pkg, mAppRow.uid);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.NOTIFICATION_TOPIC_NOTIFICATION;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity();
        mDashboardFeatureProvider =
                FeatureFactory.getFactory(activity).getDashboardFeatureProvider(activity);
        addPreferencesFromResource(R.xml.channel_notification_settings);

        mImportance = (ImportanceSeekBarPreference) findPreference(KEY_IMPORTANCE);
        mPriority =
                (RestrictedSwitchPreference) findPreference(KEY_BYPASS_DND);
        mVisibilityOverride =
                (RestrictedDropDownPreference) findPreference(KEY_VISIBILITY_OVERRIDE);
        mBlock = (RestrictedSwitchPreference) findPreference(KEY_BLOCK);
        mSilent = (RestrictedSwitchPreference) findPreference(KEY_SILENT);
        mLights = (RestrictedSwitchPreference) findPreference(KEY_LIGHTS);
        mVibrate = (RestrictedSwitchPreference) findPreference(KEY_VIBRATE);
        mRingtone = (DefaultNotificationTonePreference) findPreference(KEY_RINGTONE);

        if (mPkgInfo != null) {
            setupPriorityPref(mChannel.canBypassDnd());
            if (mAppRow.appBypassDnd) {
                mPriority.setShouldDisableView(true);
            }
            setupVisOverridePref(mChannel.getLockscreenVisibility());
            if (mAppRow.appVisOverride != Ranking.VISIBILITY_NO_OVERRIDE) {
                mVisibilityOverride.setShouldDisableView(true);
            }
            setupLights();
            setupVibrate();
            setupRingtone();
            mMaxImportance = mAppRow.appImportance == NotificationManager.IMPORTANCE_UNSPECIFIED
                    ? NotificationManager.IMPORTANCE_HIGH : mAppRow.appImportance;
            setupImportancePrefs(false, mChannel.getImportance(),
                    mChannel.getImportance() == NotificationManager.IMPORTANCE_NONE,
                    mMaxImportance);
        }
        if (mDashboardFeatureProvider.isEnabled()) {
            final Preference pref = FeatureFactory.getFactory(activity)
                    .getApplicationFeatureProvider(activity)
                    .newAppHeaderController(this /* fragment */, null /* appHeader */)
                    .setIcon(mAppRow.icon)
                    .setLabel(mAppRow.label)
                    .setSummary(mChannel.getName())
                    .setPackageName(mAppRow.pkg)
                    .setUid(mAppRow.uid)
                    .setButtonActions(AppHeaderController.ActionType.ACTION_APP_INFO,
                            AppHeaderController.ActionType.ACTION_NONE)
                    .done(getPrefContext());
            getPreferenceScreen().addPreference(pref);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((mUid != -1 && getPackageManager().getPackagesForUid(mUid) == null)) {
            // App isn't around anymore, must have been removed.
            finish();
            return;
        }
        mSuspendedAppsAdmin = RestrictedLockUtils.checkIfApplicationIsSuspended(
                mContext, mPkg, mUserId);
        if (mLights != null) {
            mLights.setDisabledByAdmin(mSuspendedAppsAdmin);
        }
        if (mVibrate != null) {
            mVibrate.setDisabledByAdmin(mSuspendedAppsAdmin);
        }
    }

    private void setupLights() {
        mLights.setDisabledByAdmin(mSuspendedAppsAdmin);
        mLights.setChecked(mChannel.shouldShowLights());
        mLights.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean lights = (Boolean) newValue;
                mChannel.setLights(lights);
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
                Uri ringtone = Uri.parse((String) newValue);
                mRingtone.setRingtone(ringtone);
                mChannel.setSound(ringtone);
                mChannel.lockFields(NotificationChannel.USER_LOCKED_SOUND);
                mBackend.updateChannel(mPkg, mUid, mChannel);
                return false;
            }
        });
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

    @Override
    protected void updateDependents(int importance) {
        if (importance == NotificationManager.IMPORTANCE_UNSPECIFIED) {
            importance = mMaxImportance;
        }
        importance = Math.min(mMaxImportance, importance);

        super.updateDependents(importance);
        setVisible(mLights, checkCanBeVisible(
                NotificationManager.IMPORTANCE_LOW, importance) && canPulseLight());
        setVisible(mVibrate, checkCanBeVisible(NotificationManager.IMPORTANCE_DEFAULT, importance));
        setVisible(mRingtone, checkCanBeVisible(
                NotificationManager.IMPORTANCE_DEFAULT, importance));
        if (mMaxImportance == NotificationManager.IMPORTANCE_LOW
                && getPreferenceScreen().findPreference(mBlock.getKey()) != null) {
            setVisible(mSilent, false);
        }
    }
}
