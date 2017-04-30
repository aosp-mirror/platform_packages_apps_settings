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

import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_NONE;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Switch;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.AppHeader;
import com.android.settings.R;
import com.android.settings.RingtonePreference;
import com.android.settings.Utils;
import com.android.settings.applications.AppHeaderController;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.FooterPreference;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.RestrictedSwitchPreference;

public class ChannelNotificationSettings extends NotificationSettingsBase {
    private static final String TAG = "ChannelSettings";

    protected static final String KEY_LIGHTS = "lights";
    protected static final String KEY_VIBRATE = "vibrate";
    protected static final String KEY_RINGTONE = "ringtone";

    protected Preference mImportance;
    protected RestrictedSwitchPreference mLights;
    protected RestrictedSwitchPreference mVibrate;
    protected NotificationSoundPreference mRingtone;

    protected LayoutPreference mBlockBar;

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

        // load settings intent
        ArrayMap<String, NotificationBackend.AppRow> rows = new ArrayMap<String, NotificationBackend.AppRow>();
        rows.put(mAppRow.pkg, mAppRow);
        collectConfigActivities(rows);

        mBlockedDesc = (FooterPreference) getPreferenceScreen().findPreference(KEY_BLOCKED_DESC);
        mBadge = (RestrictedSwitchPreference) getPreferenceScreen().findPreference(KEY_BADGE);
        mImportance = findPreference(KEY_IMPORTANCE);
        mPriority = (RestrictedSwitchPreference) findPreference(KEY_BYPASS_DND);
        mVisibilityOverride =
                (RestrictedDropDownPreference) findPreference(KEY_VISIBILITY_OVERRIDE);
        mLights = (RestrictedSwitchPreference) findPreference(KEY_LIGHTS);
        mVibrate = (RestrictedSwitchPreference) findPreference(KEY_VIBRATE);
        mRingtone = (NotificationSoundPreference) findPreference(KEY_RINGTONE);


        setupPriorityPref(mChannel.canBypassDnd());
        setupVisOverridePref(mChannel.getLockscreenVisibility());
        setupLights();
        setupVibrate();
        setupRingtone();
        setupBadge();
        setupBlock();
        setupImportance();
        updateDependents();

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
            FooterPreference descPref = new FooterPreference(getPrefContext());
            descPref.setSelectable(false);
            descPref.setSummary(mChannel.getDescription());
            descPref.setEnabled(false);
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

    protected void setupBlock() {
        View switchBarContainer = LayoutInflater.from(
                getPrefContext()).inflate(R.layout.styled_switch_bar, null);
        SwitchBar switchBar = switchBarContainer.findViewById(R.id.switch_bar);
        switchBar.show();
        switchBar.setDisabledByAdmin(mSuspendedAppsAdmin);
        switchBar.setChecked(mChannel.getImportance() != NotificationManager.IMPORTANCE_NONE);
        switchBar.addOnSwitchChangeListener(new SwitchBar.OnSwitchChangeListener() {
            @Override
            public void onSwitchChanged(Switch switchView, boolean isChecked) {
                int importance = isChecked ? IMPORTANCE_LOW : IMPORTANCE_NONE;
                mImportance.setSummary(getImportanceSummary(importance));
                mChannel.setImportance(importance);
                mChannel.lockFields(NotificationChannel.USER_LOCKED_IMPORTANCE);
                mBackend.updateChannel(mPkg, mUid, mChannel);
                updateDependents();
            }
        });

        mBlockBar = new LayoutPreference(getPrefContext(), switchBarContainer);
        mBlockBar.setOrder(-500);
        mBlockBar.setKey(KEY_BLOCK);
        getPreferenceScreen().addPreference(mBlockBar);

        if (mAppRow.systemApp && mChannel.getImportance() != NotificationManager.IMPORTANCE_NONE) {
            setVisible(mBlockBar, false);
        }

        setupBlockDesc(R.string.channel_notifications_off_desc);
    }

    protected void setupBadge() {
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
    }

    protected void setupImportance() {
        Bundle channelArgs = new Bundle();
        channelArgs.putInt(AppInfoBase.ARG_PACKAGE_UID, mUid);
        channelArgs.putBoolean(AppHeader.EXTRA_HIDE_INFO_BUTTON, true);
        channelArgs.putString(AppInfoBase.ARG_PACKAGE_NAME, mPkg);
        channelArgs.putString(Settings.EXTRA_CHANNEL_ID, mChannel.getId());
        Intent channelIntent = Utils.onBuildStartFragmentIntent(getActivity(),
                ChannelImportanceSettings.class.getName(),
                channelArgs, null, R.string.notification_importance_title, null,
                false, getMetricsCategory());
        mImportance.setIntent(channelIntent);
        mImportance.setEnabled(mSuspendedAppsAdmin == null);
        mImportance.setSummary(getImportanceSummary(mChannel.getImportance()));
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
        setVisible(mBlockedDesc, mChannel.getImportance() == IMPORTANCE_NONE);
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
