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

package com.android.settings.notification.app;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

/** These settings are per app, so should not be returned in global search results. */
public class AppNotificationSettings extends NotificationSettings {
    private static final String TAG = "AppNotificationSettings";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static String KEY_ADVANCED_CATEGORY = "app_advanced";
    private static String KEY_BADGE = "badge";
    private static String KEY_APP_LINK = "app_link";
    private static String[] LEGACY_NON_ADVANCED_KEYS = {KEY_BADGE, KEY_APP_LINK};

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.NOTIFICATION_APP_NOTIFICATION;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final PreferenceScreen screen = getPreferenceScreen();
        if (mShowLegacyChannelConfig && screen != null) {
            // if showing legacy settings, pull advanced settings out of the advanced category
            PreferenceGroup advanced = (PreferenceGroup) findPreference(KEY_ADVANCED_CATEGORY);
            removePreference(KEY_ADVANCED_CATEGORY);
            if (advanced != null) {
                for (String key : LEGACY_NON_ADVANCED_KEYS) {
                    Preference pref = advanced.findPreference(key);
                    advanced.removePreference(pref);
                    if (pref != null) {
                        screen.addPreference(pref);
                    }
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mUid < 0 || TextUtils.isEmpty(mPkg) || mPkgInfo == null) {
            Log.w(TAG, "Missing package or uid or packageinfo");
            finish();
            return;
        }

        for (NotificationPreferenceController controller : mControllers) {
            controller.onResume(mAppRow, mChannel, mChannelGroup, null, null, mSuspendedAppsAdmin);
            controller.displayPreference(getPreferenceScreen());
        }
        updatePreferenceStates();
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
        mControllers.add(new BlockPreferenceController(context, mDependentFieldListener, mBackend));
        mControllers.add(new BadgePreferenceController(context, mBackend));
        mControllers.add(new AllowSoundPreferenceController(
                context, mDependentFieldListener, mBackend));
        mControllers.add(new ImportancePreferenceController(
                context, mDependentFieldListener, mBackend));
        mControllers.add(new MinImportancePreferenceController(
                context, mDependentFieldListener, mBackend));
        mControllers.add(new HighImportancePreferenceController(
                context, mDependentFieldListener, mBackend));
        mControllers.add(new SoundPreferenceController(context, this,
                mDependentFieldListener, mBackend));
        mControllers.add(new LightsPreferenceController(context, mBackend));
        mControllers.add(new VibrationPreferenceController(context, mBackend));
        mControllers.add(new VisibilityPreferenceController(context, new LockPatternUtils(context),
                mBackend));
        mControllers.add(new DndPreferenceController(context, mBackend));
        mControllers.add(new AppLinkPreferenceController(context));
        mControllers.add(new DescriptionPreferenceController(context));
        mControllers.add(new NotificationsOffPreferenceController(context));
        mControllers.add(new DeletedChannelsPreferenceController(context, mBackend));
        mControllers.add(new ChannelListPreferenceController(context, mBackend));
        mControllers.add(new AppConversationListPreferenceController(context, mBackend));
        mControllers.add(new InvalidConversationInfoPreferenceController(context, mBackend));
        mControllers.add(new InvalidConversationPreferenceController(context, mBackend));
        mControllers.add(new BubbleSummaryPreferenceController(context, mBackend));
        return new ArrayList<>(mControllers);
    }
}
