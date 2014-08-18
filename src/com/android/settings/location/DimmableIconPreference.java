/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.location;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.util.AttributeSet;

/**
 * A preference item that can dim the icon when it's disabled, either directly or because its parent
 * is disabled.
 */
public class DimmableIconPreference extends Preference {
    private static final int ICON_ALPHA_ENABLED = 255;
    private static final int ICON_ALPHA_DISABLED = 102;

    public DimmableIconPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public DimmableIconPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DimmableIconPreference(Context context) {
        super(context);
    }

    private void dimIcon(boolean dimmed) {
        Drawable icon = getIcon();
        if (icon != null) {
            icon.mutate().setAlpha(dimmed ? ICON_ALPHA_DISABLED : ICON_ALPHA_ENABLED);
            setIcon(icon);
        }
    }

    @Override
    public void onParentChanged(Preference parent, boolean disableChild) {
        dimIcon(disableChild);
        super.onParentChanged(parent, disableChild);
    }

    @Override
    public void setEnabled(boolean enabled) {
        dimIcon(!enabled);
        super.setEnabled(enabled);
    }
}
