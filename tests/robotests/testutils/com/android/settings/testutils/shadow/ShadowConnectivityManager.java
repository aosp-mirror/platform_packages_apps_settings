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

import android.net.ConnectivityManager;
import android.util.SparseBooleanArray;

import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;

@Implements(value = ConnectivityManager.class)
public class ShadowConnectivityManager extends org.robolectric.shadows.ShadowConnectivityManager {

    private final SparseBooleanArray mSupportedNetworkTypes = new SparseBooleanArray();
    private boolean mTetheringSupported = false;

    public void setNetworkSupported(int networkType, boolean supported) {
        mSupportedNetworkTypes.put(networkType, supported);
    }

    @Implementation
    protected boolean isNetworkSupported(int networkType) {
        return mSupportedNetworkTypes.get(networkType);
    }

    public void setTetheringSupported(boolean supported) {
        mTetheringSupported = supported;
    }

    @Implementation
    protected boolean isTetheringSupported() {
        return mTetheringSupported;
    }

    public static ShadowConnectivityManager getShadow() {
        return Shadow.extract(
                RuntimeEnvironment.application.getSystemService(ConnectivityManager.class));
    }
}
