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
 * limitations under the License
 */
package com.android.internal.app;

/**
 * Fake controller to make robolectric test compile. Should be removed when Robolectric supports
 * API 25.
 */
public class NightDisplayController {

    public static final int AUTO_MODE_CUSTOM = 1;
    public static final int COLOR_MODE_NATURAL = 0;

    public static final int AUTO_MODE_TWILIGHT = 2;
    public static final int COLOR_MODE_BOOSTED = 1;
    public static final int COLOR_MODE_SATURATED = 2;
    private int mColorMode;

    public void setColorMode(int colorMode) {
        mColorMode = colorMode;
    }

    public int getColorMode() {
        return mColorMode;
    }

    public interface Callback {
    }
}
