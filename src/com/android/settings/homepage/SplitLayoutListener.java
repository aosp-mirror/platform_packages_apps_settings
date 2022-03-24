/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.homepage;

/** A listener receiving the spilt layout change */
public interface SplitLayoutListener {

    /**
     * Called when the spilt layout is changed.
     *
     * @param isRegularLayout whether the layout should be regular or simplified
     */
    void onSplitLayoutChanged(boolean isRegularLayout);

    /**
     * Notifies the listener whether the split layout is supported.
     */
    default void setSplitLayoutSupported(boolean supported) {
    }
}
