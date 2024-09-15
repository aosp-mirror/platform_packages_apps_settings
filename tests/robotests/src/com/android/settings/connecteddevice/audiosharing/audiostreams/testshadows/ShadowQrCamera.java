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

package com.android.settings.connecteddevice.audiosharing.audiostreams.testshadows;

import android.graphics.SurfaceTexture;

import com.android.settingslib.qrcode.QrCamera;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

@Implements(value = QrCamera.class, callThroughByDefault = false)
public class ShadowQrCamera {

    private static QrCamera sMockQrCamera;

    public static void setUseMock(QrCamera mockQrCamera) {
        sMockQrCamera = mockQrCamera;
    }

    /** Start camera */
    @Implementation
    public void start(SurfaceTexture surface) {
        sMockQrCamera.start(surface);
    }

    /** Stop camera */
    @Implementation
    public void stop() {
        sMockQrCamera.stop();
    }

    /** Reset static fields */
    @Resetter
    public static void reset() {
        sMockQrCamera = null;
    }
}
