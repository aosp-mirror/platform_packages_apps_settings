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

package com.android.settings;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settingslib.search.Indexable;

/**
 * Base class for fragment suitable for unit testing.
 */
public abstract class SettingsPreferenceFragmentBase extends SettingsPreferenceFragment
        implements Indexable {
    @Override
    @SuppressWarnings({"RequiresNullabilityAnnotation"})
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);
        onCreateCallback(icicle);
    }

    @Override
    @SuppressWarnings({"RequiresNullabilityAnnotation"})
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        onActivityCreatedCallback(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        onSaveInstanceStateCallback(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        onStartCallback();
    }

    @Override
    public void onStop() {
        super.onStop();
        onStopCallback();
    }

    protected Activity getCurrentActivity() {
        return getActivity();
    }

    /**
     * Callback called from {@link #onCreate}
     */
    public abstract void onCreateCallback(@Nullable Bundle icicle);

    /**
     * Callback called from {@link #onActivityCreated}
     */
    public abstract void onActivityCreatedCallback(@Nullable Bundle savedInstanceState);

    /**
     * Callback called from {@link #onStart}
     */
    public abstract void onStartCallback();

    /**
     * Callback called from {@link #onStop}
     */
    public abstract void onStopCallback();

    /**
     * Callback called from {@link #onSaveInstanceState}
     */
    public void onSaveInstanceStateCallback(@NonNull final Bundle outState) {
        // Do nothing.
    }
}
