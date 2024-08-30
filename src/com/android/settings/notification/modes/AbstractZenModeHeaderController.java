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

import static com.google.common.base.Preconditions.checkNotNull;

import android.app.Flags;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.notification.modes.ZenIcon;
import com.android.settingslib.notification.modes.ZenIconLoader;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.widget.LayoutPreference;

import com.google.common.base.Objects;

import java.util.function.Function;

abstract class AbstractZenModeHeaderController extends AbstractZenModePreferenceController {

    private final DashboardFragment mFragment;
    private final ZenIconLoader mIconLoader;
    private EntityHeaderController mHeaderController;
    @Nullable private ZenIcon.Key mCurrentIconKey;

    AbstractZenModeHeaderController(
            @NonNull Context context,
            @NonNull ZenIconLoader iconLoader,
            @NonNull String key,
            @NonNull DashboardFragment fragment) {
        super(context, key);
        mFragment = fragment;
        mIconLoader = iconLoader;
    }

    @Override
    public boolean isAvailable() {
        return Flags.modesApi() && Flags.modesUi();
    }

    protected void setUpHeader(PreferenceScreen screen, int iconSizePx) {
        LayoutPreference preference = checkNotNull(screen.findPreference(getPreferenceKey()));
        preference.setSelectable(false);

        ImageView iconView = checkNotNull(preference.findViewById(R.id.entity_header_icon));
        ViewGroup.LayoutParams layoutParams = iconView.getLayoutParams();
        if (layoutParams.width != iconSizePx || layoutParams.height != iconSizePx) {
            layoutParams.width = iconSizePx;
            layoutParams.height = iconSizePx;
            iconView.setLayoutParams(layoutParams);
        }

        if (mHeaderController == null) {
            mHeaderController = EntityHeaderController.newInstance(
                    mFragment.getActivity(),
                    mFragment,
                    preference.findViewById(R.id.entity_header));
            mHeaderController.done(false); // Make the space for the (unused) name go away.
        }
    }

    protected void updateIcon(Preference preference, @NonNull ZenMode zenMode,
            Function<Drawable, Drawable> iconStylist, boolean isSelected) {

        ImageView iconView = checkNotNull(
                ((LayoutPreference) preference).findViewById(R.id.entity_header_icon));
        iconView.setSelected(isSelected);

        if (!Objects.equal(mCurrentIconKey, zenMode.getIconKey())) {
            mCurrentIconKey = zenMode.getIconKey();
            FutureUtil.whenDone(
                    mIconLoader.getIcon(mContext, zenMode),
                    icon -> {
                        checkNotNull(mHeaderController)
                                .setIcon(iconStylist.apply(icon.drawable()))
                                .done(/* rebindActions= */ false);
                        iconView.jumpDrawablesToCurrentState(); // Skip animation on first load.
                    },
                    mContext.getMainExecutor());
        }
    }
}
