/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import java.util.List;

/**
 * Top-level settings activity to handle single pane and double pane UI layout.
 */
public class Settings extends PreferenceActivity implements
        SettingsPreferenceFragment.FragmentStarter {

    // TODO: Update Call Settings based on airplane mode state.

    /**
     * Checks if the component name in the intent is different from the Settings class and
     * returns the class name to load as a fragment.
     */
    private String getStartingFragmentClass(Intent intent) {
        final String intentClass = intent.getComponent().getClassName();
        if (intentClass.equals(getClass().getName())) return null;

        return intentClass;
    }

    /**
     * Override initial header when an activity-alias is causing Settings to be launched
     * for a specific fragment encoded in the android:name parameter.
     */
    @Override
    public Header onGetInitialHeader() {
        String fragmentClass = getStartingFragmentClass(super.getIntent());
        if (fragmentClass != null) {
            Header header = new Header();
            header.fragment = fragmentClass;
            return header;
        }
        return super.onGetInitialHeader();
    }

    /**
     * Populate the activity with the top-level headers.
     */
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.settings_headers, target);

        updateHeaderList(target);
    }

    private void updateHeaderList(List<Header> target) {
        int i = 0;
        while (i < target.size()) {
            Header header = target.get(i);
            long id = header.id;
            if (id == R.id.dock_settings) {
                if (!needsDockSettings())
                    target.remove(header);
            } else if (id == R.id.operator_settings || id == R.id.manufacturer_settings) {
                Utils.updateHeaderToSpecificActivityFromMetaDataOrRemove(this, target, header);
            } else if (id == R.id.call_settings) {
                if (!Utils.isVoiceCapable(this))
                    target.remove(header);
            }
            if (target.get(i) == header)
                i++;
        }
    }

    private boolean needsDockSettings() {
        return getResources().getBoolean(R.bool.has_dock_settings);
    }

    public boolean startFragment(Fragment caller, String fragmentClass, int requestCode,
            Bundle extras) {
        Fragment f = Fragment.instantiate(this, fragmentClass, extras);
        caller.setTargetFragment(f, requestCode);
        if (f instanceof SettingsPreferenceFragment) {
            SettingsPreferenceFragment spf = (SettingsPreferenceFragment) f;
            spf.setFragmentStarter(this);
        }
        return true;
    }
}
