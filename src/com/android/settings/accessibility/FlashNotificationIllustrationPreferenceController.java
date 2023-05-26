/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.content.Context;
import android.os.SystemProperties;
import android.util.ArraySet;

import com.android.settings.core.BasePreferenceController;

import java.util.Collections;
import java.util.Set;

/** Preference controller for illustration in flash notifications page. */
public class FlashNotificationIllustrationPreferenceController extends BasePreferenceController {

    public FlashNotificationIllustrationPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        // TODO(b/280748155): Update tablet illustration when it's available. Hide it for now.
        String characteristics = SystemProperties.get("ro.build.characteristics");
        String[] characteristicsSplit = characteristics.split(",");
        Set<String> productCharacteristics = new ArraySet<>(characteristicsSplit.length);
        Collections.addAll(productCharacteristics, characteristicsSplit);
        final boolean isTablet = productCharacteristics.contains("tablet");
        return isTablet ? UNSUPPORTED_ON_DEVICE : AVAILABLE;
    }
}
