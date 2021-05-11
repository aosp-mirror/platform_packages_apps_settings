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

package com.android.settings.notification;

import static com.google.common.truth.Truth.assertThat;

import android.app.Instrumentation;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.Stage;

import com.android.settings.R;
import com.android.settings.testutils.UiUtils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AppNotificationComponentTest {
    private static final String TAG =
            AppNotificationComponentTest.class.getSimpleName();
    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private final String mNoSlientAppName = "com.google.android.dialer";


    @Rule
    public ActivityScenarioRule<com.android.settings.Settings.AppNotificationSettingsActivity>
            rule = new ActivityScenarioRule<>(
            new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, mNoSlientAppName)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

    @Test
    public void test_special_app_could_not_disable_notification() {
        ActivityScenario ac = rule.getScenario();
        ac.onActivity(
                activity -> {
                    View rv = activity.findViewById(R.id.recycler_view);

                    if (rv == null) {
                        Log.d("UI_UTILS",
                                "Target not found: R.id.recycler_view #" + Integer.toHexString(
                                        R.id.recycler_view));
                        UiUtils.dumpView(UiUtils.getFirstViewFromActivity(activity));
                        assertThat(Boolean.TRUE).isFalse();
                    }

                    UiUtils.waitUntilCondition(5000,
                            () -> rv.findViewById(R.id.main_switch_bar) != null);

                    View mainSwitchBar = rv.findViewById(R.id.main_switch_bar);

                    assertThat(mainSwitchBar.isEnabled()).isEqualTo(false);

                    UiUtils.waitForActivitiesInStage(1000, Stage.RESUMED);

                    for (int i = 0; i < ((ViewGroup) rv).getChildCount(); i++) {
                        if (((ViewGroup) rv).getChildAt(i) instanceof LinearLayout) {
                            Switch sWidget = rv.findViewById(R.id.switchWidget);
                            if (sWidget != null) {
                                assertThat(sWidget.isEnabled()).isEqualTo(false);
                            }
                        }
                    }
                }
        );
    }
}
