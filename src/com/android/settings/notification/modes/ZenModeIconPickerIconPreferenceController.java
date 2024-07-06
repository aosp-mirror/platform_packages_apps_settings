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

import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.notification.modes.ZenIconLoader;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.widget.LayoutPreference;

/** Controller used for displaying the currently-chosen icon at the top of the icon picker. */
class ZenModeIconPickerIconPreferenceController extends AbstractZenModePreferenceController {

    private final DashboardFragment mFragment;
    private EntityHeaderController mHeaderController;

    ZenModeIconPickerIconPreferenceController(@NonNull Context context, @NonNull String key,
            @NonNull DashboardFragment fragment) {
        super(context, key);
        mFragment = fragment;
    }

    @Override
    void updateState(Preference preference, @NonNull ZenMode zenMode) {
        preference.setSelectable(false);

        if (mHeaderController == null) {
            final LayoutPreference pref = (LayoutPreference) preference;
            mHeaderController = EntityHeaderController.newInstance(
                            mFragment.getActivity(),
                            mFragment,
                            pref.findViewById(R.id.entity_header));

            ImageView iconView = pref.findViewById(R.id.entity_header_icon);
            ViewGroup.LayoutParams layoutParams = iconView.getLayoutParams();
            int imageSizePx = iconView.getContext().getResources().getDimensionPixelSize(
                    R.dimen.zen_mode_icon_list_header_circle_diameter);
            layoutParams.width = imageSizePx;
            layoutParams.height = imageSizePx;
            iconView.setLayoutParams(layoutParams);
        }

        FutureUtil.whenDone(
                zenMode.getIcon(mContext, ZenIconLoader.getInstance()),
                icon -> mHeaderController.setIcon(IconUtil.makeBigIconCircle(mContext, icon))
                        .done(/* rebindActions= */ false),
                mContext.getMainExecutor());
    }
}
