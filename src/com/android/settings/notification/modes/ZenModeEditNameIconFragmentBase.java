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
import static com.google.common.base.Preconditions.checkState;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.notification.modes.ZenIconLoader;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Base class for the "add a mode" and "edit mode name and icon" fragments. In both cases we are
 * editing a {@link ZenMode}, but the mode shouldn't be saved immediately after each atomic change
 * -- instead, it will be saved to the backend upon user confirmation.
 *
 * <p>As a result, instead of using {@link ZenModesBackend} to apply each change, we instead modify
 * an in-memory {@link ZenMode}, that is preserved/restored in extras. This also means we don't
 * listen to changes -- whatever the user sees should be applied.
 */
public abstract class ZenModeEditNameIconFragmentBase extends DashboardFragment {

    private static final String MODE_KEY = "ZenMode";

    @Nullable private ZenMode mZenMode;

    private ZenModesBackend mBackend;

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    void setBackend(ZenModesBackend backend) {
        mBackend = backend;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (mBackend == null) {
            mBackend = ZenModesBackend.getInstance(context);
        }
    }

    @Override
    public final void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mZenMode = icicle != null
                ? icicle.getParcelable(MODE_KEY, ZenMode.class)
                : onCreateInstantiateZenMode();

        if (mZenMode != null) {
            for (var controller : getZenPreferenceControllers()) {
                controller.setZenMode(mZenMode);
            }
        } else {
            finish();
        }
    }

    /**
     * Provides the mode that will be edited. Called in {@link #onCreate}, the first time (the
     * value returned here is persisted on Fragment recreation).
     *
     * <p>If {@code null} is returned, the fragment will {@link #finish()}.
     */
    @Nullable
    protected abstract ZenMode onCreateInstantiateZenMode();

    @Override
    protected final int getPreferenceScreenResId() {
        return R.xml.modes_edit_name_icon;
    }

    @Override
    protected final List<AbstractPreferenceController> createPreferenceControllers(
            Context context) {
        return ImmutableList.of(
                new ZenModeIconPickerIconPreferenceController(context, ZenIconLoader.getInstance(),
                        "chosen_icon", this),
                new ZenModeEditNamePreferenceController(context, "name", this::setModeName),
                new ZenModeIconPickerListPreferenceController(context, "icon_list",
                        this::setModeIcon),
                new ZenModeEditDonePreferenceController(context, "done", this::saveMode)
        );
    }

    private Iterable<AbstractZenModePreferenceController> getZenPreferenceControllers() {
        return getPreferenceControllers().stream()
                .flatMap(List::stream)
                .filter(AbstractZenModePreferenceController.class::isInstance)
                .map(AbstractZenModePreferenceController.class::cast)
                .toList();
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @Nullable
    ZenMode getZenMode() {
        return mZenMode;
    }

    @VisibleForTesting
    final void setModeName(String name) {
        checkNotNull(mZenMode).getRule().setName(Strings.nullToEmpty(name));
        forceUpdatePreferences(); // Updates confirmation button.
    }

    @VisibleForTesting
    final void setModeIcon(@DrawableRes int iconResId) {
        checkNotNull(mZenMode).getRule().setIconResId(iconResId);
        forceUpdatePreferences();  // Updates icon at the top.
    }


    @VisibleForTesting
    final void saveMode() {
        saveMode(checkNotNull(mZenMode));
    }

    /**
     * Called to actually save the mode, after the user confirms. This method is also responsible
     * for calling {@link #finish()}, if appropriate.
     *
     * <p>Note that {@code mode} is the <em>in-memory</em> mode and, as such, may have obsolete
     * data. If the concrete fragment is editing an existing mode, it should first fetch it from
     * the backend, and copy the new name and icon before saving. */
    abstract void saveMode(ZenMode mode);

    @NonNull
    protected ZenModesBackend requireBackend() {
        checkState(mBackend != null);
        return mBackend;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(MODE_KEY, mZenMode);
    }
}
