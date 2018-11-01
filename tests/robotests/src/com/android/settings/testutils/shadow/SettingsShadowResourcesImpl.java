/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.res.Resources;
import android.content.res.ResourcesImpl;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import com.android.settings.R;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowLegacyResourcesImpl;

@Implements(
        value = ResourcesImpl.class,
        isInAndroidSdk = false,
        minSdk = 26
)
public class SettingsShadowResourcesImpl extends ShadowLegacyResourcesImpl {

    @Implementation
    public Drawable loadDrawable(Resources wrapper, TypedValue value, int id, int density,
            Resources.Theme theme) {
        // The drawable item in switchbar_background.xml refers to a very recent color attribute
        // that Robolectric isn't yet aware of.
        // TODO: Remove this once Robolectric is updated.
        if (id == R.drawable.switchbar_background
                || id == R.color.ripple_material_light
                || id == R.color.ripple_material_dark) {
            return new ColorDrawable();
        } else if (id == R.drawable.ic_launcher_settings) {
            // ic_launcher_settings uses adaptive-icon, which is not supported by robolectric,
            // change it to a normal drawable.
            id = R.drawable.ic_settings_wireless;
        } else if (id == R.drawable.app_filter_spinner_background) {
            id = R.drawable.ic_expand_more_inverse;
        } else if (id == R.drawable.color_bar_progress
                || id == R.drawable.ring_progress) {
            // color_bar_progress and ring_progress use hidden resources, so just use the regular
            // progress_horizontal drawable
            id = android.R.drawable.progress_horizontal;
        }

        return super.loadDrawable(wrapper, value, id, density, theme);
    }
}
