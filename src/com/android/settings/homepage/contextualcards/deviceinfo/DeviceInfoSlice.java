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

package com.android.settings.homepage.contextualcards.deviceinfo;

import android.app.PendingIntent;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.deviceinfo.HardwareInfoPreferenceController;
import com.android.settings.deviceinfo.aboutphone.MyDeviceInfoFragment;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBuilderUtils;
import com.android.settingslib.DeviceInfoUtils;

import java.util.List;

public class DeviceInfoSlice implements CustomSliceable {
    private static final String TAG = "DeviceInfoSlice";

    private final Context mContext;
    private final SubscriptionManager mSubscriptionManager;

    public DeviceInfoSlice(Context context) {
        mContext = context;
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
    }

    @Override
    public Slice getSlice() {
        final IconCompat icon = IconCompat.createWithResource(mContext,
                R.drawable.ic_info_outline_24dp);
        final String title = mContext.getString(R.string.device_info_label);
        final SliceAction primaryAction = SliceAction.createDeeplink(getPrimaryAction(), icon,
                ListBuilder.ICON_IMAGE, title);
        return new ListBuilder(mContext, CustomSliceRegistry.DEVICE_INFO_SLICE_URI,
                ListBuilder.INFINITY)
                .setAccentColor((Utils.getColorAccentDefaultColor(mContext)))
                .setHeader(new ListBuilder.HeaderBuilder().setTitle(title))
                .addRow(new ListBuilder.RowBuilder()
                        .setTitle(getPhoneNumber())
                        .setSubtitle(getDeviceModel())
                        .setPrimaryAction(primaryAction))
                .build();
    }

    @Override
    public Uri getUri() {
        return CustomSliceRegistry.DEVICE_INFO_SLICE_URI;
    }

    @Override
    public Intent getIntent() {
        final String screenTitle = mContext.getText(R.string.device_info_label).toString();
        return SliceBuilderUtils.buildSearchResultPageIntent(mContext,
                MyDeviceInfoFragment.class.getName(), "" /* key */, screenTitle,
                SettingsEnums.SLICE)
                .setClassName(mContext.getPackageName(), SubSettings.class.getName())
                .setData(CustomSliceRegistry.DEVICE_INFO_SLICE_URI);
    }

    private PendingIntent getPrimaryAction() {
        final Intent intent = getIntent();
        return PendingIntent.getActivity(mContext, 0  /* requestCode */, intent, 0  /* flags */);
    }

    @VisibleForTesting
    CharSequence getPhoneNumber() {
        final SubscriptionInfo subscriptionInfo = getFirstSubscriptionInfo();
        if (subscriptionInfo == null) {
            return mContext.getString(R.string.device_info_default);
        }
        final String phoneNumber = DeviceInfoUtils.getFormattedPhoneNumber(mContext,
                subscriptionInfo);
        return TextUtils.isEmpty(phoneNumber) ? mContext.getString(R.string.device_info_default)
                : BidiFormatter.getInstance().unicodeWrap(phoneNumber, TextDirectionHeuristics.LTR);
    }

    private CharSequence getDeviceModel() {
        return HardwareInfoPreferenceController.getDeviceModel();
    }

    @VisibleForTesting
    SubscriptionInfo getFirstSubscriptionInfo() {
        final List<SubscriptionInfo> subscriptionInfoList =
                mSubscriptionManager.getActiveSubscriptionInfoList(true);
        if (subscriptionInfoList == null || subscriptionInfoList.isEmpty()) {
            return null;
        }
        return subscriptionInfoList.get(0);
    }

    @Override
    public void onNotifyChange(Intent intent) {

    }
}
