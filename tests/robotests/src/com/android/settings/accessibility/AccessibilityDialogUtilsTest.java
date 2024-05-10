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

import static com.google.common.truth.Truth.assertThat;

import android.app.Dialog;
import android.content.Context;

import androidx.appcompat.app.AlertDialog;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link AccessibilityDialogUtils} */
@RunWith(RobolectricTestRunner.class)
public class AccessibilityDialogUtilsTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        mContext.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
    }

    @Test
    public void updateShortcutInDialog_correctDialogType_success() {
        final AlertDialog dialog = AccessibilityDialogUtils.showEditShortcutDialog(
                mContext, AccessibilityDialogUtils.DialogType.EDIT_SHORTCUT_GENERIC, "Title",
                null);

        assertThat(
                AccessibilityDialogUtils.updateShortcutInDialog(mContext, dialog)).isTrue();
    }

    @Test
    public void updateShortcutInDialog_useNotSupportedDialog_fail() {
        final AlertDialog dialog = new AlertDialog.Builder(mContext).setTitle("Title").show();

        assertThat(AccessibilityDialogUtils.updateShortcutInDialog(mContext,
                dialog)).isFalse();
    }

    @Test
    public void showDialog_createCustomDialog_isShowing() {
        final Dialog dialog = AccessibilityDialogUtils.createCustomDialog(mContext,
                "Title", /* customView= */ null, "positiveButton", /* positiveListener= */ null,
                "negativeButton", /* negativeListener= */ null);
        dialog.show();

        assertThat(dialog.isShowing()).isTrue();
    }
}
