/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.applications.appinfo;

import android.app.slice.Slice;
import android.content.Context;
import android.os.Bundle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.text.TextUtils;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.applications.DefaultAppSettings;
import com.android.settings.core.BasePreferenceController;

/*
 * Abstract base controller for the default app shortcut preferences that launches the default app
 * settings with the corresponding default app highlighted.
 */
public abstract class DefaultAppShortcutPreferenceControllerBase extends BasePreferenceController {

    protected final String mPackageName;

    public DefaultAppShortcutPreferenceControllerBase(Context context, String preferenceKey,
            String packageName) {
        super(context, preferenceKey);
        mPackageName = packageName;
    }

    @Override
    public int getAvailabilityStatus() {
        if (UserManager.get(mContext).isManagedProfile()) {
            return DISABLED_FOR_USER;
        }
        return hasAppCapability() ? AVAILABLE : DISABLED_UNSUPPORTED;
    }

    @Override
    public Slice getSettingSlice() {
        return null;
    }

    @Override
    public void updateState(Preference preference) {
        preference.setSummary(isDefaultApp() ? R.string.yes : R.string.no);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(mPreferenceKey, preference.getKey())) {
            Bundle bundle = new Bundle();
            bundle.putString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY, mPreferenceKey);
            Utils.startWithFragment(mContext, DefaultAppSettings.class.getName(), bundle, null, 0,
                    R.string.configure_apps, null, MetricsProto.MetricsEvent.VIEW_UNKNOWN);
            return true;
        }
        return false;
    }

    /**
     * Check whether the app has the default app capability
     * @return true if the app has the default app capability
     */
    protected abstract boolean hasAppCapability();

    /**
     * Check whether the app is the default app
     * @return true if the app is the default app
     */
    protected abstract boolean isDefaultApp();

}
