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

import static com.android.settings.accessibility.AccessibilityGestureNavigationTutorial.createAccessibilityTutorialDialog;
import static com.android.settings.accessibility.AccessibilityGestureNavigationTutorial.createShortcutTutorialPages;
import static com.android.settings.accessibility.AccessibilityUtil.UserShortcutType;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;
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

/** Tests for {@link AccessibilityGestureNavigationTutorial}. */
@RunWith(RobolectricTestRunner.class)
public final class AccessibilityGestureNavigationTutorialTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private DialogInterface.OnClickListener mMockOnClickListener;

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private int mShortcutTypes;

    @Before
    public void setUp() {
        mContext.setTheme(R.style.Theme_AppCompat);
        mShortcutTypes = /* initial */ 0;
    }

    @Test(expected = IllegalArgumentException.class)
    public void createTutorialPages_shortcutListIsEmpty_throwsException() {
        createAccessibilityTutorialDialog(mContext, mShortcutTypes);
    }

    @Test
    public void createTutorialPages_turnOnTripleTapShortcut_hasOnePage() {
        mShortcutTypes |= UserShortcutType.TRIPLETAP;

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(mContext, mShortcutTypes);

        assertThat(createShortcutTutorialPages(mContext,
                mShortcutTypes)).hasSize(/* expectedSize= */ 1);
        assertThat(alertDialog).isNotNull();
    }

    @Test
    public void createTutorialPages_turnOnSoftwareShortcut_hasOnePage() {
        mShortcutTypes |= UserShortcutType.SOFTWARE;

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(mContext, mShortcutTypes);

        assertThat(createShortcutTutorialPages(mContext,
                mShortcutTypes)).hasSize(/* expectedSize= */ 1);
        assertThat(alertDialog).isNotNull();
    }

    @Test
    public void createTutorialPages_turnOnSoftwareAndHardwareShortcuts_hasTwoPages() {
        mShortcutTypes |= UserShortcutType.SOFTWARE;
        mShortcutTypes |= UserShortcutType.HARDWARE;

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(mContext, mShortcutTypes);

        assertThat(createShortcutTutorialPages(mContext,
                mShortcutTypes)).hasSize(/* expectedSize= */ 2);
        assertThat(alertDialog).isNotNull();
    }

    @Test
    public void performClickOnNegativeButton_turnOnSoftwareShortcut_dismiss() {
        mShortcutTypes |= UserShortcutType.SOFTWARE;
        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(mContext, mShortcutTypes);
        alertDialog.show();

        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();

        assertThat(alertDialog.isShowing()).isFalse();
    }

    @Test
    public void performClickOnNegativeButton_turnOnSoftwareShortcut_callOnClickListener() {
        mShortcutTypes |= UserShortcutType.SOFTWARE;
        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(mContext, mShortcutTypes, mMockOnClickListener);
        alertDialog.show();

        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();

        verify(mMockOnClickListener).onClick(alertDialog, DialogInterface.BUTTON_NEGATIVE);
    }
}
