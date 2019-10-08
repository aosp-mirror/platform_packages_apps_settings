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

package com.android.settings.development;

import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.pm.PackageManager;
import android.os.ServiceManager;
import android.view.DisplayCutout;

import androidx.annotation.VisibleForTesting;

public class EmulateDisplayCutoutPreferenceController extends OverlayCategoryPreferenceController {

    private static final String KEY = "display_cutout_emulation";

    @VisibleForTesting
    EmulateDisplayCutoutPreferenceController(Context context, PackageManager packageManager,
            IOverlayManager overlayManager) {
        super(context, packageManager, overlayManager, DisplayCutout.EMULATION_OVERLAY_CATEGORY);
    }

    public EmulateDisplayCutoutPreferenceController(Context context) {
        this(context, context.getPackageManager(), IOverlayManager.Stub
                .asInterface(ServiceManager.getService(Context.OVERLAY_SERVICE)));
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }
}
