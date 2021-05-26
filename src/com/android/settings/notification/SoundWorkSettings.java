/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.RingtonePreference;
import com.android.settings.core.OnActivityResultListener;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/** Sounds settings for work profile. */
@SearchIndexable
public class SoundWorkSettings extends DashboardFragment implements OnActivityResultListener {

    private static final String TAG = "SoundWorkSettings";
    private static final int REQUEST_CODE = 200;
    private static final String SELECTED_PREFERENCE_KEY = "selected_preference";

    private RingtonePreference mRequestPreference;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.WORK_PROFILE_SOUNDS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            String selectedPreference = savedInstanceState.getString(
                    SELECTED_PREFERENCE_KEY, /* defaultValue= */ null);
            if (!TextUtils.isEmpty(selectedPreference)) {
                mRequestPreference = findPreference(selectedPreference);
            }
        }
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
                    /* options= */ null,
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

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, /* fragment= */ this, getSettingsLifecycle());
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.sound_work_settings;
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            SoundWorkSettings fragment, Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new SoundWorkSettingsController(context, fragment, lifecycle));
        return controllers;
    }

    static final boolean isSupportWorkProfileSound(Context context) {
        final AudioHelper audioHelper = new AudioHelper(context);
        final boolean hasWorkProfile = audioHelper.getManagedProfileId(
                UserManager.get(context)) != UserHandle.USER_NULL;
        final boolean shouldShowRingtoneSettings = !audioHelper.isSingleVolume();

        return hasWorkProfile && shouldShowRingtoneSettings;
    }

    void enableWorkSync() {
        final SoundWorkSettingsController soundWorkSettingsController =
                use(SoundWorkSettingsController.class);
        if (soundWorkSettingsController != null) {
            soundWorkSettingsController.enableWorkSync();
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.sound_work_settings) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return isSupportWorkProfileSound(context);
                }
            };
}
