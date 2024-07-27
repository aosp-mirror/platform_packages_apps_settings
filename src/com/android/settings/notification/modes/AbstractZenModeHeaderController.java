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
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.notification.modes.ZenIconLoader;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.widget.LayoutPreference;

import java.util.function.Consumer;
import java.util.function.Function;

abstract class AbstractZenModeHeaderController extends AbstractZenModePreferenceController {

    private final DashboardFragment mFragment;
    private EntityHeaderController mHeaderController;

    AbstractZenModeHeaderController(
            @NonNull Context context,
            @NonNull String key,
            @NonNull DashboardFragment fragment) {
        super(context, key);
        mFragment = fragment;
    }

    @Override
    public boolean isAvailable() {
        return Flags.modesApi() && Flags.modesUi();
    }

    protected void updateIcon(Preference preference, @NonNull ZenMode zenMode, int iconSizePx,
            Function<Drawable, Drawable> modeIconStylist,
            @Nullable Consumer<ImageView> iconViewCustomizer) {
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

        ImageView iconView = ((LayoutPreference) preference).findViewById(R.id.entity_header_icon);
        if (iconView != null) {
            if (iconViewCustomizer != null) {
                iconViewCustomizer.accept(iconView);
            }
            ViewGroup.LayoutParams layoutParams = iconView.getLayoutParams();
            if (layoutParams.width != iconSizePx || layoutParams.height != iconSizePx) {
                layoutParams.width = iconSizePx;
                layoutParams.height = iconSizePx;
                iconView.setLayoutParams(layoutParams);
            }
        }

        FutureUtil.whenDone(
                zenMode.getIcon(mContext, ZenIconLoader.getInstance()),
                icon -> mHeaderController
                        .setIcon(modeIconStylist.apply(icon))
                        .done(/* rebindActions= */ false),
                mContext.getMainExecutor());
    }
}
