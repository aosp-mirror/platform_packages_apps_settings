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

package com.android.settings.notification.app;

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.core.lifecycle.HideNonSystemOverlayMixin;

/**
 * Dialog Activity to host channel settings
 */
public class ChannelPanelActivity extends FragmentActivity {

    private static final String TAG = "ChannelPanelActivity";

    final Bundle mBundle = new Bundle();
    NotificationSettings mPanelFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!getIntent().hasExtra(Settings.EXTRA_CHANNEL_FILTER_LIST)) {
            launchFullSettings();
        }

        getApplicationContext().getTheme().rebase();
        createOrUpdatePanel();
        getLifecycle().addObserver(new HideNonSystemOverlayMixin(this));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        createOrUpdatePanel();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void launchFullSettings() {
        Bundle extras = getIntent().getExtras();
        extras.remove(Settings.EXTRA_CHANNEL_FILTER_LIST);
        startActivity(new SubSettingLauncher(this)
                .setDestination(ChannelNotificationSettings.class.getName())
                .setExtras(extras)
                .setSourceMetricsCategory(SettingsEnums.NOTIFICATION_TOPIC_NOTIFICATION)
                .toIntent());
        finish();
    }

    private void createOrUpdatePanel() {
        final Intent callingIntent = getIntent();
        if (callingIntent == null) {
            Log.e(TAG, "Null intent, closing Panel Activity");
            finish();
            return;
        }

        final FragmentManager fragmentManager = getSupportFragmentManager();
        setContentView(R.layout.notification_channel_panel);

        // Move the window to the bottom of screen, and make it take up the entire screen width.
        final Window window = getWindow();
        window.setGravity(Gravity.BOTTOM);
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT);

        findViewById(R.id.done).setOnClickListener(v -> finish());
        findViewById(R.id.see_more).setOnClickListener(v -> launchFullSettings());
        setupNavigationBar();
        mPanelFragment = callingIntent.hasExtra(Settings.EXTRA_CONVERSATION_ID)
                ? new ConversationNotificationSettings()
                : new ChannelNotificationSettings();
        mPanelFragment.setArguments(new Bundle(mBundle));
        fragmentManager.beginTransaction().replace(
                android.R.id.list_container, mPanelFragment).commit();
    }

    /**
     * Adjust bottom edge and color.
     */
    private void setupNavigationBar() {
        // Extend the panel all the way to the bottom of the screen, as opposed to sitting on top of
        // the navigation bar.
        ViewCompat.setOnApplyWindowInsetsListener(getWindow().getDecorView(),
                (v, windowInsets) -> {
                    v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), 0);
                    return windowInsets; // propagate down to panel layout root element
                });

        // When using 3-button navigation in light mode, the system picks white navigation buttons
        // which are not sufficiently contrasted from the panel background.
        WindowInsetsControllerCompat windowInsetsController =
                ViewCompat.getWindowInsetsController(getWindow().getDecorView());

        if (windowInsetsController != null) {
            boolean forceNavigationButtonsDark = !Utils.isNightMode(this);
            windowInsetsController.setAppearanceLightNavigationBars(forceNavigationButtonsDark);
        }
    }
}
