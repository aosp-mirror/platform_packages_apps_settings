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
     * Returns whether the device is in COMP mode (primary user managed by a Device Owner app and
     * work profile managed by a Profile Owner app).
     */
    boolean isInCompMode();

    /**
     * Returns the name of the organization managing the device via a Device Owner app. If the
     * device is not managed by a Device Owner app or the name of the managing organization was not
     * set, returns {@code null}.
     */
    String getDeviceOwnerOrganizationName();

    /**
     * Returns a message informing the user that the device is managed by a Device Owner app. The
     * message includes a Learn More link that takes the user to the enterprise privacy section of
     * Settings. If the device is not managed by a Device Owner app, returns {@code null}.
     */
    CharSequence getDeviceOwnerDisclosure();

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

    /**
     * Returns whether security logging is currently enabled.
     */
    boolean isSecurityLoggingEnabled();

    /**
     * Returns whether network logging is currently enabled.
     */
    boolean isNetworkLoggingEnabled();

    /**
     * Returns whether the Device Owner or Profile Owner in the current user set an always-on VPN.
     */
    boolean isAlwaysOnVpnSetInCurrentUser();

    /**
     * Returns whether the Profile Owner in the current user's managed profile (if any) set an
     * always-on VPN.
     */
    boolean isAlwaysOnVpnSetInManagedProfile();

    /**
     * Returns whether the Device Owner set a recommended global HTTP proxy.
     */
    boolean isGlobalHttpProxySet();

    /**
     * Returns the number of failed login attempts that the Device Owner or Profile Owner allows
     * before the current user is wiped, or zero if no such limit is set.
     */
    int getMaximumFailedPasswordsBeforeWipeInCurrentUser();

    /**
     * Returns the number of failed login attempts that the Profile Owner allows before the current
     * user's managed profile (if any) is wiped, or zero if no such limit is set.
     */
    int getMaximumFailedPasswordsBeforeWipeInManagedProfile();

    /**
     * Returns the label of the current user's input method if that input method was set by a Device
     * Owner or Profile Owner in that user. Otherwise, returns {@code null}.
     */
    String getImeLabelIfOwnerSet();

    /**
     * Returns the number of CA certificates that the Device Owner or Profile Owner installed in
     * current user.
     */
    int getNumberOfOwnerInstalledCaCertsForCurrentUser();

    /**
     * Returns the number of CA certificates that the Device Owner or Profile Owner installed in
     * the current user's managed profile  (if any).
     */
    int getNumberOfOwnerInstalledCaCertsForManagedProfile();

    /**
     * Returns the number of Device Admin apps active in the current user and the user's managed
     * profile (if any).
     */
    int getNumberOfActiveDeviceAdminsForCurrentUserAndManagedProfile();

    /**
     * Returns {@code true} if it is possilbe to resolve an Intent to launch the "Your work policy
     * info" page provided by the active Device Owner or Profile Owner app if it exists, {@code
     * false} otherwise.
     */
    boolean hasWorkPolicyInfo();

    /**
     * Launches the Device Owner or Profile Owner's activity that displays the "Your work policy
     * info" page. Returns {@code true} if the activity has indeed been launched.
     */
    boolean showWorkPolicyInfo();
}
