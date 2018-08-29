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
 * limitations under the License.
 */
package com.android.settings.development;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Display;

import androidx.preference.SwitchPreference;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

public class ColorModePreference extends SwitchPreference implements DisplayListener {

    private DisplayManager mDisplayManager;
    private Display mDisplay;

    private int mCurrentIndex;
    private List<ColorModeDescription> mDescriptions;

    public static List<ColorModeDescription> getColorModeDescriptions(Context context) {

        List<ColorModeDescription> colorModeDescriptions = new ArrayList<>();
        Resources resources = context.getResources();
        int[] colorModes = resources.getIntArray(R.array.color_mode_ids);
        String[] titles = resources.getStringArray(R.array.color_mode_names);
        String[] descriptions = resources.getStringArray(R.array.color_mode_descriptions);
        // Map the resource information describing color modes.
        for (int i = 0; i < colorModes.length; i++) {
            if (colorModes[i] != -1 && i != 1 /* Skip Natural for now. */) {
                ColorModeDescription desc = new ColorModeDescription();
                desc.colorMode = colorModes[i];
                desc.title = titles[i];
                desc.summary = descriptions[i];
                colorModeDescriptions.add(desc);
            }
        }

        return colorModeDescriptions;
    }

    public ColorModePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDisplayManager = getContext().getSystemService(DisplayManager.class);
    }

    public int getColorModeCount() {
        return mDescriptions.size();
    }

    public void startListening() {
        mDisplayManager.registerDisplayListener(this, new Handler(Looper.getMainLooper()));
    }

    public void stopListening() {
        mDisplayManager.unregisterDisplayListener(this);
    }

    @Override
    public void onDisplayAdded(int displayId) {
        if (displayId == Display.DEFAULT_DISPLAY) {
            updateCurrentAndSupported();
        }
    }

    @Override
    public void onDisplayChanged(int displayId) {
        if (displayId == Display.DEFAULT_DISPLAY) {
            updateCurrentAndSupported();
        }
    }

    @Override
    public void onDisplayRemoved(int displayId) {
    }

    public void updateCurrentAndSupported() {
        mDisplay = mDisplayManager.getDisplay(Display.DEFAULT_DISPLAY);

        mDescriptions = getColorModeDescriptions(getContext());

        int currentColorMode = mDisplay.getColorMode();
        mCurrentIndex = -1;
        for (int i = 0; i < mDescriptions.size(); i++) {
            if (mDescriptions.get(i).colorMode == currentColorMode) {
                mCurrentIndex = i;
                break;
            }
        }
        setChecked(mCurrentIndex == 1);
    }

    @Override
    protected boolean persistBoolean(boolean value) {
        // Right now this is a switch, so we only support two modes.
        if (mDescriptions.size() == 2) {
            ColorModeDescription desc = mDescriptions.get(value ? 1 : 0);

            mDisplay.requestColorMode(desc.colorMode);
            mCurrentIndex = mDescriptions.indexOf(desc);
        }

        return true;
    }

    private static class ColorModeDescription {
        private int colorMode;
        private String title;
        private String summary;
    }
}
