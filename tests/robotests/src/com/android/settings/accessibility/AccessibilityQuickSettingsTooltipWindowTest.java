/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;

import android.content.Context;
import android.view.View;
import android.widget.PopupWindow;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLooper;

/** Tests for {@link AccessibilityQuickSettingsTooltipWindow}. */
@RunWith(RobolectricTestRunner.class)
public class AccessibilityQuickSettingsTooltipWindowTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private PopupWindow.OnDismissListener mMockOnDismissListener;

    private static final String TEST_PACKAGE_NAME = "com.test.package";
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private AccessibilityQuickSettingsTooltipWindow mToolTipView;
    private View mView;

    @Before
    public void setUp() {
        mToolTipView = new AccessibilityQuickSettingsTooltipWindow(mContext);
        mView = new View(RuntimeEnvironment.application);
    }

    @Test
    public void initToolTipView_atMostAvailableTextWidth() {
        final String quickSettingsTooltipsContent = mContext.getString(
                R.string.accessibility_service_quick_settings_tooltips_content, TEST_PACKAGE_NAME);
        mToolTipView.setup(quickSettingsTooltipsContent);

        final int getMaxWidth = mToolTipView.getAvailableWindowWidth();
        assertThat(mToolTipView.getWidth()).isAtMost(getMaxWidth);
    }

    @Test
    public void showToolTipView_success() {
        mToolTipView.setup(TEST_PACKAGE_NAME);
        assertThat(getLatestPopupWindow()).isNull();

        mToolTipView.showAtTopCenter(mView);

        assertThat(getLatestPopupWindow()).isSameInstanceAs(mToolTipView);
    }

    @Test
    public void dismiss_toolTipViewShown_shouldInvokeCallbackAndNotShowing() {
        mToolTipView.setup(TEST_PACKAGE_NAME);
        mToolTipView.setOnDismissListener(mMockOnDismissListener);
        mToolTipView.showAtTopCenter(mView);

        mToolTipView.dismiss();

        verify(mMockOnDismissListener).onDismiss();
        assertThat(getLatestPopupWindow().isShowing()).isFalse();
    }

    @Test
    public void waitAutoCloseDelayTime_toolTipViewShown_shouldInvokeCallbackAndNotShowing() {
        mToolTipView.setup(TEST_PACKAGE_NAME, /* closeDelayTimeMillis= */ 1);
        mToolTipView.setOnDismissListener(mMockOnDismissListener);
        mToolTipView.showAtTopCenter(mView);

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        verify(mMockOnDismissListener).onDismiss();
        assertThat(getLatestPopupWindow().isShowing()).isFalse();
    }

    private static PopupWindow getLatestPopupWindow() {
        final ShadowApplication shadowApplication = Shadow.extract(RuntimeEnvironment.application);
        return shadowApplication.getLatestPopupWindow();
    }
}
