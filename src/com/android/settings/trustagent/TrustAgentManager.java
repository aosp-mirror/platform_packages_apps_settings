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

package com.android.settings.trustagent;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;


/** A manager for trust agent state. */
public interface TrustAgentManager {

   String PERMISSION_PROVIDE_AGENT = android.Manifest.permission.PROVIDE_TRUST_AGENT;

  /**
   * Determines if the service associated with a resolved trust agent intent is allowed to provide
   * trust on this device.
   *
   * @param resolveInfo The entry corresponding to the matched trust agent intent.
   * @param pm The package manager to be used to check for permissions.
   * @return {@code true} if the associated service is allowed to provide a trust agent, and
   * {@code false} if otherwise.
   */
   boolean shouldProvideTrust(ResolveInfo resolveInfo, PackageManager pm);
}
