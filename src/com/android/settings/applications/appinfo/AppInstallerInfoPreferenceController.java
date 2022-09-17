/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.applications.appinfo;

import static com.android.settings.applications.mobilebundledapps.MobileBundledAppDetailsActivity.ACTION_TRANSPARENCY_METADATA;

import android.content.Context;
import android.content.Intent;
import android.os.UserManager;
import android.provider.DeviceConfig;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.AppStoreUtil;
import com.android.settings.applications.mobilebundledapps.ApplicationMetadataUtils;
import com.android.settingslib.applications.AppUtils;
public class AppInstallerInfoPreferenceController extends AppInfoPreferenceControllerBase {

    private static final String KEY_ENABLE_PROMPT = "enable_prompt";
    private String mPackageName;
    private String mInstallerPackage;
    private CharSequence mInstallerLabel;
    private Boolean mAppIsMbaWithMetadata;
    private Boolean mEnableMbaUiFlag = false;

    public AppInstallerInfoPreferenceController(Context context, String key) {
        super(context, key);
        updateFromDeviceConfigFlags();
    }

    @Override
    public int getAvailabilityStatus() {
        if (UserManager.get(mContext).isManagedProfile()
                || AppUtils.isMainlineModule(mContext.getPackageManager(), mPackageName)) {
            return DISABLED_FOR_USER;
        }
        if (mInstallerLabel != null || (mAppIsMbaWithMetadata && mEnableMbaUiFlag)) {
            return AVAILABLE;
        }
        return DISABLED_FOR_USER;
    }

    @Override
    public void updateState(final Preference preference) {
        final int detailsStringId = AppUtils.isInstant(mParent.getPackageInfo().applicationInfo)
                ? R.string.instant_app_details_summary
                : R.string.app_install_details_summary;
        preference.setSummary(mContext.getString(detailsStringId, mInstallerLabel));

        Intent intent = AppStoreUtil.getAppStoreLink(mContext, mInstallerPackage, mPackageName);
        if (intent != null) {
            preference.setIntent(intent);
        } else if (mAppIsMbaWithMetadata && mEnableMbaUiFlag) {
            preference.setIntent(generateMetadataXmlViewerIntent());
            preference.setSummary(mContext.getString(R.string.app_install_details_mba_summary));
        } else {
            preference.setEnabled(false);
        }
    }

    /**
     * Sets the packageName in context for the controller.
     */
    public void setPackageName(final String packageName) {
        mPackageName = packageName;
        mInstallerPackage = AppStoreUtil.getInstallerPackageName(mContext, mPackageName);
        mInstallerLabel = Utils.getApplicationLabel(mContext, mInstallerPackage);
    }

    /**
     * Setups and determines if the current package in context is an mobile-bundled-app with
     * an application metadata file embedded within.
     */
    public void setMbaWithMetadataStatus(final ApplicationMetadataUtils appMetadataUtils,
            final String packageName) {
        mAppIsMbaWithMetadata = appMetadataUtils.packageContainsXmlFile(
                mContext.getPackageManager(), packageName);
    }

    private Intent generateMetadataXmlViewerIntent() {
        final Intent metadataXmlIntent = new Intent(ACTION_TRANSPARENCY_METADATA)
                .setPackage(mContext.getPackageName());
        metadataXmlIntent.putExtra(Intent.EXTRA_PACKAGE_NAME, mPackageName);
        return metadataXmlIntent;
    }

    private void updateFromDeviceConfigFlags() {
        String enablePromptFlag = DeviceConfig.getProperty(
                DeviceConfig.NAMESPACE_TRANSPARENCY_METADATA,
                KEY_ENABLE_PROMPT);
        //No-op for empty field and relies on default value of false
        if (!TextUtils.isEmpty(enablePromptFlag)) {
            setEnableMbaFlag(Boolean.parseBoolean(enablePromptFlag));
        }
    }

    @VisibleForTesting
    void setEnableMbaFlag(final boolean flagValue) {
        mEnableMbaUiFlag = flagValue;
    }
}
