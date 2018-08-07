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
 * limitations under the License.
 */

package com.android.settings.testutils.shadow;

import static org.mockito.Mockito.mock;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.SystemVibrator;
import android.os.VibrationEffect;
import android.os.Vibrator;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.fakes.RoboVibrator;
import org.robolectric.shadows.ShadowContextImpl;
import org.robolectric.util.ReflectionHelpers;

import java.util.Map;

@Implements(SystemVibrator.class)
public class ShadowVibrator {

    private static Map<String, String> getSystemServiceMap() {
        return ReflectionHelpers.getStaticField(ShadowContextImpl.class, "SYSTEM_SERVICE_MAP");
    }

    public static void addToServiceMap() {
        getSystemServiceMap().put(Context.VIBRATOR_SERVICE, SystemVibrator.class.getName());
    }

    public static void reset() {
        getSystemServiceMap().put(Context.VIBRATOR_SERVICE, RoboVibrator.class.getName());
    }

    public final Vibrator delegate = mock(Vibrator.class);

    @Implementation
    public void vibrate(int uid, String opPkg, VibrationEffect vibe, AudioAttributes attributes) {
        delegate.vibrate(uid, opPkg, vibe, attributes);
    }
}
