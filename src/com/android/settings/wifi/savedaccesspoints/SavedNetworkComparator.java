/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.wifi.savedaccesspoints;

import android.icu.text.Collator;

import com.android.settingslib.wifi.AccessPoint;

import java.util.Comparator;

public final class SavedNetworkComparator {
    public static final Comparator<AccessPoint> INSTANCE =
            new Comparator<AccessPoint>() {
                final Collator mCollator = Collator.getInstance();

                @Override
                public int compare(AccessPoint ap1, AccessPoint ap2) {
                    return mCollator.compare(
                            nullToEmpty(ap1.getTitle()), nullToEmpty(ap2.getTitle()));
                }

                private String nullToEmpty(String string) {
                    return (string == null) ? "" : string;
                }
            };
}
