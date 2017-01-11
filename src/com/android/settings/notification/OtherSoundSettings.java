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

import android.content.Context;
import android.provider.SearchIndexableResource;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.core.PreferenceController;
import com.android.settings.core.lifecycle.Lifecycle;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* This class has been deprecated  Modifications to Other Sounds settings should be made in
   {@link SoundSettings } instead. */
@Deprecated
public class OtherSoundSettings extends DashboardFragment {
    private static final String TAG = "OtherSoundSettings";

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.NOTIFICATION_OTHER_SOUND;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_other_sounds;
    }

    @Override
    protected String getCategoryKey() {
        return null;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.other_sound_settings;
    }

    @Override
    protected List<PreferenceController> getPreferenceControllers(Context context) {
        final List<PreferenceController> controllers = new ArrayList<>();
        Lifecycle lifecycle = getLifecycle();
        controllers.add(new DialPadTonePreferenceController(context, this, lifecycle));
        controllers.add(new ScreenLockSoundPreferenceController(context, this, lifecycle));
        controllers.add(new ChargingSoundPreferenceController(context, this, lifecycle));
        controllers.add(new DockingSoundPreferenceController(context, this, lifecycle));
        controllers.add(new TouchSoundPreferenceController(context, this, lifecycle));
        controllers.add(new VibrateOnTouchPreferenceController(context, this, lifecycle));
        controllers.add(new DockAudioMediaPreferenceController(context, this, lifecycle));
        controllers.add(new BootSoundPreferenceController(context));
        controllers.add(new EmergencyTonePreferenceController(context, this, lifecycle));
        return controllers;
    }

    // === Indexing ===

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

        public List<SearchIndexableResource> getXmlResourcesToIndex(
                Context context, boolean enabled) {
            final SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.other_sound_settings;
            return Arrays.asList(sir);
        }

        public List<String> getNonIndexableKeys(Context context) {
            final ArrayList<String> rt = new ArrayList<String>();
            new DialPadTonePreferenceController(context, null /* SettingsPreferenceFragment */,
                null /* Lifecycle */).updateNonIndexableKeys(rt);
            new ScreenLockSoundPreferenceController(context, null /* SettingsPreferenceFragment */,
                null /* Lifecycle */).updateNonIndexableKeys(rt);
            new ChargingSoundPreferenceController(context, null /* SettingsPreferenceFragment */,
                null /* Lifecycle */).updateNonIndexableKeys(rt);
            new DockingSoundPreferenceController(context, null /* SettingsPreferenceFragment */,
                null /* Lifecycle */).updateNonIndexableKeys(rt);
            new TouchSoundPreferenceController(context, null /* SettingsPreferenceFragment */,
                null /* Lifecycle */).updateNonIndexableKeys(rt);
            new VibrateOnTouchPreferenceController(context, null /* SettingsPreferenceFragment */,
                null /* Lifecycle */).updateNonIndexableKeys(rt);
            new DockAudioMediaPreferenceController(context, null /* SettingsPreferenceFragment */,
                null /* Lifecycle */).updateNonIndexableKeys(rt);
            new BootSoundPreferenceController(context).updateNonIndexableKeys(rt);
            new EmergencyTonePreferenceController(context, null /* SettingsPreferenceFragment */,
                null /* Lifecycle */).updateNonIndexableKeys(rt);
            return rt;
        }
    };
}
