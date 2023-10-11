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

package com.android.settings.vpn2;

import android.content.Context;

/**
 * Feature Provider used in vpn usage
 */
public interface AdvancedVpnFeatureProvider {

    /**
     * Returns package name of advanced vpn.
     */
    String getAdvancedVpnPackageName();

    /**
     * Returns {@code true} advanced vpn is supported.
     */
    boolean isAdvancedVpnSupported(Context context);

    /**
     * Returns the title of advanced vpn preference group.
     */
    String getAdvancedVpnPreferenceGroupTitle(Context context);

    /**
     * Returns the title of vpn preference group.
     */
    String getVpnPreferenceGroupTitle(Context context);

    /**
     * Returns {@code true} advanced vpn is removable.
     */
    boolean isAdvancedVpnRemovable();

    /**
     * Returns {@code true} if the disconnect dialog is enabled when advanced vpn is connected.
     */
    boolean isDisconnectDialogEnabled();
}
