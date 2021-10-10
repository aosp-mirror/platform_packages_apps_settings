/*
 * Copyright (C) 2021 The Android Open Source Project
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
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.ImageView;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.widget.LayoutPreference;

/** Preference controller that controls the preview effect in accessibility button page. */
public class AccessibilityButtonPreviewPreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnResume, OnPause {

    private static final int SMALL_SIZE = 0;
    private static final float DEFAULT_OPACITY = 0.55f;
    private static final int DEFAULT_SIZE = 0;

    private final ContentResolver mContentResolver;
    @VisibleForTesting
    final ContentObserver mContentObserver;
    private FloatingMenuLayerDrawable mFloatingMenuPreviewDrawable;

    @VisibleForTesting
    ImageView mPreview;

    public AccessibilityButtonPreviewPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContentResolver = context.getContentResolver();
        mContentObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                updatePreviewPreference();
            }
        };
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final LayoutPreference preference = screen.findPreference(getPreferenceKey());
        mPreview = preference.findViewById(R.id.preview_image);

        updatePreviewPreference();
    }

    @Override
    public void onResume() {
        mContentResolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_BUTTON_MODE),
                /* notifyForDescendants= */ false, mContentObserver);
        mContentResolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_FLOATING_MENU_SIZE),
                /* notifyForDescendants= */ false, mContentObserver);
        mContentResolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_FLOATING_MENU_OPACITY),
                /* notifyForDescendants= */ false, mContentObserver);
    }

    @Override
    public void onPause() {
        mContentResolver.unregisterContentObserver(mContentObserver);
    }

    private void updatePreviewPreference() {
        if (AccessibilityUtil.isFloatingMenuEnabled(mContext)) {
            final int size = Settings.Secure.getInt(mContentResolver,
                    Settings.Secure.ACCESSIBILITY_FLOATING_MENU_SIZE, DEFAULT_SIZE);
            final int opacity = (int) (Settings.Secure.getFloat(mContentResolver,
                    Settings.Secure.ACCESSIBILITY_FLOATING_MENU_OPACITY, DEFAULT_OPACITY) * 100);
            final int floatingMenuIconId = (size == SMALL_SIZE)
                    ? R.drawable.accessibility_button_preview_small_floating_menu
                    : R.drawable.accessibility_button_preview_large_floating_menu;

            mPreview.setImageDrawable(getFloatingMenuPreviewDrawable(floatingMenuIconId, opacity));
            // Only change opacity(alpha) would not invoke redraw view, need to invalidate manually.
            mPreview.invalidate();
        } else {
            mPreview.setImageDrawable(
                    mContext.getDrawable(R.drawable.accessibility_button_navigation));
        }
    }

    private Drawable getFloatingMenuPreviewDrawable(int resId, int opacity) {
        if (mFloatingMenuPreviewDrawable == null) {
            mFloatingMenuPreviewDrawable = FloatingMenuLayerDrawable.createLayerDrawable(
                    mContext, resId, opacity);
        } else {
            mFloatingMenuPreviewDrawable.updateLayerDrawable(mContext, resId, opacity);
        }

        return mFloatingMenuPreviewDrawable;
    }
}
