/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.network.telephony;

import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

abstract class AbstractMobileNetworkSettings extends RestrictedDashboardFragment {

    private static final String LOG_TAG = "AbsNetworkSettings";

    private List<AbstractPreferenceController> mHiddenControllerList =
            new ArrayList<AbstractPreferenceController>();

    /**
     * @param restrictionKey The restriction key to check before pin protecting
     *            this settings page. Pass in {@link RESTRICT_IF_OVERRIDABLE} if it should
     *            be protected whenever a restrictions provider is set. Pass in
     *            null if it should never be protected.
     */
    AbstractMobileNetworkSettings(String restrictionKey) {
        super(restrictionKey);
    }

    List<AbstractPreferenceController> getPreferenceControllersAsList() {
        final List<AbstractPreferenceController> result =
                new ArrayList<AbstractPreferenceController>();
        getPreferenceControllers().forEach(controllers -> result.addAll(controllers));
        return result;
    }

    TelephonyStatusControlSession setTelephonyAvailabilityStatus(
            Collection<AbstractPreferenceController> listOfPrefControllers) {
        return (new TelephonyStatusControlSession.Builder(listOfPrefControllers))
                .build();
    }

    @Override
    public void onExpandButtonClick() {
        final PreferenceScreen screen = getPreferenceScreen();
        mHiddenControllerList.stream()
                .filter(controller -> controller.isAvailable())
                .forEach(controller -> {
                    final String key = controller.getPreferenceKey();
                    final Preference preference = screen.findPreference(key);
                    controller.updateState(preference);
                });
        super.onExpandButtonClick();
    }

    /*
     * Replace design within {@link DashboardFragment#updatePreferenceStates()}
     */
    @Override
    protected void updatePreferenceStates() {
        mHiddenControllerList.clear();

        final PreferenceScreen screen = getPreferenceScreen();
        getPreferenceControllersAsList().forEach(controller -> {
            final String key = controller.getPreferenceKey();
            if (TextUtils.isEmpty(key)) {
                return;
            }
            final Preference preference = screen.findPreference(key);
            if (preference == null) {
                return;
            }
            if (!isPreferenceExpanded(preference)) {
                mHiddenControllerList.add(controller);
                return;
            }
            if (!controller.isAvailable()) {
                return;
            }
            controller.updateState(preference);
        });
    }


}
