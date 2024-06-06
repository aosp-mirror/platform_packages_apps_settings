/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.settings.notification.modes;

import android.app.Flags;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.widget.LayoutPreference;

class ZenModeHeaderController extends AbstractZenModePreferenceController {

    private final DashboardFragment mFragment;
    private EntityHeaderController mHeaderController;

    ZenModeHeaderController(
            @NonNull  Context context,
            @NonNull String key,
            @NonNull DashboardFragment fragment,
            @Nullable ZenModesBackend backend) {
        super(context, key, backend);
        mFragment = fragment;
    }

    @Override
    public boolean isAvailable() {
        return Flags.modesApi();
    }

    @Override
    public void updateState(Preference preference, @NonNull ZenMode zenMode) {
        if (mFragment == null) {
            return;
        }
        preference.setSelectable(false);

        if (mHeaderController == null) {
            final LayoutPreference pref = (LayoutPreference) preference;
            mHeaderController = EntityHeaderController.newInstance(
                    mFragment.getActivity(),
                    mFragment,
                    pref.findViewById(R.id.entity_header));
        }

        FutureUtil.whenDone(
                zenMode.getIcon(mContext, IconLoader.getInstance()),
                icon -> mHeaderController.setIcon(icon)
                        .setLabel(zenMode.getRule().getName())
                        .done(false /* rebindActions */),
                mContext.getMainExecutor());
    }
}
