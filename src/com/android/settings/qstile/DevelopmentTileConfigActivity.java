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
 * limitations under the License
 */

package com.android.settings.qstile;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.service.quicksettings.TileService;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;

import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.instrumentation.Instrumentable;

public class DevelopmentTileConfigActivity extends SettingsActivity {

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent())
                .putExtra(EXTRA_SHOW_FRAGMENT, DevelopmentTileConfigFragment.class.getName())
                .putExtra(EXTRA_HIDE_DRAWER, true);
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return (DevelopmentTileConfigFragment.class.getName().equals(fragmentName));
    }

    public static class DevelopmentTileConfigFragment extends SettingsPreferenceFragment
            implements Preference.OnPreferenceChangeListener {

        @Override
        public void onCreate(Bundle icicle) {
            super.onCreate(icicle);

            Context context = getPrefContext();
            setPreferenceScreen(getPreferenceManager().createPreferenceScreen(context));
            getPreferenceScreen().removeAll();

            Intent intent = new Intent(TileService.ACTION_QS_TILE)
                    .setPackage(context.getPackageName());
            PackageManager pm = getPackageManager();
            for (ResolveInfo info :
                    pm.queryIntentServices(intent, PackageManager.MATCH_DISABLED_COMPONENTS)) {
                ServiceInfo sInfo = info.serviceInfo;
                int enabledSetting = pm.getComponentEnabledSetting(
                        new ComponentName(sInfo.packageName, sInfo.name));
                boolean checked = enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        || ((enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
                        && sInfo.enabled);

                SwitchPreference preference = new SwitchPreference(context);
                preference.setTitle(sInfo.loadLabel(pm));
                preference.setIcon(sInfo.icon);
                preference.setKey(sInfo.name);
                preference.setChecked(checked);
                preference.setPersistent(false);
                preference.setOnPreferenceChangeListener(this);
                getPreferenceScreen().addPreference(preference);
            }
        }

        @Override
        public int getMetricsCategory() {
            return Instrumentable.METRICS_CATEGORY_UNKNOWN;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ComponentName cn = new ComponentName(
                    getPrefContext().getPackageName(), preference.getKey());
            getPackageManager().setComponentEnabledSetting(cn, (Boolean) newValue
                            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            return true;
        }
    }
}