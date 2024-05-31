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
import android.service.notification.ZenPolicy;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.android.settingslib.core.AbstractPreferenceController;

import com.google.common.base.Preconditions;

import java.util.function.Function;

/**
 * Base class for any preference controllers pertaining to any single Zen mode.
 */
abstract class AbstractZenModePreferenceController extends AbstractPreferenceController {

    private static final String TAG = "AbstractZenModePreferenceController";

    @Nullable
    protected ZenModesBackend mBackend;

    @Nullable  // only until setZenMode() is called
    private ZenMode mZenMode;

    @NonNull
    private final String mKey;

    // ZenModesBackend should only be passed in if the preference controller may set the user's
    // policy for this zen mode. Otherwise, if the preference controller is essentially read-only
    // and leads to a further Settings screen, backend should be null.
    AbstractZenModePreferenceController(@NonNull Context context, @NonNull String key,
            @Nullable ZenModesBackend backend) {
        super(context);
        mBackend = backend;
        mKey = key;
    }

    @Override
    @NonNull
    public String getPreferenceKey() {
        return mKey;
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

    // Called by parent Fragment onAttach, for any methods (such as isAvailable()) that need
    // zen mode info before onStart. Most callers should use updateZenMode instead, which will
    // do any further necessary propagation.
    protected final void setZenMode(@NonNull ZenMode zenMode) {
        mZenMode = zenMode;
    }

    // Called by the parent Fragment onStart, which means it will happen before resume.
    public void updateZenMode(@NonNull Preference preference, @NonNull ZenMode zenMode) {
        mZenMode = zenMode;
        updateState(preference);
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
        Preconditions.checkState(mBackend != null);
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
}
