/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.enterprise;

import java.util.Date;

public interface EnterprisePrivacyFeatureProvider {

    /**
     * Returns whether the device is managed by a Device Owner app.
     */
    boolean hasDeviceOwner();

    /**
     * Returns the time at which the Device Owner last retrieved security logs, or {@code null} if
     * logs were never retrieved by the Device Owner on this device.
     */
    Date getLastSecurityLogRetrievalTime();

    /**
     * Returns the time at which the Device Owner last requested a bug report, or {@code null} if no
     * bug report was ever requested by the Device Owner on this device.
     */
    Date getLastBugReportRequestTime();

    /**
     * Returns the time at which the Device Owner last retrieved network logs, or {@code null} if
     * logs were never retrieved by the Device Owner on this device.
     */
    Date getLastNetworkLogRetrievalTime();
}
