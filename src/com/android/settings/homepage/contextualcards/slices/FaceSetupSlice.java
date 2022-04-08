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


import android.app.PendingIntent;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.hardware.face.FaceManager;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.ListBuilder.RowBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.biometrics.face.FaceStatusPreferenceController;
import com.android.settings.homepage.contextualcards.FaceReEnrollDialog;
import com.android.settings.security.SecuritySettings;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBuilderUtils;

/**
 * This class is used for showing re-enroll suggestions in the Settings page. By either having an
 * un-enrolled user or setting {@link Settings.Secure#FACE_UNLOCK_RE_ENROLL} to one of the
 * states listed in {@link Settings.Secure} the slice will change its text and potentially show
 * a {@link FaceReEnrollDialog}.
 */
public class FaceSetupSlice implements CustomSliceable {

    private final Context mContext;
    /**
     * If a user currently is not enrolled then this class will show a recommendation to
     * enroll their face.
     */
    private FaceManager mFaceManager;

    /**
     * Various states the {@link FaceSetupSlice} can be in,
     * See {@link Settings.Secure#FACE_UNLOCK_RE_ENROLL} for more details.
     */

    // No re-enrollment.
    public static final int FACE_NO_RE_ENROLL_REQUIRED = 0;
    // Re enrollment is suggested.
    public static final int FACE_RE_ENROLL_SUGGESTED = 1;
    // Re enrollment is required after a set time period.
    public static final int FACE_RE_ENROLL_AFTER_TIMEOUT = 2;
    // Re enrollment is required immediately.
    public static final int FACE_RE_ENROLL_REQUIRED = 3;

    public FaceSetupSlice(Context context) {
        mContext = context;
    }

    @Override
    public Slice getSlice() {
        mFaceManager = Utils.getFaceManagerOrNull(mContext);
        if (mFaceManager == null) {
            return new ListBuilder(mContext, CustomSliceRegistry.FACE_ENROLL_SLICE_URI,
                    ListBuilder.INFINITY).setIsError(true).build();
        }

        final int userId = UserHandle.myUserId();
        final boolean hasEnrolledTemplates = mFaceManager.hasEnrolledTemplates(userId);
        final int shouldReEnroll = FaceSetupSlice.getReEnrollSetting(mContext, userId);

        CharSequence title = "";
        CharSequence subtitle = "";

        // Set the title and subtitle according to the different states, the icon and layout will
        // stay the same.
        if (!hasEnrolledTemplates) {
            title = mContext.getText(R.string.security_settings_face_settings_enroll);
            subtitle = mContext.getText(
                    R.string.security_settings_face_settings_context_subtitle);
        } else if (shouldReEnroll == FACE_RE_ENROLL_SUGGESTED) {
            title = mContext.getText(
                    R.string.security_settings_face_enroll_should_re_enroll_title);
            subtitle = mContext.getText(
                    R.string.security_settings_face_enroll_should_re_enroll_subtitle);
        } else if (shouldReEnroll == FACE_RE_ENROLL_REQUIRED) {
            title = mContext.getText(
                    R.string.security_settings_face_enroll_must_re_enroll_title);
            subtitle = mContext.getText(
                    R.string.security_settings_face_enroll_must_re_enroll_subtitle);
        } else {
            return new ListBuilder(mContext, CustomSliceRegistry.FACE_ENROLL_SLICE_URI,
                    ListBuilder.INFINITY).setIsError(true).build();
        }

        final ListBuilder listBuilder = new ListBuilder(mContext,
                CustomSliceRegistry.FACE_ENROLL_SLICE_URI, ListBuilder.INFINITY)
                .setAccentColor(Utils.getColorAccentDefaultColor(mContext));
        final IconCompat icon = IconCompat.createWithResource(mContext, R.drawable.ic_face_24dp);
        return listBuilder.addRow(buildRowBuilder(title, subtitle, icon, mContext, getIntent()))
                .build();
    }

    @Override
    public Uri getUri() {
        return CustomSliceRegistry.FACE_ENROLL_SLICE_URI;
    }

    @Override
    public Intent getIntent() {
        final boolean hasEnrolledTemplates = mFaceManager.hasEnrolledTemplates(
                UserHandle.myUserId());
        if (!hasEnrolledTemplates) {
            return SliceBuilderUtils.buildSearchResultPageIntent(mContext,
                    SecuritySettings.class.getName(),
                    FaceStatusPreferenceController.KEY_FACE_SETTINGS,
                    mContext.getText(R.string.security_settings_face_settings_enroll).toString(),
                    SettingsEnums.SLICE)
                    .setClassName(mContext.getPackageName(), SubSettings.class.getName());
        } else {
            return new Intent(mContext, FaceReEnrollDialog.class);
        }
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

    public static int getReEnrollSetting(Context context, int userId) {
        return Settings.Secure.getIntForUser(context.getContentResolver(),
                Settings.Secure.FACE_UNLOCK_RE_ENROLL, FACE_NO_RE_ENROLL_REQUIRED, userId);
    }

}