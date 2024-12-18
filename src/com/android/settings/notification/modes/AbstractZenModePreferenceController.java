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

import static com.google.common.base.Preconditions.checkState;

import android.app.Flags;
import android.content.Context;
import android.service.notification.ZenPolicy;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

import java.util.function.Function;

/**
 * Base class for any preference controllers pertaining to any single Zen mode.
 */
abstract class AbstractZenModePreferenceController extends AbstractPreferenceController {

    private static final String TAG = "AbstractZenModePreferenceController";

    @Nullable protected final ZenModesBackend mBackend;

    @Nullable  // only until setZenMode() is called
    private ZenMode mZenMode;

    @NonNull
    private final String mKey;

    @NonNull private final MetricsFeatureProvider mMetricsFeatureProvider;

    /**
     * Constructor suitable for "read-only" controllers (e.g. link to a different sub-screen.
     * Controllers that call this constructor to initialize themselves <em>cannot</em> call
     * {@link #saveMode} or {@link #savePolicy} later.
     */
    AbstractZenModePreferenceController(@NonNull Context context, @NonNull String key) {
        super(context);
        mKey = key;
        mBackend = null;
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    /**
     * Constructor suitable for controllers that will update the associated {@link ZenMode}.
     * Controllers that call this constructor to initialize themselves may call {@link #saveMode} or
     * {@link #savePolicy} later.
     */
    AbstractZenModePreferenceController(@NonNull Context context, @NonNull String key,
            @NonNull ZenModesBackend backend) {
        super(context);
        mKey = key;
        mBackend = backend;
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    @NonNull
    public String getPreferenceKey() {
        return mKey;
    }

    @NonNull
    public MetricsFeatureProvider getMetricsFeatureProvider() {
        return mMetricsFeatureProvider;
    }

    @Override
    public boolean isAvailable() {
        if (mZenMode != null) {
            return Flags.modesUi() && isAvailable(mZenMode);
        } else {
            return Flags.modesUi();
        }
    }

    public boolean isAvailable(@NonNull ZenMode zenMode) {
        return true;
    }

    /**
     * Assigns the {@link ZenMode} of this controller, so that it can be used later from
     * {@link #isAvailable()} and {@link #updateState(Preference)}.
     */
    final void setZenMode(@NonNull ZenMode zenMode) {
        mZenMode = zenMode;
    }

    @Override
    public final void updateState(Preference preference) {
        super.updateState(preference);
        if (mZenMode != null) {
            updateState(preference, mZenMode);
        }
    }

    abstract void updateState(Preference preference, @NonNull ZenMode zenMode);

    @Override
    public final CharSequence getSummary() {
        if (mZenMode != null) {
            return getSummary(mZenMode);
        } else {
            return null;
        }
    }

    @Nullable
    protected CharSequence getSummary(@NonNull ZenMode zenMode) {
        return null;
    }

    /**
     * Subclasses should call this method (or a more specific one, like {@link #savePolicy} from
     * their {@code onPreferenceChange()} or similar, in order to apply changes to the mode being
     * edited (e.g. {@code saveMode(mode -> { mode.setX(value); return mode; } }.
     *
     * @param updater Function to update the {@link ZenMode}. Modifying and returning the same
     *                instance is ok.
     */
    protected final boolean saveMode(Function<ZenMode, ZenMode> updater) {
        checkState(mBackend != null);
        ZenMode mode = mZenMode;
        if (mode == null) {
            Log.wtf(TAG, "Cannot save mode, it hasn't been loaded (" + getClass() + ")");
            return false;
        }
        mode = updater.apply(mode);
        mBackend.updateMode(mode);
        return true;
    }

    protected final boolean savePolicy(Function<ZenPolicy.Builder, ZenPolicy.Builder> updater) {
        return saveMode(mode -> {
            ZenPolicy.Builder policyBuilder = new ZenPolicy.Builder(mode.getPolicy());
            policyBuilder = updater.apply(policyBuilder);
            mode.setPolicy(policyBuilder.build());
            return mode;
        });
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @Nullable
    ZenMode getZenMode() {
        return mZenMode;
    }

    /**
     * Convenience method for tests. Assigns the {@link ZenMode} of this controller, and calls
     * {@link #updateState(Preference)} immediately.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    final void updateZenMode(@NonNull Preference preference, @NonNull ZenMode zenMode) {
        mZenMode = zenMode;
        updateState(preference);
    }
}
