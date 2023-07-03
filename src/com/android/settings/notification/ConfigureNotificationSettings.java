/*
 * Copyright (C) 2015 The Android Open Source Project
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

import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_LOCK_SCREEN_REDACT_NOTIFICATION_SUMMARY;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_LOCK_SCREEN_REDACT_NOTIFICATION_TITLE;

import android.app.Activity;
import android.app.Application;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.RingtonePreference;
import com.android.settings.core.OnActivityResultListener;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class ConfigureNotificationSettings extends DashboardFragment implements
        OnActivityResultListener {
    private static final String TAG = "ConfigNotiSettings";

    @VisibleForTesting
    static final String KEY_SWIPE_DOWN = "gesture_swipe_down_fingerprint_notifications";
    static final String KEY_LOCKSCREEN = "lock_screen_notifications";

    private static final String KEY_NOTI_DEFAULT_RINGTONE = "notification_default_ringtone";
    private static final int REQUEST_CODE = 200;
    private static final String SELECTED_PREFERENCE_KEY = "selected_preference";
    private static final String KEY_ADVANCED_CATEGORY = "configure_notifications_advanced";

    private RingtonePreference mRequestPreference;

    private NotificationAssistantPreferenceController mNotificationAssistantPreferenceController;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.CONFIGURE_NOTIFICATION;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        replaceEnterpriseStringTitle("lock_screen_work_redact",
                WORK_PROFILE_LOCK_SCREEN_REDACT_NOTIFICATION_TITLE,
                R.string.lock_screen_notifs_redact_work);
        replaceEnterpriseStringSummary("lock_screen_work_redact",
                WORK_PROFILE_LOCK_SCREEN_REDACT_NOTIFICATION_SUMMARY,
                R.string.lock_screen_notifs_redact_work_summary);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.configure_notification_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final Activity activity = getActivity();
        final Application app;
        if (activity != null) {
            app = activity.getApplication();
        } else {
            app = null;
        }
        return buildPreferenceControllers(context, app, this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mNotificationAssistantPreferenceController =
                use(NotificationAssistantPreferenceController.class);
        mNotificationAssistantPreferenceController.setFragment(this);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Application app, Fragment host) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new ShowOnLockScreenNotificationPreferenceController(
                context, KEY_LOCKSCREEN));
        controllers.add(new NotificationRingtonePreferenceController(context) {
            @Override
            public String getPreferenceKey() {
                return KEY_NOTI_DEFAULT_RINGTONE;
            }

        });
        controllers.add(new EmergencyBroadcastPreferenceController(context,
                "app_and_notif_cell_broadcast_settings"));

        return controllers;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof RingtonePreference) {
            writePreferenceClickMetric(preference);
            mRequestPreference = (RingtonePreference) preference;
            mRequestPreference.onPrepareRingtonePickerIntent(mRequestPreference.getIntent());
            getActivity().startActivityForResultAsUser(
                    mRequestPreference.getIntent(),
                    REQUEST_CODE,
                    null,
                    UserHandle.of(mRequestPreference.getUserId()));
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mRequestPreference != null) {
            mRequestPreference.onActivityResult(requestCode, resultCode, data);
            mRequestPreference = null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mRequestPreference != null) {
            outState.putString(SELECTED_PREFERENCE_KEY, mRequestPreference.getKey());
        }
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.configure_notification_settings) {

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null, null);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);
                    keys.add(KEY_SWIPE_DOWN);
                    return keys;
                }
            };

    // Dialogs only have access to the parent fragment, not the controller, so pass the information
    // along to keep business logic out of this file
    protected void enableNAS(ComponentName cn) {
        final PreferenceScreen screen = getPreferenceScreen();
        NotificationAssistantPreferenceController napc =
                use(NotificationAssistantPreferenceController.class);
        napc.setNotificationAssistantGranted(cn);
        napc.updateState(screen.findPreference(napc.getPreferenceKey()));
    }
}
