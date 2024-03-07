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
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.Context;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
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
    private static final int TEST_RES_ID = 1234;
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private AccessibilityQuickSettingsTooltipWindow mTooltipView;
    private View mView;

    @Before
    public void setUp() {
        mTooltipView = new AccessibilityQuickSettingsTooltipWindow(mContext);
        mView = new View(mContext);
    }

    @Test
    public void initTooltipView_atMostAvailableTextWidth() {
        final String quickSettingsTooltipsContent = mContext.getString(
                R.string.accessibility_service_qs_tooltip_content, TEST_PACKAGE_NAME);
        mTooltipView.setup(quickSettingsTooltipsContent, TEST_RES_ID);

        final int getMaxWidth = mTooltipView.getAvailableWindowWidth();
        assertThat(mTooltipView.getWidth()).isAtMost(getMaxWidth);
    }

    @Test
    public void showTooltipView_success() {
        mTooltipView.setup(TEST_PACKAGE_NAME, TEST_RES_ID);
        assertThat(getLatestPopupWindow()).isNull();

        mTooltipView.showAtTopCenter(mView);

        assertThat(getLatestPopupWindow()).isSameInstanceAs(mTooltipView);
    }

    @Test
    public void accessibilityClickActionOnTooltipViewShown_shouldInvokeCallbackAndNotShowing() {
        mTooltipView.setup(TEST_PACKAGE_NAME, TEST_RES_ID);
        mTooltipView.setOnDismissListener(mMockOnDismissListener);
        mTooltipView.showAtTopCenter(mView);

        final boolean isActionPerformed =
                mTooltipView.getContentView().performAccessibilityAction(
                        AccessibilityNodeInfo.ACTION_CLICK,
                        /* arguments= */ null);

        assertThat(isActionPerformed).isTrue();
        verify(mMockOnDismissListener).onDismiss();
        assertThat(getLatestPopupWindow().isShowing()).isFalse();
    }

    @Test
    public void dismiss_tooltipViewShown_shouldInvokeCallbackAndNotShowing() {
        mTooltipView.setup(TEST_PACKAGE_NAME, TEST_RES_ID);
        mTooltipView.setOnDismissListener(mMockOnDismissListener);
        mTooltipView.showAtTopCenter(mView);

        mTooltipView.dismiss();

        verify(mMockOnDismissListener).onDismiss();
        assertThat(getLatestPopupWindow().isShowing()).isFalse();
    }

    @Test
    public void waitAutoCloseDelayTime_tooltipViewShown_shouldInvokeCallbackAndNotShowing() {
        mTooltipView.setup(TEST_PACKAGE_NAME, TEST_RES_ID, /* closeDelayTimeMillis= */ 1);
        mTooltipView.setOnDismissListener(mMockOnDismissListener);
        mTooltipView.showAtTopCenter(mView);

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        verify(mMockOnDismissListener).onDismiss();
        assertThat(getLatestPopupWindow().isShowing()).isFalse();
    }

    private static PopupWindow getLatestPopupWindow() {
        final ShadowApplication shadowApplication = shadowOf(
                (Application) ApplicationProvider.getApplicationContext());
        return shadowApplication.getLatestPopupWindow();
    }
}
