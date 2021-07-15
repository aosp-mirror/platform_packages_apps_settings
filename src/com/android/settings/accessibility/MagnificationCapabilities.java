/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.annotation.IntDef;

import com.android.settings.R;

import com.google.common.primitives.Ints;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Class to provide magnification capabilities. */
public final class MagnificationCapabilities {

    private static final String KEY_CAPABILITY =
            Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CAPABILITY;

    /**
     * Annotation for supported magnification mode.
     *
     * @see Settings.Secure#ACCESSIBILITY_MAGNIFICATION_CAPABILITY
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            MagnificationMode.NONE,
            MagnificationMode.FULLSCREEN,
            MagnificationMode.WINDOW,
            MagnificationMode.ALL,
    })

    public @interface MagnificationMode {
        int NONE = Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_NONE;
        int FULLSCREEN = Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_FULLSCREEN;
        int WINDOW = Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_WINDOW;
        int ALL = Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_ALL;
    }

    /**
     * Gets the summary for the given {@code capabilities}.
     *
     * @param context A {@link Context}.
     * @param capabilities Magnification capabilities {@link MagnificationMode}
     * @return The summary text represents the given capabilities
     */
    public static String getSummary(Context context, @MagnificationMode int capabilities) {
        final String[] summaries = context.getResources().getStringArray(
                R.array.magnification_mode_summaries);
        final int[] values = context.getResources().getIntArray(
                R.array.magnification_mode_values);

        final int idx = Ints.indexOf(values, capabilities);
        return summaries[idx == /* no index exist */ -1 ? 0 : idx];
    }

    /**
     * Sets the magnification capabilities {@link MagnificationMode} to settings key. This
     * overwrites any existing capabilities.
     *
     * @param context      A {@link Context}.
     * @param capabilities Magnification capabilities {@link MagnificationMode}
     */
    public static void setCapabilities(Context context, @MagnificationMode int capabilities) {
        final ContentResolver contentResolver = context.getContentResolver();

        Settings.Secure.putIntForUser(contentResolver, KEY_CAPABILITY, capabilities,
                contentResolver.getUserId());
    }

    /**
     * Returns the magnification capabilities {@link MagnificationMode} from setting's key. May be
     * default value {@link MagnificationMode#FULLSCREEN} if not set.
     *
     * @param context A {@link Context}.
     * @return The magnification capabilities {@link MagnificationMode}
     */
    @MagnificationMode
    public static int getCapabilities(Context context) {
        final ContentResolver contentResolver = context.getContentResolver();

        return Settings.Secure.getIntForUser(contentResolver, KEY_CAPABILITY,
                MagnificationMode.FULLSCREEN, contentResolver.getUserId());
    }

    private MagnificationCapabilities() {}
}
