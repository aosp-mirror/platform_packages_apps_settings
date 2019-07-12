/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.homepage.contextualcards.slices;


import static android.hardware.biometrics.BiometricConstants.BIOMETRIC_SUCCESS;

import android.app.PendingIntent;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.biometrics.BiometricManager;
import android.hardware.face.FaceManager;
import android.net.Uri;
import android.os.UserHandle;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.ListBuilder.RowBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.biometrics.face.FaceStatusPreferenceController;
import com.android.settings.security.SecuritySettings;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBuilderUtils;

public class FaceSetupSlice implements CustomSliceable {

    private final Context mContext;

    public FaceSetupSlice(Context context) {
        mContext = context;
    }

    @Override
    public Slice getSlice() {
        final FaceManager faceManager = Utils.getFaceManagerOrNull(mContext);
        if (faceManager == null || faceManager.hasEnrolledTemplates(UserHandle.myUserId())) {
            return null;
        }

        final CharSequence title = mContext.getText(
                R.string.security_settings_face_settings_enroll);
        final ListBuilder listBuilder = new ListBuilder(mContext,
                CustomSliceRegistry.FACE_ENROLL_SLICE_URI, ListBuilder.INFINITY)
                .setAccentColor(Utils.getColorAccentDefaultColor(mContext));
        final IconCompat icon = IconCompat.createWithResource(mContext, R.drawable.ic_face_24dp);
        return listBuilder
                .addRow(buildRowBuilder(title,
                        mContext.getText(R.string.security_settings_face_settings_context_subtitle),
                        icon, mContext, getIntent()))
                .build();
    }

    @Override
    public Uri getUri() {
        return CustomSliceRegistry.FACE_ENROLL_SLICE_URI;
    }

    @Override
    public Intent getIntent() {
        return SliceBuilderUtils.buildSearchResultPageIntent(mContext,
                SecuritySettings.class.getName(),
                FaceStatusPreferenceController.KEY_FACE_SETTINGS,
                mContext.getText(R.string.security_settings_face_settings_enroll).toString(),
                SettingsEnums.SLICE)
                .setClassName(mContext.getPackageName(), SubSettings.class.getName());
    }

    private static RowBuilder buildRowBuilder(CharSequence title, CharSequence subTitle,
            IconCompat icon, Context context, Intent intent) {
        final SliceAction primarySliceAction = SliceAction.createDeeplink(
                PendingIntent.getActivity(context, 0, intent, 0), icon, ListBuilder.ICON_IMAGE,
                title);
        return new RowBuilder()
                .setTitleItem(icon, ListBuilder.ICON_IMAGE)
                .setTitle(title)
                .setSubtitle(subTitle)
                .setPrimaryAction(primarySliceAction);
    }
}