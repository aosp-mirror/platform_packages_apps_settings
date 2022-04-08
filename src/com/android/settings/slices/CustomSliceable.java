/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.slices;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.slice.Slice;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


/**
 * Common functions for custom Slices.
 * <p>
 *     A template for all Settings slices which are not represented by a Preference. By
 *     standardizing the methods used by the Slice helpers, we can use generically take actions
 *     rather than maintaining a list of all of the custom slices every time we reference Slices in
 *     Settings.
 * <p>
 *     By default, all Slices in Settings should be built through Preference Controllers extending
 *     {@link com.android.settings.core.BasePreferenceController}, which are automatically piped
 *     into Settings-Slices infrastructure. Cases where you should implement this interface are:
 *     <ul>
 *         <li>Multi-line slices</li>
 *         <li>Slices that don't exist in the UI</li>
 *         <li>Preferences that use a supported component, like a Switch Bar</li>
 *     </ul>
 * <p>
 *      Note that if your UI is supported because the Preference is not backed by a
 *      {@link com.android.settings.dashboard.DashboardFragment}, then you should first convert the
 *      existing fragment into a dashboard fragment, and then extend
 *      {@link com.android.settings.core.BasePreferenceController}.
 * <p>
 *     If you implement this interface, you should add your Slice to {@link CustomSliceManager}.
 */
public interface CustomSliceable extends Sliceable {

    /**
     * The color representing not to be tinted for the slice.
     */
    int COLOR_NOT_TINTED = -1;

    /**
     * @return an complete instance of the {@link Slice}.
     */
    Slice getSlice();

    /**
     * @return a {@link android.content.ContentResolver#SCHEME_CONTENT content} {@link Uri} which
     * backs the {@link Slice} returned by {@link #getSlice()}.
     */
    Uri getUri();

    /**
     * Handles the actions sent by the {@link Intent intents} bound to the {@link Slice} returned by
     * {@link #getSlice()}.
     *
     * @param intent which has the action taken on a {@link Slice}.
     */
    default void onNotifyChange(Intent intent) {}

    /**
     * @return an {@link Intent} to the source of the Slice data.
     */
    Intent getIntent();

    /**
     * Standardize the intents returned to indicate actions by the Slice.
     * <p>
     *     The {@link PendingIntent} is linked to {@link SliceBroadcastReceiver} where the Intent
     *     Action is found by {@code getUri().toString()}.
     *
     * @return a {@link PendingIntent} linked to {@link SliceBroadcastReceiver}.
     */
    default PendingIntent getBroadcastIntent(Context context) {
        final Intent intent = new Intent(getUri().toString())
                .setData(getUri())
                .setClass(context, SliceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(context, 0 /* requestCode */, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    default boolean isSliceable() {
        return true;
    }

    /**
     * Build an instance of a {@link CustomSliceable} which has a {@link Context}-only constructor.
     */
    static CustomSliceable createInstance(Context context,
            Class<? extends CustomSliceable> sliceable) {
        try {
            final Constructor<? extends CustomSliceable> constructor =
                    sliceable.getConstructor(Context.class);
            final Object[] params = new Object[]{context.getApplicationContext()};
            return constructor.newInstance(params);
        } catch (NoSuchMethodException | InstantiationException |
                IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
            throw new IllegalStateException(
                    "Invalid sliceable class: " + sliceable, e);
        }
    }
}