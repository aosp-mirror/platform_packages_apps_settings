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
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService.Ranking;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.AppHeader;
import com.android.settings.R;
import com.android.settings.RingtonePreference;
import com.android.settings.applications.AppHeaderController;
import com.android.settings.dashboard.DashboardFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;

import java.util.ArrayList;
import java.util.List;

public class ChannelNotificationSettings extends NotificationSettingsBase {
    private static final String TAG = "ChannelSettings";

    protected static final String KEY_BYPASS_DND = "bypass_dnd";
    protected static final String KEY_VISIBILITY_OVERRIDE = "visibility_override";
    protected static final String KEY_IMPORTANCE = "importance";
    protected static final String KEY_LIGHTS = "lights";
    protected static final String KEY_VIBRATE = "vibrate";
    protected static final String KEY_RINGTONE = "ringtone";

    protected RestrictedSwitchPreference mLights;
    protected RestrictedSwitchPreference mVibrate;
    protected DefaultNotificationTonePreference mRingtone;
    protected RestrictedDropDownPreference mImportance;
    protected RestrictedSwitchPreference mPriority;
    protected RestrictedDropDownPreference mVisibilityOverride;

    private DashboardFeatureProvider mDashboardFeatureProvider;

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
        if (mUid < 0 || TextUtils.isEmpty(mPkg) || mPkgInfo == null || mChannel == null) {
            Log.w(TAG, "Missing package or uid or packageinfo or channel");
            toastAndFinish();
            return;
        }
        final Activity activity = getActivity();
        mDashboardFeatureProvider =
                FeatureFactory.getFactory(activity).getDashboardFeatureProvider(activity);
        addPreferencesFromResource(R.xml.channel_notification_settings);

        mBlock = (RestrictedSwitchPreference) getPreferenceScreen().findPreference(KEY_BLOCK);
        mBadge = (RestrictedSwitchPreference) getPreferenceScreen().findPreference(KEY_BADGE);
        mImportance = (RestrictedDropDownPreference) findPreference(KEY_IMPORTANCE);
        mPriority =
                (RestrictedSwitchPreference) findPreference(KEY_BYPASS_DND);
        mVisibilityOverride =
                (RestrictedDropDownPreference) findPreference(KEY_VISIBILITY_OVERRIDE);
        mLights = (RestrictedSwitchPreference) findPreference(KEY_LIGHTS);
        mVibrate = (RestrictedSwitchPreference) findPreference(KEY_VIBRATE);
        mRingtone = (DefaultNotificationTonePreference) findPreference(KEY_RINGTONE);

        if (mPkgInfo != null && mChannel != null) {
            setupPriorityPref(mChannel.canBypassDnd());
            setupVisOverridePref(mChannel.getLockscreenVisibility());
            setupLights();
            setupVibrate();
            setupRingtone();
            setupBlockAndImportance();
            updateDependents();
        }
        if (mDashboardFeatureProvider.isEnabled()) {
            final Preference pref = FeatureFactory.getFactory(activity)
                    .getApplicationFeatureProvider(activity)
                    .newAppHeaderController(this /* fragment */, null /* appHeader */)
                    .setIcon(mAppRow.icon)
                    .setLabel(mChannel.getName())
                    .setSummary(mAppRow.label)
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
        if (mUid < 0 || TextUtils.isEmpty(mPkg) || mPkgInfo == null || mChannel == null) {
            Log.w(TAG, "Missing package or uid or packageinfo or channel");
            finish();
            return;
        }
        mLights.setDisabledByAdmin(mSuspendedAppsAdmin);
        mVibrate.setDisabledByAdmin(mSuspendedAppsAdmin);
        mImportance.setDisabledByAdmin(mSuspendedAppsAdmin);
        mPriority.setDisabledByAdmin(mSuspendedAppsAdmin);
        mVisibilityOverride.setDisabledByAdmin(mSuspendedAppsAdmin);
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
                Uri ringtone = Uri.parse((String) newValue);
                mRingtone.setRingtone(ringtone);
                mChannel.setSound(ringtone, mChannel.getAudioAttributes());
                mChannel.lockFields(NotificationChannel.USER_LOCKED_SOUND);
                mBackend.updateChannel(mPkg, mUid, mChannel);
                return false;
            }
        });
    }

    protected void setupBlockAndImportance() {
        mBlock.setDisabledByAdmin(mSuspendedAppsAdmin);
        mBlock.setChecked(mChannel.getImportance() == NotificationManager.IMPORTANCE_NONE);
        mBlock.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean value = (Boolean) newValue;
                int importance = value ?  IMPORTANCE_NONE : IMPORTANCE_LOW;
                mImportance.setValue(String.valueOf(importance));
                mChannel.setImportance(importance);
                mChannel.lockFields(NotificationChannel.USER_LOCKED_IMPORTANCE);
                mBackend.updateChannel(mPkg, mUid, mChannel);
                updateDependents();
                return true;
            }
        });
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
        List<String> values = new ArrayList<>();;
        for (int i = 0; i < numImportances; i++) {
            int importance = i + 1;
            summaries.add(getSummary(importance));
            values.add(String.valueOf(importance));
        }
        if (NotificationChannel.DEFAULT_CHANNEL_ID.equals(mChannel.getId())) {
            // Add option to reset to letting the app decide
            summaries.add(getSummary(NotificationManager.IMPORTANCE_UNSPECIFIED));
            values.add(String.valueOf(NotificationManager.IMPORTANCE_UNSPECIFIED));
        }
        mImportance.setEntryValues(values.toArray(new String[0]));
        mImportance.setEntries(summaries.toArray(new String[0]));
        mImportance.setValue(String.valueOf(mChannel.getImportance()));
        mImportance.setSummary("%s");

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

    private String getSummary(int importance) {
        switch (importance) {
            case NotificationManager.IMPORTANCE_UNSPECIFIED:
                return getContext().getString(R.string.notification_importance_unspecified);
            case NotificationManager.IMPORTANCE_NONE:
                return getContext().getString(R.string.notification_importance_blocked);
            case NotificationManager.IMPORTANCE_MIN:
                return getContext().getString(R.string.notification_importance_min);
            case NotificationManager.IMPORTANCE_LOW:
                return getContext().getString(R.string.notification_importance_low);
            case NotificationManager.IMPORTANCE_DEFAULT:
                return getContext().getString(R.string.notification_importance_default);
            case NotificationManager.IMPORTANCE_HIGH:
            case NotificationManager.IMPORTANCE_MAX:
            default:
                return getContext().getString(R.string.notification_importance_high);
        }
    }

    protected void setupPriorityPref(boolean priority) {
        mPriority.setDisabledByAdmin(mSuspendedAppsAdmin);
        mPriority.setChecked(priority);
        mPriority.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean bypassZenMode = (Boolean) newValue;
                mChannel.setBypassDnd(bypassZenMode);
                mChannel.lockFields(NotificationChannel.USER_LOCKED_PRIORITY);
                mBackend.updateChannel(mPkg, mUid, mChannel);
                return true;
            }
        });
    }

    protected void setupVisOverridePref(int sensitive) {
        ArrayList<CharSequence> entries = new ArrayList<>();
        ArrayList<CharSequence> values = new ArrayList<>();

        mVisibilityOverride.clearRestrictedItems();
        if (getLockscreenNotificationsEnabled() && getLockscreenAllowPrivateNotifications()) {
            final String summaryShowEntry =
                    getString(R.string.lock_screen_notifications_summary_show);
            final String summaryShowEntryValue =
                    Integer.toString(NotificationManager.VISIBILITY_NO_OVERRIDE);
            entries.add(summaryShowEntry);
            values.add(summaryShowEntryValue);
            setRestrictedIfNotificationFeaturesDisabled(summaryShowEntry, summaryShowEntryValue,
                    DevicePolicyManager.KEYGUARD_DISABLE_SECURE_NOTIFICATIONS
                            | DevicePolicyManager.KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS);
        }

        final String summaryHideEntry = getString(R.string.lock_screen_notifications_summary_hide);
        final String summaryHideEntryValue = Integer.toString(Notification.VISIBILITY_PRIVATE);
        entries.add(summaryHideEntry);
        values.add(summaryHideEntryValue);
        setRestrictedIfNotificationFeaturesDisabled(summaryHideEntry, summaryHideEntryValue,
                DevicePolicyManager.KEYGUARD_DISABLE_SECURE_NOTIFICATIONS);
        entries.add(getString(R.string.lock_screen_notifications_summary_disable));
        values.add(Integer.toString(Notification.VISIBILITY_SECRET));
        mVisibilityOverride.setEntries(entries.toArray(new CharSequence[entries.size()]));
        mVisibilityOverride.setEntryValues(values.toArray(new CharSequence[values.size()]));

        if (sensitive == Ranking.VISIBILITY_NO_OVERRIDE) {
            mVisibilityOverride.setValue(Integer.toString(getGlobalVisibility()));
        } else {
            mVisibilityOverride.setValue(Integer.toString(sensitive));
        }
        mVisibilityOverride.setSummary("%s");

        mVisibilityOverride.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        int sensitive = Integer.parseInt((String) newValue);
                        if (sensitive == getGlobalVisibility()) {
                            sensitive = Ranking.VISIBILITY_NO_OVERRIDE;
                        }
                        mChannel.setLockscreenVisibility(sensitive);
                        mChannel.lockFields(NotificationChannel.USER_LOCKED_VISIBILITY);
                        mBackend.updateChannel(mPkg, mUid, mChannel);
                        return true;
                    }
                });
    }

    private void setRestrictedIfNotificationFeaturesDisabled(CharSequence entry,
            CharSequence entryValue, int keyguardNotificationFeatures) {
        RestrictedLockUtils.EnforcedAdmin admin =
                RestrictedLockUtils.checkIfKeyguardFeaturesDisabled(
                        mContext, keyguardNotificationFeatures, mUserId);
        if (admin != null) {
            RestrictedDropDownPreference.RestrictedItem item =
                    new RestrictedDropDownPreference.RestrictedItem(entry, entryValue, admin);
            mVisibilityOverride.addRestrictedItem(item);
        }
    }

    private int getGlobalVisibility() {
        int globalVis = Ranking.VISIBILITY_NO_OVERRIDE;
        if (!getLockscreenNotificationsEnabled()) {
            globalVis = Notification.VISIBILITY_SECRET;
        } else if (!getLockscreenAllowPrivateNotifications()) {
            globalVis = Notification.VISIBILITY_PRIVATE;
        }
        return globalVis;
    }

    private boolean getLockscreenNotificationsEnabled() {
        return Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 0) != 0;
    }

    private boolean getLockscreenAllowPrivateNotifications() {
        return Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 0) != 0;
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
