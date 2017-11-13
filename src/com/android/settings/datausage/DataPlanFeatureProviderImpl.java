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

package com.android.settings.datausage;

import android.os.SystemProperties;

/**
 * Impl for data plan feature provider.
 */
public final class DataPlanFeatureProviderImpl implements DataPlanFeatureProvider {
  private static final String ENABLE_SETTINGS_DATA_PLAN = "enable.settings.data.plan";

  @Override
  public boolean isEnabled() {
    return SystemProperties.getBoolean(ENABLE_SETTINGS_DATA_PLAN, false /* default */);
  }
}
