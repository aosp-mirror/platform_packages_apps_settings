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

package com.android.settings.testutils.shadow;

import android.annotation.SuppressLint;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import org.robolectric.Shadows;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.annotation.Resetter;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.util.ReflectionHelpers;

import javax.annotation.Nullable;

/* Robolectric shadow for the androidx alert dialog. */
@Implements(AlertDialog.class)
public class ShadowAlertDialogCompat extends ShadowDialog {

    @SuppressLint("StaticFieldLeak")
    @Nullable
    private static ShadowAlertDialogCompat sLatestSupportAlertDialog;
    @RealObject
    private AlertDialog mRealAlertDialog;

    @Implementation
    public void show() {
        super.show();
        sLatestSupportAlertDialog = this;
    }

    public CharSequence getMessage() {
        final Object alertController = ReflectionHelpers.getField(mRealAlertDialog, "mAlert");
        return ReflectionHelpers.getField(alertController, "mMessage");
    }

    public CharSequence getTitle() {
        final Object alertController = ReflectionHelpers.getField(mRealAlertDialog, "mAlert");
        return ReflectionHelpers.getField(alertController, "mTitle");
    }

    public View getView() {
        final Object alertController = ReflectionHelpers.getField(mRealAlertDialog, "mAlert");
        return ReflectionHelpers.getField(alertController, "mView");
    }

    @Nullable
    public static AlertDialog getLatestAlertDialog() {
        return sLatestSupportAlertDialog == null
                ? null : sLatestSupportAlertDialog.mRealAlertDialog;
    }

    @Resetter
    public static void reset() {
        sLatestSupportAlertDialog = null;
    }

    public static ShadowAlertDialogCompat shadowOf(AlertDialog alertDialog) {
        return (ShadowAlertDialogCompat) Shadow.extract(alertDialog);
    }

    public void clickOnItem(int index) {
        Shadows.shadowOf(mRealAlertDialog.getListView()).performItemClick(index);
    }
}
