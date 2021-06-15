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
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.Stage;

import com.android.settings.testutils.CommonUtils;
import com.android.settings.testutils.UiUtils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AppNotificationComponentTest {
    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private final String mNoSlientAppName = "com.google.android.dialer";
    public final String TAG = this.getClass().getName();

    @Rule
    public ActivityScenarioRule<com.android.settings.Settings.AppNotificationSettingsActivity>
            rule = new ActivityScenarioRule<>(
            new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, mNoSlientAppName)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

    /**
     * Tests user should not able to modify notification settings for some system apps.
     * In this case, test `phone` app that will disabled notification configuration.
     * Steps:
     * 1. Open notification page of phone app.
     * 2. Checks system privilege notification should not able to be changed.
     */
    @Test
    public void test_special_app_could_not_disable_notification() {
        List<String> disabledList = Arrays.asList("Default", "Incoming calls",
                "Background Processing", "Missed calls",
                "Ongoing calls", "Voicemails");

        ActivityScenario ac = rule.getScenario();
        ac.onActivity(
                activity -> {
                    View recyclerView = activity.findViewById(
                            CommonUtils.getResId("recycler_view"));

                    if (recyclerView == null) {
                        Log.d("UI_UTILS",
                                "Target not found: R.id.recycler_view #" + Integer.toHexString(
                                        CommonUtils.getResId("recycler_view")));
                        UiUtils.dumpView(UiUtils.getFirstViewFromActivity(activity));
                        assertThat(Boolean.TRUE).isFalse();
                    }

                    UiUtils.waitUntilCondition(5000,
                            () -> recyclerView.findViewById(CommonUtils.getResId("recycler_view"))
                                    != null);

                    View mainSwitchBar = recyclerView.findViewById(
                            CommonUtils.getResId("main_switch_bar"));

                    assertThat(mainSwitchBar.isEnabled()).isEqualTo(false);
                    Log.d(TAG, "main switch bar = " + mainSwitchBar.isEnabled());

                    UiUtils.waitForActivitiesInStage(10000, Stage.RESUMED);
                    Log.d(TAG, "In stage!.");

                    UiUtils.dumpView(UiUtils.getFirstViewFromActivity(activity));

                    // The privileges are under the recycle view. Fetch all of them and check.
                    ViewGroup viewGroup = (ViewGroup) recyclerView;

                    for (int i = 0; i < viewGroup.getChildCount(); i++) {
                        if (viewGroup.getChildAt(i) instanceof LinearLayout) {
                            // A notification in Settings should have both switch_widget and text.
                            // There has another circle pin is no belongs to Settings package.
                            // But belongs to Switch in Android.
                            View sWidget = viewGroup.getChildAt(i).findViewById(
                                    CommonUtils.getResId("switchWidget"));
                            TextView sText = viewGroup.getChildAt(i).findViewById(
                                    android.R.id.title);
                            if (sText != null && sWidget != null
                                    && disabledList.stream().anyMatch(
                                            str -> str.equals(sText.getText().toString().trim()))) {

                                assertThat(sWidget.isEnabled()).isFalse();
                            }
                        }
                    }
                }
        );
    }
}
