/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings;

import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.support.annotation.ArrayRes;
import android.support.v7.preference.ListPreference;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.MathUtils;
import android.view.Display;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;

import com.android.settings.R;

import java.util.Arrays;

/**
 * Preference for changing the density of the display on which the preference
 * is visible.
 */
public class DisplayDensityPreference extends ListPreference {
    private static final String LOG_TAG = "DisplayDensityPreference";

    /** Minimum increment between density scales. */
    private static final float MIN_SCALE_INTERVAL = 0.09f;

    /** Minimum density scale. This is available on all devices. */
    private static final float MIN_SCALE = 0.85f;

    /** Maximum density scale. The actual scale used depends on the device. */
    private static final float MAX_SCALE = 1.50f;

    /** Sentinel value for "normal" scaling (effectively disabled). */
    private static final int DENSITY_VALUE_NORMAL = -1;

    /** Summary used for "normal" scale. */
    private static final int DENSITY_SUMMARY_NORMAL = R.string.force_density_summary_normal;

    /**
     * Summaries for scales smaller than "normal" in order of smallest to
     * largest.
     */
    private static final int[] SMALLER_SUMMARIES = new int[] {
            R.string.force_density_summary_small
    };

    /**
     * Summaries for scales larger than "normal" in order of smallest to
     * largest.
     */
    private static final int[] LARGER_SUMMARIES = new int[] {
            R.string.force_density_summary_large,
            R.string.force_density_summary_very_large,
            R.string.force_density_summary_extremely_large,
    };

    /**
     * Minimum allowed screen dimension, corresponds to resource qualifiers
     * "small" or "sw320dp". This value must be at least the minimum screen
     * size required by the CDD so that we meet developer expectations.
     */
    private static final int MIN_DIMENSION_DP = 320;

    /** The ID of the display affected by this preference. */
    private int mDisplayId = Display.DEFAULT_DISPLAY;

    public DisplayDensityPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (!prepareList()) {
            setEnabled(false);
        }
    }

    private boolean prepareList() {
        final int initialDensity = getInitialDisplayDensity(mDisplayId);
        if (initialDensity <= 0) {
            return false;
        }

        final Resources res = getContext().getResources();
        final DisplayMetrics metrics = res.getDisplayMetrics();
        final int currentDensity = metrics.densityDpi;
        int currentDensityIndex = -1;

        // Compute number of "larger" and "smaller" scales for this display.
        final int minDimensionPx = Math.min(metrics.widthPixels, metrics.heightPixels);
        final int maxDensity = DisplayMetrics.DENSITY_MEDIUM * minDimensionPx / MIN_DIMENSION_DP;
        final float maxScale = Math.min(MAX_SCALE, maxDensity / (float) initialDensity);
        final float minScale = MIN_SCALE;
        final int numLarger = (int) MathUtils.constrain((maxScale - 1) / MIN_SCALE_INTERVAL,
                0, LARGER_SUMMARIES.length);
        final int numSmaller = (int) MathUtils.constrain((1 - minScale) / MIN_SCALE_INTERVAL,
                0, SMALLER_SUMMARIES.length);

        CharSequence[] values = new CharSequence[1 + numSmaller + numLarger];
        CharSequence[] entries = new CharSequence[values.length];
        int curIndex = 0;

        if (numSmaller > 0) {
            final float interval = (1 - minScale) / numSmaller;
            for (int i = numSmaller - 1; i >= 0; i--) {
                final int density = (int) (initialDensity * (1 - (i + 1) * interval));
                if (currentDensity == density) {
                    currentDensityIndex = curIndex;
                }
                values[curIndex] = Integer.toString(density);
                entries[curIndex] = res.getText(SMALLER_SUMMARIES[i]);
                curIndex++;
            }
        }

        if (currentDensity == initialDensity) {
            currentDensityIndex = curIndex;
        }
        values[curIndex] = Integer.toString(DENSITY_VALUE_NORMAL);
        entries[curIndex] = res.getText(DENSITY_SUMMARY_NORMAL);
        curIndex++;

        if (numLarger > 0) {
            final float interval = (maxScale - 1) / numLarger;
            for (int i = 0; i < numLarger; i++) {
                final int density = (int) (initialDensity * (1 + (i + 1) * interval));
                if (currentDensity == density) {
                    currentDensityIndex = curIndex;
                }
                values[curIndex] = Integer.toString(density);
                entries[curIndex] = res.getText(LARGER_SUMMARIES[i]);
                curIndex++;
            }
        }

        final int displayIndex;
        if (currentDensityIndex >= 0) {
            displayIndex = currentDensityIndex;
        } else {
            // We don't understand the current density. Must have been set by
            // someone else. Make room for another entry...
            values = Arrays.copyOf(values, values.length + 1);
            values[curIndex] = res.getString(R.string.force_density_summary_custom, currentDensity);

            entries = Arrays.copyOf(entries, values.length + 1);
            entries[curIndex] = Integer.toString(currentDensity);

            displayIndex = curIndex;
        }

        super.setEntryValues(values);
        super.setEntries(entries);

        setValueIndex(displayIndex);

        return true;
    }

    @Override
    public boolean callChangeListener(Object newValue) {
        final boolean allowed = super.callChangeListener(newValue);
        if (allowed) {
            final int density = Integer.parseInt((String) newValue);
            setForcedDisplayDensity(mDisplayId, density);
        }

        return allowed;
    }

    @Override
    public void setEntries(CharSequence[] entries) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEntries(@ArrayRes int entriesResId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEntryValues(CharSequence[] entryValues) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEntryValues(@ArrayRes int entryValuesResId) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the initial density for the specified display.
     *
     * @param displayId the identifier of the display
     * @return the initial density of the specified display, or {@code -1} if
     *         the display does not exist or the density could not be obtained
     */
    private static int getInitialDisplayDensity(int displayId) {
        try {
            IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
            return wm.getInitialDisplayDensity(displayId);
        } catch (RemoteException exc) {
            return -1;
        }
    }

    /**
     * Asynchronously applies display density changes to the specified display.
     *
     * @param displayId the identifier of the display to modify
     * @param density the density to force for the specified display, or <= 0
     *                to clear any previously forced density
     */
    private static void setForcedDisplayDensity(final int displayId, final int density) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
                    if (density <= 0) {
                        wm.clearForcedDisplayDensity(displayId);
                    } else {
                        wm.setForcedDisplayDensity(displayId, density);
                    }
                } catch (RemoteException exc) {
                    Log.w(LOG_TAG, "Unable to save forced display density setting");
                }
            }
        });
    }
}
