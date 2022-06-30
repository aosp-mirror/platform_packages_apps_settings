/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.display;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.view.Display;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/** Controller that switch the screen resolution. */
public class ScreenResolutionController extends BasePreferenceController {

    static final int FHD_WIDTH = 1080;
    static final int QHD_WIDTH = 1440;

    private Display mDisplay;

    public ScreenResolutionController(Context context, String key) {
        super(context, key);

        mDisplay =
                mContext.getSystemService(DisplayManager.class).getDisplay(Display.DEFAULT_DISPLAY);
    }

    /** Check if the width is supported by the display. */
    private boolean isSupportedMode(int width) {
        for (Display.Mode mode : getSupportedModes()) {
            if (mode.getPhysicalWidth() == width) return true;
        }
        return false;
    }

    /** Return true if the device contains two (or more) resolutions. */
    protected boolean checkSupportedResolutions() {
        return isSupportedMode(FHD_WIDTH) && isSupportedMode(QHD_WIDTH);
    }

    @Override
    public int getAvailabilityStatus() {
        return (checkSupportedResolutions()) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        String summary = null;
        switch (getDisplayWidth()) {
            case FHD_WIDTH:
                summary = mContext.getString(R.string.screen_resolution_summary_high);
                break;
            case QHD_WIDTH:
                summary = mContext.getString(R.string.screen_resolution_summary_highest);
                break;
            default:
                summary = mContext.getString(R.string.screen_resolution_title);
        }

        return summary;
    }

    @VisibleForTesting
    public int getDisplayWidth() {
        return mDisplay.getMode().getPhysicalWidth();
    }

    @VisibleForTesting
    public Display.Mode[] getSupportedModes() {
        return mDisplay.getSupportedModes();
    }
}
