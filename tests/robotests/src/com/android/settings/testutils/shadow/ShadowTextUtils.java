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

package com.android.settings.testutils.shadow;

import android.icu.util.ULocale;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import java.util.Locale;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * Important: The current robolectric doesn't support API 24, so I copy the code
 * from API 24 here to make it compatible. Once robolectric is upgraded to 24,
 * We can delete this shadow class.
 **/
@Implements(TextUtils.class)
public class ShadowTextUtils {

    @Implementation
    public static int getLayoutDirectionFromLocale(Locale locale) {
        return ((locale != null && !locale.equals(Locale.ROOT)
                && ULocale.forLocale(locale).isRightToLeft())
                // If forcing into RTL layout mode, return RTL as default
                || SystemProperties.getBoolean(Settings.Global.DEVELOPMENT_FORCE_RTL, false))
                ? View.LAYOUT_DIRECTION_RTL
                : View.LAYOUT_DIRECTION_LTR;
    }

}
