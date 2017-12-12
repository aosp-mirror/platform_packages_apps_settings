/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.gestures;

import android.content.Context;

import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.List;

/** Feature provider for the assist gesture. */
public interface AssistGestureFeatureProvider {

    /** Returns true if the assist gesture is supported. */
    boolean isSupported(Context context);

    /** Returns true if the sensor is available. */
    boolean isSensorAvailable(Context context);

    /** Returns a list of additional preference controllers */
    List<AbstractPreferenceController> getControllers(Context context, Lifecycle lifecycle);

}
