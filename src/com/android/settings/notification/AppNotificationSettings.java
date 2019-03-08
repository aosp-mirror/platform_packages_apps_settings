/*
 * Copyright (C) 2014 The Android Open Source Project
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

import static android.view.WindowManager.LayoutParams.PRIVATE_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.widget.MasterCheckBoxPreference;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** These settings are per app, so should not be returned in global search results. */
public class AppNotificationSettings extends NotificationSettingsBase {
    private static final String TAG = "AppNotificationSettings";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static String KEY_GENERAL_CATEGORY = "categories";
    private static String KEY_ADVANCED_CATEGORY = "app_advanced";
    private static String KEY_BADGE = "badge";
    private static String KEY_APP_LINK = "app_link";

    private List<NotificationChannelGroup> mChannelGroupList;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.NOTIFICATION_APP_NOTIFICATION;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final PreferenceScreen screen = getPreferenceScreen();
        if (mShowLegacyChannelConfig && screen != null) {
            // if showing legacy settings, pull advanced settings out of the advanced category
            Preference badge = findPreference(KEY_BADGE);
            Preference appLink = findPreference(KEY_APP_LINK);
            removePreference(KEY_ADVANCED_CATEGORY);
            if (badge != null) {
                screen.addPreference(badge);

            }
            if (appLink != null) {
                screen.addPreference(appLink);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        getActivity().getWindow().addPrivateFlags(PRIVATE_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);
        android.util.EventLog.writeEvent(0x534e4554, "119115683", -1, "");

        if (mUid < 0 || TextUtils.isEmpty(mPkg) || mPkgInfo == null) {
            Log.w(TAG, "Missing package or uid or packageinfo");
            finish();
            return;
        }

        if (!mShowLegacyChannelConfig) {
            // Load channel settings
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... unused) {
                    mChannelGroupList = mBackend.getGroups(mPkg, mUid).getList();
                    Collections.sort(mChannelGroupList, mChannelGroupComparator);
                    return null;
                }

                @Override
                protected void onPostExecute(Void unused) {
                    if (getHost() == null) {
                        return;
                    }
                    populateList();
                }
            }.execute();
        }

        for (NotificationPreferenceController controller : mControllers) {
            controller.onResume(mAppRow, mChannel, mChannelGroup, mSuspendedAppsAdmin);
            controller.displayPreference(getPreferenceScreen());
        }
        updatePreferenceStates();
    }

    @Override
    public void onPause() {
        super.onPause();
        final Window window = getActivity().getWindow();
        final WindowManager.LayoutParams attrs = window.getAttributes();
        attrs.privateFlags &= ~PRIVATE_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS;
        window.setAttributes(attrs);
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.app_notification_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        mControllers = new ArrayList<>();
        mControllers.add(new HeaderPreferenceController(context, this));
        mControllers.add(new BlockPreferenceController(context, mImportanceListener, mBackend));
        mControllers.add(new BadgePreferenceController(context, mBackend));
        mControllers.add(new AllowSoundPreferenceController(
                context, mImportanceListener, mBackend));
        mControllers.add(new ImportancePreferenceController(
                context, mImportanceListener, mBackend));
        mControllers.add(new SoundPreferenceController(context, this,
                mImportanceListener, mBackend));
        mControllers.add(new LightsPreferenceController(context, mBackend));
        mControllers.add(new VibrationPreferenceController(context, mBackend));
        mControllers.add(new VisibilityPreferenceController(context, new LockPatternUtils(context),
                mBackend));
        mControllers.add(new DndPreferenceController(context, mBackend));
        mControllers.add(new AppLinkPreferenceController(context));
        mControllers.add(new DescriptionPreferenceController(context));
        mControllers.add(new NotificationsOffPreferenceController(context));
        mControllers.add(new DeletedChannelsPreferenceController(context, mBackend));
        return new ArrayList<>(mControllers);
    }

    private void populateList() {
        if (!mDynamicPreferences.isEmpty()) {
            for (Preference p : mDynamicPreferences) {
                getPreferenceScreen().removePreference(p);
            }
            mDynamicPreferences.clear();
        }
        if (mChannelGroupList.isEmpty()) {
            PreferenceCategory groupCategory = new PreferenceCategory(getPrefContext());
            groupCategory.setTitle(R.string.notification_channels);
            groupCategory.setKey(KEY_GENERAL_CATEGORY);
            getPreferenceScreen().addPreference(groupCategory);
            mDynamicPreferences.add(groupCategory);

            Preference empty = new Preference(getPrefContext());
            empty.setTitle(R.string.no_channels);
            empty.setEnabled(false);
            groupCategory.addPreference(empty);
        } else {
            populateGroupList();
            mImportanceListener.onImportanceChanged();
        }
    }

    private void populateGroupList() {
        for (NotificationChannelGroup group : mChannelGroupList) {
            PreferenceCategory groupCategory = new PreferenceCategory(getPrefContext());
            groupCategory.setOrderingAsAdded(true);
            getPreferenceScreen().addPreference(groupCategory);
            mDynamicPreferences.add(groupCategory);
            if (group.getId() == null) {
                if (mChannelGroupList.size() > 1) {
                    groupCategory.setTitle(R.string.notification_channels_other);
                }
                groupCategory.setKey(KEY_GENERAL_CATEGORY);
            } else {
                groupCategory.setTitle(group.getName());
                groupCategory.setKey(group.getId());
                populateGroupToggle(groupCategory, group);
            }
            if (!group.isBlocked()) {
                final List<NotificationChannel> channels = group.getChannels();
                Collections.sort(channels, mChannelComparator);
                int N = channels.size();
                for (int i = 0; i < N; i++) {
                    final NotificationChannel channel = channels.get(i);
                    populateSingleChannelPrefs(groupCategory, channel, group.isBlocked());
                }
            }
        }
    }

    protected void populateGroupToggle(final PreferenceGroup parent,
            NotificationChannelGroup group) {
        RestrictedSwitchPreference preference = new RestrictedSwitchPreference(getPrefContext());
        preference.setTitle(R.string.notification_switch_label);
        preference.setEnabled(mSuspendedAppsAdmin == null
                && isChannelGroupBlockable(group));
        preference.setChecked(!group.isBlocked());
        preference.setOnPreferenceClickListener(preference1 -> {
            final boolean allowGroup = ((SwitchPreference) preference1).isChecked();
            group.setBlocked(!allowGroup);
            mBackend.updateChannelGroup(mAppRow.pkg, mAppRow.uid, group);

            onGroupBlockStateChanged(group);
            return true;
        });

        parent.addPreference(preference);
    }

    private Comparator<NotificationChannelGroup> mChannelGroupComparator =
            new Comparator<NotificationChannelGroup>() {

                @Override
                public int compare(NotificationChannelGroup left, NotificationChannelGroup right) {
                    // Non-grouped channels (in placeholder group with a null id) come last
                    if (left.getId() == null && right.getId() != null) {
                        return 1;
                    } else if (right.getId() == null && left.getId() != null) {
                        return -1;
                    }
                    return left.getId().compareTo(right.getId());
                }
            };

    protected void onGroupBlockStateChanged(NotificationChannelGroup group) {
        if (group == null) {
            return;
        }
        PreferenceGroup groupGroup = (
                PreferenceGroup) getPreferenceScreen().findPreference(group.getId());

        if (groupGroup != null) {
            if (group.isBlocked()) {
                List<Preference> toRemove = new ArrayList<>();
                int childCount = groupGroup.getPreferenceCount();
                for (int i = 0; i < childCount; i++) {
                    Preference pref = groupGroup.getPreference(i);
                    if (pref instanceof MasterCheckBoxPreference) {
                        toRemove.add(pref);
                    }
                }
                for (Preference pref : toRemove) {
                    groupGroup.removePreference(pref);
                }
            } else {
                final List<NotificationChannel> channels = group.getChannels();
                Collections.sort(channels, mChannelComparator);
                int N = channels.size();
                for (int i = 0; i < N; i++) {
                    final NotificationChannel channel = channels.get(i);
                    populateSingleChannelPrefs(groupGroup, channel, group.isBlocked());
                }
            }
        }
    }

}
