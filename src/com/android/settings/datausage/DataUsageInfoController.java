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

import android.net.NetworkPolicy;

import com.android.settingslib.net.DataUsageController.DataUsageInfo;

/**
 * Controller to handle caching and mobile data logic.
 */
public class DataUsageInfoController {
    /**
     * Take the cached data usage values in the NetworkPolicy to update DataUsageInfo.
     */
    public void updateDataLimit(DataUsageInfo info, NetworkPolicy policy) {
        if (info == null || policy == null) {
            return;
        }
        if (policy.warningBytes >= 0) {
            info.warningLevel = policy.warningBytes;
        }
        if (policy.limitBytes >= 0) {
            info.limitLevel = policy.limitBytes;
        }
    }

    /**
     * @returns the most appropriate limit for the data usage summary. Use the total usage when it
     * is higher than the limit and warning level. Use the limit when it is set and less than usage.
     * Otherwise use warning level.
     */
    public long getSummaryLimit(DataUsageInfo info) {
        long limit = info.limitLevel;
        if (limit <= 0) {
            limit = info.warningLevel;
        }
        if (info.usageLevel > limit) {
            limit = info.usageLevel;
        }
        return limit;
    }
}
