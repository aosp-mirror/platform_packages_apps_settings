/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.lockscreen;

import static android.provider.Settings.Secure.LOCK_SCREEN_NOTIFICATION_MINIMALISM;
import static android.provider.Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.server.notification.Flags;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.IllustrationPreference;
import com.android.settingslib.widget.LayoutPreference;

import java.util.HashMap;
import java.util.Map;

public class MinimalismPreferenceController
        extends BasePreferenceController
        implements LifecycleEventObserver {

    private static final int LS_SHOW_NOTIF_ON = 1;
    private static final int LS_SHOW_NOTIF_OFF = 0;
    private static final int LS_MINIMALISM_OFF = 0;
    private static final int LS_MINIMALISM_ON = 1;
    private static final String KEY_MINIMALISM_PREFERENCE = "ls_minimalism";
    private static final String KEY_FULL_LIST_ILLUSTRATION = "full_list_illustration";
    private static final String KEY_COMPACT_ILLUSTRATION = "compact_illustration";
    private static final Uri URI_LOCK_SCREEN_NOTIFICATION_MINIMALISM =
            Settings.Secure.getUriFor(LOCK_SCREEN_NOTIFICATION_MINIMALISM);
    private static final Uri URI_LOCK_SCREEN_SHOW_NOTIFICATIONS =
            Settings.Secure.getUriFor(LOCK_SCREEN_SHOW_NOTIFICATIONS);

    @Nullable private LayoutPreference mPreference;
    @Nullable private TextView mDescView;
    private Map<Integer, LinearLayout> mButtons = new HashMap<>();
    private Map<Integer, IllustrationPreference> mIllustrations = new HashMap<>();
    private final Map<Integer, Integer> mDescriptionTexts = Map.ofEntries(
            Map.entry(LS_MINIMALISM_OFF, R.string.lock_screen_notifs_full_list_desc),
            Map.entry(LS_MINIMALISM_ON, R.string.lock_screen_notifs_compact_desc)
    );

    private final ContentResolver mContentResolver;

    final ContentObserver mContentObserver = new ContentObserver(
            new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange, @Nullable Uri uri) {
            refreshState(uri);
        }
    };

    public MinimalismPreferenceController(@NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mContentResolver = context.getContentResolver();
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner,
            @NonNull Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_RESUME) {
            mContentResolver.registerContentObserver(
                    URI_LOCK_SCREEN_NOTIFICATION_MINIMALISM,
                    /* notifyForDescendants= */ false,
                    mContentObserver
            );
            mContentResolver.registerContentObserver(
                    URI_LOCK_SCREEN_SHOW_NOTIFICATIONS,
                    /* notifyForDescendants= */ false,
                    mContentObserver
            );
        } else if (event == Lifecycle.Event.ON_PAUSE) {
            mContentResolver.unregisterContentObserver(mContentObserver);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        if (!Flags.notificationMinimalism()) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        if (!lockScreenShowNotification()) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        return AVAILABLE;
    }

    /**
     * @return Whether showing notifications on the lockscreen is enabled.
     */
    private boolean lockScreenShowNotification() {
        return Settings.Secure.getInt(
                mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS,
                LS_SHOW_NOTIF_OFF
        ) == LS_SHOW_NOTIF_ON;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(KEY_MINIMALISM_PREFERENCE);
        mDescView = mPreference.findViewById(R.id.notif_ls_style_desc);

        mButtons = Map.ofEntries(
                Map.entry(LS_MINIMALISM_OFF,
                        mPreference.findViewById(R.id.button_full)),
                Map.entry(LS_MINIMALISM_ON,
                        mPreference.findViewById(R.id.button_compact))
        );

        mIllustrations = Map.ofEntries(
                Map.entry(LS_MINIMALISM_OFF,
                        screen.findPreference(KEY_FULL_LIST_ILLUSTRATION)),
                Map.entry(LS_MINIMALISM_ON,
                        screen.findPreference(KEY_COMPACT_ILLUSTRATION))
        );
        mButtons.forEach((value, button) -> button.setOnClickListener(v ->
                Settings.Secure.putInt(
                        mContext.getContentResolver(),
                        Settings.Secure.LOCK_SCREEN_NOTIFICATION_MINIMALISM,
                        value
                )
        ));

        refreshState(URI_LOCK_SCREEN_NOTIFICATION_MINIMALISM);
    }

    private void highlightButton(int currentValue) {
        mButtons.forEach((value, button) -> button.setSelected(currentValue == value));
    }

    private void highlightIllustration(int currentValue) {
        mIllustrations.forEach((value, preference)
                -> preference.setVisible(currentValue == value));
    }

    private void highlightDescription(int value) {
        if (mDescView == null) return;
        Integer descStringId = mDescriptionTexts.get(value);
        if (descStringId != null) {
            mDescView.setText(descStringId);
        }
    }

    private int getCurrentMinimalismValue() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                LOCK_SCREEN_NOTIFICATION_MINIMALISM, LS_MINIMALISM_ON);
    }

    private void refreshState(@Nullable Uri uri) {
        if (mPreference == null) return;
        if (URI_LOCK_SCREEN_SHOW_NOTIFICATIONS.equals(uri) && !lockScreenShowNotification()) {
            // hide all preferences when showing notifications on lock screen is disabled
            mIllustrations.forEach((value, preference)
                    -> preference.setVisible(false));
            mPreference.setVisible(false);
        } else {
            mPreference.setVisible(isAvailable());
            int currentValue = getCurrentMinimalismValue();
            highlightButton(currentValue);
            highlightIllustration(currentValue);
            highlightDescription(currentValue);
        }
    }
}
