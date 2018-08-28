/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.applications.assist;

import android.content.Context;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.internal.app.AssistUtils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import java.util.Arrays;
import java.util.List;

public class AssistContextPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener,
        LifecycleObserver, OnResume, OnPause {

    private static final String KEY_CONTEXT = "context";

    private final AssistUtils mAssistUtils;
    private final SettingObserver mSettingObserver;

    private Preference mPreference;
    private PreferenceScreen mScreen;

    public AssistContextPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        mAssistUtils = new AssistUtils(context);
        mSettingObserver = new SettingObserver();

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public boolean isAvailable() {
        return mAssistUtils.getAssistComponentForUser(UserHandle.myUserId()) != null;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_CONTEXT;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mScreen = screen;
        mPreference = screen.findPreference(getPreferenceKey());
        super.displayPreference(screen);
    }

    @Override
    public void onResume() {
        mSettingObserver.register(mContext.getContentResolver(), true);
        updatePreference();
    }

    @Override
    public void updateState(Preference preference) {
        updatePreference();
    }

    @Override
    public void onPause() {
        mSettingObserver.register(mContext.getContentResolver(), false);
    }


    private void updatePreference() {
        if (mPreference == null || !(mPreference instanceof TwoStatePreference)) {
            return;
        }
        if (isAvailable()) {
            if (mScreen.findPreference(getPreferenceKey()) == null) {
                // add it if it's not on scree
                mScreen.addPreference(mPreference);
            }
        } else {
            mScreen.removePreference(mPreference);
        }

        ((TwoStatePreference) mPreference).setChecked(isChecked(mContext));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ASSIST_STRUCTURE_ENABLED,
                (boolean) newValue ? 1 : 0);
        return true;
    }

    static boolean isChecked(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.ASSIST_STRUCTURE_ENABLED, 1) != 0;
    }

    class SettingObserver extends AssistSettingObserver {

        private final Uri URI =
                Settings.Secure.getUriFor(Settings.Secure.ASSIST_STRUCTURE_ENABLED);

        @Override
        protected List<Uri> getSettingUris() {
            return Arrays.asList(URI);
        }

        @Override
        public void onSettingChange() {
            updatePreference();
        }
    }
}
