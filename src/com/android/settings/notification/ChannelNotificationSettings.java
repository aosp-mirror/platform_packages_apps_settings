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
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.text.TextUtils;
import android.text.BidiFormatter;
import android.text.SpannableStringBuilder;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Switch;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.RingtonePreference;
import com.android.settings.Utils;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.widget.EntityHeaderController;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.widget.FooterPreference;

import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_NONE;
import static android.app.NotificationManager.IMPORTANCE_UNSPECIFIED;

public class ChannelNotificationSettings extends NotificationSettingsBase {
    private static final String TAG = "ChannelSettings";

    private static final String KEY_LIGHTS = "lights";
    private static final String KEY_VIBRATE = "vibrate";
    private static final String KEY_RINGTONE = "ringtone";
    private static final String KEY_IMPORTANCE = "importance";
    private static final String KEY_ADVANCED = "advanced";

    private Preference mImportance;
    private RestrictedSwitchPreference mLights;
    private RestrictedSwitchPreference mVibrate;
    private NotificationSoundPreference mRingtone;
    private FooterPreference mFooter;
    private NotificationChannelGroup mChannelGroup;
    private EntityHeaderController mHeaderPref;
    private PreferenceGroup mAdvanced;

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

            if (mChannel.getGroup() != null) {
                // Go look up group name
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... unused) {
                        if (mChannel.getGroup() != null) {
                            mChannelGroup = mBackend.getGroup(mChannel.getGroup(), mPkg, mUid);
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void unused) {
                        if (getHost() == null || mChannelGroup == null) {
                            return;
                        }
                        setChannelGroupLabel(mChannelGroup.getName());
                    }
                }.execute();
            }
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
        mAdvanced = (PreferenceGroup) findPreference(KEY_ADVANCED);
    }

    private void addHeaderPref() {
        ArrayMap<String, NotificationBackend.AppRow> rows = new ArrayMap<>();
        rows.put(mAppRow.pkg, mAppRow);
        collectConfigActivities(rows);
        final Activity activity = getActivity();
        mHeaderPref = EntityHeaderController
                .newInstance(activity, this /* fragment */, null /* header */)
                .setRecyclerView(getListView(), getLifecycle());
        final Preference pref = mHeaderPref
                .setIcon(mAppRow.icon)
                .setLabel(mChannel.getName())
                .setSummary(mAppRow.label)
                .setPackageName(mAppRow.pkg)
                .setUid(mAppRow.uid)
                .setButtonActions(EntityHeaderController.ActionType.ACTION_NOTIF_PREFERENCE,
                        EntityHeaderController.ActionType.ACTION_NONE)
                .setHasAppInfoLink(true)
                .done(activity, getPrefContext());
        getPreferenceScreen().addPreference(pref);
    }

    private void setChannelGroupLabel(CharSequence groupName) {
        final SpannableStringBuilder summary = new SpannableStringBuilder();
        BidiFormatter bidi = BidiFormatter.getInstance();
        summary.append(bidi.unicodeWrap(mAppRow.label.toString()));
        if (groupName != null) {
            summary.append(bidi.unicodeWrap(mContext.getText(
                    R.string.notification_header_divider_symbol_with_spaces)));
            summary.append(bidi.unicodeWrap(groupName.toString()));
        }
        final Activity activity = getActivity();
        mHeaderPref.setSummary(summary.toString());
        mHeaderPref.done(activity, getPrefContext());
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
        mVibrate.setEnabled(!mVibrate.isDisabledByAdmin() && isChannelConfigurable(mChannel));
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
        mRingtone.setEnabled(isChannelConfigurable(mChannel));
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

        if (!isChannelBlockable(mAppRow.systemApp, mChannel)) {
            setVisible(mBlockBar, false);
        }

        setupBlockDesc(R.string.channel_notifications_off_desc);
    }

    private void setupImportance() {
        mImportance = findPreference(KEY_IMPORTANCE);
        Bundle channelArgs = new Bundle();
        channelArgs.putInt(AppInfoBase.ARG_PACKAGE_UID, mUid);
        channelArgs.putString(AppInfoBase.ARG_PACKAGE_NAME, mPkg);
        channelArgs.putString(Settings.EXTRA_CHANNEL_ID, mChannel.getId());
        mImportance.setEnabled(mSuspendedAppsAdmin == null && isChannelConfigurable(mChannel));
        // Set up intent to show importance selection only if this setting is enabled.
        if (mImportance.isEnabled()) {
            Intent channelIntent = Utils.onBuildStartFragmentIntent(getActivity(),
                    ChannelImportanceSettings.class.getName(),
                    channelArgs, null, R.string.notification_importance_title, null,
                    false, getMetricsCategory());
            mImportance.setIntent(channelIntent);
        }
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
                if (hasValidSound(mChannel)) {
                    summary = getContext().getString(R.string.notification_importance_default);
                } else {
                    summary = getContext().getString(R.string.notification_importance_low);
                }
                break;
            case NotificationManager.IMPORTANCE_HIGH:
            case NotificationManager.IMPORTANCE_MAX:
                title = getContext().getString(R.string.notification_importance_high_title);
                if (hasValidSound(mChannel)) {
                    summary = getContext().getString(R.string.notification_importance_high);
                } else {
                    summary = getContext().getString(R.string.notification_importance_high_silent);
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
        if (mChannel != null) {
            mImportance.setSummary(getImportanceSummary(mChannel.getImportance()));
        }
    }

    boolean canPulseLight() {
        if (!getResources()
                .getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed)) {
            return false;
        }
        return Settings.System.getInt(getContentResolver(),
                Settings.System.NOTIFICATION_LIGHT_PULSE, 0) == 1;
    }

    void updateDependents(boolean banned) {
        PreferenceGroup parent;
        if (mShowLegacyChannelConfig) {
            parent = getPreferenceScreen();
            setVisible(mImportanceToggle, checkCanBeVisible(NotificationManager.IMPORTANCE_MIN));
        } else {
            setVisible(mAdvanced, checkCanBeVisible(NotificationManager.IMPORTANCE_MIN));
            setVisible(mImportance, checkCanBeVisible(NotificationManager.IMPORTANCE_MIN));
            setVisible(mAdvanced, mLights, checkCanBeVisible(
                    NotificationManager.IMPORTANCE_DEFAULT) && canPulseLight());
            setVisible(mVibrate, checkCanBeVisible(NotificationManager.IMPORTANCE_DEFAULT));
            setVisible(mRingtone, checkCanBeVisible(NotificationManager.IMPORTANCE_DEFAULT));
            parent = mAdvanced;
        }
        setVisible(parent, mBadge, checkCanBeVisible(NotificationManager.IMPORTANCE_MIN));
        setVisible(parent, mPriority, checkCanBeVisible(NotificationManager.IMPORTANCE_DEFAULT)
                || (checkCanBeVisible(NotificationManager.IMPORTANCE_LOW)
                && mDndVisualEffectsSuppressed));
        setVisible(parent, mVisibilityOverride, isLockScreenSecure()
                &&checkCanBeVisible(NotificationManager.IMPORTANCE_LOW));
        setVisible(mBlockedDesc, mChannel.getImportance() == IMPORTANCE_NONE);
        if (mAppLink != null) {
            setVisible(mAppLink, checkCanBeVisible(NotificationManager.IMPORTANCE_MIN));
        }
        if (mFooter != null) {
            setVisible(mFooter, checkCanBeVisible(NotificationManager.IMPORTANCE_MIN));
        }
    }
}
