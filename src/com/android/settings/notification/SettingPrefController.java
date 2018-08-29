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

package com.android.settings.notification;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

public abstract class SettingPrefController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnResume, OnPause {

    protected static final int DEFAULT_ON = 1;

    private SettingsPreferenceFragment mParent;
    protected SettingsObserver mSettingsObserver;
    protected SettingPref mPreference;

    public SettingPrefController(Context context, SettingsPreferenceFragment parent,
            Lifecycle lifecycle) {
        super(context);
        mParent = parent;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mPreference.init(mParent);
        super.displayPreference(screen);
        if (isAvailable()) {
            mSettingsObserver = new SettingsObserver();
        }
    }

    @Override
    public String getPreferenceKey() {
        return mPreference.getKey();
    }

    @Override
    public boolean isAvailable() {
        return mPreference.isApplicable(mContext);
    }

    @Override
    public void updateState(Preference preference) {
        mPreference.update(mContext);
    }

    @Override
    public void onResume() {
        if (mSettingsObserver != null) {
            mSettingsObserver.register(true /* register */);
        }
    }

    @Override
    public void onPause() {
        if (mSettingsObserver != null) {
            mSettingsObserver.register(false /* register */);
        }
    }

    @VisibleForTesting
    final class SettingsObserver extends ContentObserver {
        public SettingsObserver() {
            super(new Handler());
        }

        public void register(boolean register) {
            final ContentResolver cr = mContext.getContentResolver();
            if (register) {
                cr.registerContentObserver(mPreference.getUri(), false, this);
            } else {
                cr.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (mPreference.getUri().equals(uri)) {
                mPreference.update(mContext);
            }
        }
    }

}
