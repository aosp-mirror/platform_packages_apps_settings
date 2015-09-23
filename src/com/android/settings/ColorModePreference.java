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
package com.android.settings;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.os.Handler;
import android.os.Looper;
import android.preference.DialogPreference;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.Display;
import android.view.Display.ColorTransform;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Checkable;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class ColorModePreference extends SwitchPreference implements DisplayListener {

    private DisplayManager mDisplayManager;
    private Display mDisplay;

    private int mCurrentIndex;
    private ArrayList<ColorTransformDescription> mDescriptions;

    public ColorModePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDisplayManager = getContext().getSystemService(DisplayManager.class);
    }

    public int getTransformsCount() {
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

        mDescriptions = new ArrayList<>();

        Resources resources = getContext().getResources();
        int[] transforms = resources.getIntArray(
                com.android.internal.R.array.config_colorTransforms);
        String[] titles = resources.getStringArray(R.array.color_mode_names);
        String[] descriptions = resources.getStringArray(R.array.color_mode_descriptions);
        // Map the resource information describing color transforms.
        for (int i = 0; i < transforms.length; i++) {
            if (transforms[i] != -1 && i != 1 /* Skip Natural for now. */) {
                ColorTransformDescription desc = new ColorTransformDescription();
                desc.colorTransform = transforms[i];
                desc.title = titles[i];
                desc.summary = descriptions[i];
                mDescriptions.add(desc);
            }
        }
        // Match up a ColorTransform to every description.
        ColorTransform[] supportedColorTransforms = mDisplay.getSupportedColorTransforms();
        for (int i = 0; i < supportedColorTransforms.length; i++) {
            for (int j = 0; j < mDescriptions.size(); j++) {
                if (mDescriptions.get(j).colorTransform
                        == supportedColorTransforms[i].getColorTransform()
                        && mDescriptions.get(j).transform == null) {
                    mDescriptions.get(j).transform = supportedColorTransforms[i];
                    break;
                }
            }
        }
        // Remove any extras that don't have a transform for some reason.
        for (int i = 0; i < mDescriptions.size(); i++) {
            if (mDescriptions.get(i).transform == null) {
                mDescriptions.remove(i--);
            }
        }

        ColorTransform currentTransform = mDisplay.getColorTransform();
        mCurrentIndex = -1;
        for (int i = 0; i < mDescriptions.size(); i++) {
            if (mDescriptions.get(i).colorTransform == currentTransform.getColorTransform()) {
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
            ColorTransformDescription desc = mDescriptions.get(value ? 1 : 0);

            mDisplay.requestColorTransform(desc.transform);
            mCurrentIndex = mDescriptions.indexOf(desc);
        }

        return true;
    }

    private static class ColorTransformDescription {
        private int colorTransform;
        private String title;
        private String summary;
        private ColorTransform transform;
    }
}
