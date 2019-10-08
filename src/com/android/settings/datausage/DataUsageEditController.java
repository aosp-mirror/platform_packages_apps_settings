/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.datausage;

import android.net.NetworkTemplate;

import com.android.settingslib.NetworkPolicyEditor;

/**
 * Used to create a dialog that modifies the Cellular data warning amount.
 */
public interface DataUsageEditController {
    /**
     * @return NetworkPolicyEditor to update the values of the data warning and usage limits.
     */
    NetworkPolicyEditor getNetworkPolicyEditor();

    /**
     * @return NetworkTemplate to get the currently set values of the data warning and usage limits.
     */
    NetworkTemplate getNetworkTemplate();

    /**
     * Callback to update the UI and values changed by the Dialog.
     */
    void updateDataUsage();
}
