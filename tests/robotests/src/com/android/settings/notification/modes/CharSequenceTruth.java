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

import androidx.annotation.Nullable;

import com.google.common.truth.StringSubject;
import com.google.common.truth.Truth;

class CharSequenceTruth {
    /**
     * Shortcut version of {@link Truth#assertThat(String)} suitable for {@link CharSequence}.
     * {@link CharSequence} doesn't necessarily provide a good {@code equals()} implementation;
     * however we don't care about formatting in most cases, and we want to assert on the resulting
     * string (without needing to worry that {@code assertThat(x.getText().toString())} can
     * throw if the text is null).
     */
    static StringSubject assertThat(@Nullable CharSequence actual) {
        return Truth.assertThat((String) (actual != null ? actual.toString() : null));
    }
}
