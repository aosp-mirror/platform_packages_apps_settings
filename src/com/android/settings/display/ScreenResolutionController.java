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
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.util.Log;
import android.view.Display;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Controller that switch the screen resolution. */
public class ScreenResolutionController extends BasePreferenceController {
    private static final String TAG = "ScreenResolutionController";
    static final int HIGHRESOLUTION_IDX = 0;
    static final int FULLRESOLUTION_IDX = 1;

    private Display mDisplay;
    private Set<Point> mSupportedResolutions = null;
    private int mHighWidth = 0;
    private int mFullWidth = 0;
    private int mHighHeight = 0;
    private int mFullHeight = 0;

    public ScreenResolutionController(Context context, String key) {
        super(context, key);

        mDisplay =
                mContext.getSystemService(DisplayManager.class).getDisplay(Display.DEFAULT_DISPLAY);

        initSupportedResolutionData();
    }

    /**
     * Initialize the resolution data. So far, we support two resolution switching. Save the width
     * and the height for high resolution and full resolution.
     */
    private void initSupportedResolutionData() {
        // Collect and filter the resolutions
        Set<Point> resolutions = new HashSet<>();
        for (Display.Mode mode : getSupportedModes()) {
            resolutions.add(new Point(mode.getPhysicalWidth(), mode.getPhysicalHeight()));
        }
        mSupportedResolutions = resolutions;

        // Get the width and height for high resolution and full resolution
        List<Point> resolutionList = new ArrayList<>(resolutions);
        if (resolutionList == null || resolutionList.size() != 2) {
            Log.e(TAG, "No support");
            return;
        }

        Collections.sort(resolutionList, (p1, p2) -> p1.x * p1.y - p2.x * p2.y);
        mHighWidth = resolutionList.get(HIGHRESOLUTION_IDX).x;
        mHighHeight = resolutionList.get(HIGHRESOLUTION_IDX).y;
        mFullWidth = resolutionList.get(FULLRESOLUTION_IDX).x;
        mFullHeight = resolutionList.get(FULLRESOLUTION_IDX).y;
    }

    /** Return true if the device contains two (or more) resolutions. */
    protected boolean checkSupportedResolutions() {
        return getHighWidth() != 0 && getFullWidth() != 0;
    }

    @Override
    public int getAvailabilityStatus() {
        return (checkSupportedResolutions()) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        String summary = null;
        int width = getDisplayWidth();
        if (width == mHighWidth) {
            summary = mContext.getString(R.string.screen_resolution_option_high);
        } else if (width == mFullWidth) {
            summary = mContext.getString(R.string.screen_resolution_option_full);
        } else {
            summary = mContext.getString(R.string.screen_resolution_title);
        }

        return summary;
    }

    /** Return all supported resolutions of the device. */
    public Set<Point> getAllSupportedResolutions() {
        return this.mSupportedResolutions;
    }

    /** Return the high resolution width of the device. */
    public int getHighWidth() {
        return this.mHighWidth;
    }

    /** Return the full resolution width of the device. */
    public int getFullWidth() {
        return this.mFullWidth;
    }

    /** Return the high resolution height of the device. */
    public int getHighHeight() {
        return this.mHighHeight;
    }

    /** Return the full resolution height of the device. */
    public int getFullHeight() {
        return this.mFullHeight;
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
