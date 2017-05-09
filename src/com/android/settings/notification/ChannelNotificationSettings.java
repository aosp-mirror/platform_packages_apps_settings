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
import static android.app.NotificationManager.IMPORTANCE_UNSPECIFIED;

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

    private static final String KEY_LIGHTS = "lights";
    private static final String KEY_VIBRATE = "vibrate";
    private static final String KEY_RINGTONE = "ringtone";
    private static final String KEY_IMPORTANCE = "importance";

    private Preference mImportance;
    private RestrictedSwitchPreference mLights;
    private RestrictedSwitchPreference mVibrate;
    private NotificationSoundPreference mRingtone;
    private FooterPreference mFooter;

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
        addPreferencesFromResource(R.xml.notification_settings);
        setupBlock();
        addHeaderPref();
        addAppLinkPref();
        addFooterPref();

        if (NotificationChannel.DEFAULT_CHANNEL_ID.equals(mChannel.getId())) {
            populateDefaultChannelPrefs();
            mShowLegacyChannelConfig = true;
        } else {
            populateUpgradedChannelPrefs();
        }

        updateDependents(mChannel.getImportance() == IMPORTANCE_NONE);
    }

    private void populateUpgradedChannelPrefs() {
        addPreferencesFromResource(R.xml.upgraded_channel_notification_settings);
        setupBadge();
        setupPriorityPref(mChannel.canBypassDnd());
        setupVisOverridePref(mChannel.getLockscreenVisibility());
        setupLights();
        setupVibrate();
        setupRingtone();
        setupImportance();
    }

    private void addHeaderPref() {
        ArrayMap<String, NotificationBackend.AppRow> rows = new ArrayMap<String, NotificationBackend.AppRow>();
        rows.put(mAppRow.pkg, mAppRow);
        collectConfigActivities(rows);
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
    }

    private void addFooterPref() {
        if (!TextUtils.isEmpty(mChannel.getDescription())) {
            FooterPreference descPref = new FooterPreference(getPrefContext());
            descPref.setOrder(ORDER_LAST);
            descPref.setSelectable(false);
            descPref.setTitle(mChannel.getDescription());
            getPreferenceScreen().addPreference(descPref);
        }
    }

    protected void setupBadge() {
        mBadge = (RestrictedSwitchPreference) getPreferenceScreen().findPreference(KEY_BADGE);
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

    private void setupLights() {
        mLights = (RestrictedSwitchPreference) findPreference(KEY_LIGHTS);
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
        mVibrate = (RestrictedSwitchPreference) findPreference(KEY_VIBRATE);
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
        mRingtone = (NotificationSoundPreference) findPreference(KEY_RINGTONE);
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

    private void setupBlock() {
        View switchBarContainer = LayoutInflater.from(
                getPrefContext()).inflate(R.layout.styled_switch_bar, null);
        mSwitchBar = switchBarContainer.findViewById(R.id.switch_bar);
        mSwitchBar.show();
        mSwitchBar.setDisabledByAdmin(mSuspendedAppsAdmin);
        mSwitchBar.setChecked(mChannel.getImportance() != NotificationManager.IMPORTANCE_NONE);
        mSwitchBar.addOnSwitchChangeListener(new SwitchBar.OnSwitchChangeListener() {
            @Override
            public void onSwitchChanged(Switch switchView, boolean isChecked) {
                int importance = 0;
                if (mShowLegacyChannelConfig) {
                    importance = isChecked ? IMPORTANCE_UNSPECIFIED : IMPORTANCE_NONE;
                    mImportanceToggle.setChecked(importance == IMPORTANCE_UNSPECIFIED);
                } else {
                    importance = isChecked ? IMPORTANCE_LOW : IMPORTANCE_NONE;
                    mImportance.setSummary(getImportanceSummary(importance));
                }
                mChannel.setImportance(importance);
                mChannel.lockFields(NotificationChannel.USER_LOCKED_IMPORTANCE);
                mBackend.updateChannel(mPkg, mUid, mChannel);
                updateDependents(mChannel.getImportance() == IMPORTANCE_NONE);
            }
        });

        mBlockBar = new LayoutPreference(getPrefContext(), switchBarContainer);
        mBlockBar.setOrder(ORDER_FIRST);
        mBlockBar.setKey(KEY_BLOCK);
        getPreferenceScreen().addPreference(mBlockBar);

        if (mAppRow.systemApp && mChannel.getImportance() != NotificationManager.IMPORTANCE_NONE) {
            setVisible(mBlockBar, false);
        }

        setupBlockDesc(R.string.channel_notifications_off_desc);
    }

    private void setupImportance() {
        mImportance = findPreference(KEY_IMPORTANCE);
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

    private String getImportanceSummary(int importance) {
        String title;
        String summary = null;
        switch (importance) {
            case IMPORTANCE_UNSPECIFIED:
                title = getContext().getString(R.string.notification_importance_unspecified);
                break;
            case NotificationManager.IMPORTANCE_MIN:
                title = getContext().getString(R.string.notification_importance_min_title);
                summary = getContext().getString(R.string.notification_importance_min);
                break;
            case NotificationManager.IMPORTANCE_LOW:
                title = getContext().getString(R.string.notification_importance_low_title);
                summary = getContext().getString(R.string.notification_importance_low);
                break;
            case NotificationManager.IMPORTANCE_DEFAULT:
                title = getContext().getString(R.string.notification_importance_default_title);
                if (hasValidSound()) {
                    summary = getContext().getString(R.string.notification_importance_default);
                }
                break;
            case NotificationManager.IMPORTANCE_HIGH:
            case NotificationManager.IMPORTANCE_MAX:
                title = getContext().getString(R.string.notification_importance_high_title);
                if (hasValidSound()) {
                    summary = getContext().getString(R.string.notification_importance_high);
                }
                break;
            default:
                return "";
        }

        if (summary != null) {
            return getContext().getString(R.string.notification_importance_divider, title, summary);
        } else {
            return title;
        }
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
        mImportance.setSummary(getImportanceSummary(mChannel.getImportance()));
    }

    boolean canPulseLight() {
        if (!getResources()
                .getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed)) {
            return false;
        }
        return Settings.System.getInt(getContentResolver(),
                Settings.System.NOTIFICATION_LIGHT_PULSE, 0) == 1;
    }

    boolean hasValidSound() {
        return mChannel.getSound() != null && !Uri.EMPTY.equals(mChannel.getSound());
    }

    void updateDependents(boolean banned) {
        if (mShowLegacyChannelConfig) {
            setVisible(mImportanceToggle, checkCanBeVisible(NotificationManager.IMPORTANCE_MIN));
        } else {
            setVisible(mImportance, checkCanBeVisible(NotificationManager.IMPORTANCE_MIN));
            setVisible(mLights, checkCanBeVisible(
                    NotificationManager.IMPORTANCE_DEFAULT) && canPulseLight());
            setVisible(mVibrate, checkCanBeVisible(NotificationManager.IMPORTANCE_DEFAULT));
            setVisible(mRingtone, checkCanBeVisible(NotificationManager.IMPORTANCE_DEFAULT));
        }
        setVisible(mBadge, checkCanBeVisible(NotificationManager.IMPORTANCE_MIN));
        setVisible(mPriority, checkCanBeVisible(NotificationManager.IMPORTANCE_DEFAULT)
                || (checkCanBeVisible(NotificationManager.IMPORTANCE_LOW)
                && mDndVisualEffectsSuppressed));
        setVisible(mVisibilityOverride, checkCanBeVisible(NotificationManager.IMPORTANCE_LOW)
                && isLockScreenSecure());
        setVisible(mBlockedDesc, mChannel.getImportance() == IMPORTANCE_NONE);
        if (mAppLink != null) {
            setVisible(mAppLink, checkCanBeVisible(NotificationManager.IMPORTANCE_MIN));
        }
        if (mFooter !=null) {
            setVisible(mFooter, checkCanBeVisible(NotificationManager.IMPORTANCE_MIN));
        }
    }
}
