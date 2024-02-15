/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.applications.credentials;

import android.content.Context;
import android.content.Intent;
import android.credentials.CredentialManager;
import android.credentials.CredentialProviderInfo;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.autofill.AutofillService;
import android.service.autofill.AutofillServiceInfo;
import android.text.TextUtils;
import android.view.autofill.AutofillManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.defaultapps.DefaultAppPreferenceController;
import com.android.settingslib.applications.DefaultAppInfo;
import com.android.settingslib.widget.TwoTargetPreference;

import java.util.ArrayList;
import java.util.List;

public class DefaultCombinedPreferenceController extends DefaultAppPreferenceController {

    private static final Intent AUTOFILL_PROBE = new Intent(AutofillService.SERVICE_INTERFACE);
    private static final String TAG = "DefaultCombinedPreferenceController";

    private final AutofillManager mAutofillManager;
    private final CredentialManager mCredentialManager;

    public DefaultCombinedPreferenceController(Context context) {
        super(context);

        mAutofillManager = mContext.getSystemService(AutofillManager.class);

        if (CredentialManager.isServiceEnabled(context)) {
            mCredentialManager = mContext.getSystemService(CredentialManager.class);
        } else {
            mCredentialManager = null;
        }
    }

    @Override
    public boolean isAvailable() {
        return mAutofillManager != null
                && mCredentialManager != null
                && mAutofillManager.hasAutofillFeature()
                && mAutofillManager.isAutofillSupported();
    }

    @Override
    public String getPreferenceKey() {
        return "default_credman_autofill_main";
    }

    @Override
    protected Intent getSettingIntent(DefaultAppInfo info) {
        // Despite this method being called getSettingIntent this intent actually
        // opens the primary picker. This is so that we can swap the cog and the left
        // hand side presses to align the UX.
        if (PrimaryProviderPreference.shouldUseNewSettingsUi()) {
            // We need to return an empty intent here since the class we inherit
            // from will throw an NPE if we return null and we don't want it to
            // open anything since we added the buttons.
            return new Intent();
        }
        return createIntentToOpenPicker();
    }

    @Override
    public void updateState(@NonNull Preference preference) {
        final CombinedProviderInfo topProvider = getTopProvider();
        if (topProvider != null && mContext != null) {
            updatePreferenceForProvider(
                    preference,
                    topProvider.getAppName(mContext),
                    topProvider.getSettingsSubtitle(),
                    topProvider.getAppIcon(mContext, getUser()),
                    topProvider.getPackageName(),
                    topProvider.getSettingsActivity());
        } else {
            updatePreferenceForProvider(preference, null, null, null, null, null);
        }
    }

    @VisibleForTesting
    public void updatePreferenceForProvider(
            Preference preference,
            @Nullable CharSequence appName,
            @Nullable String appSubtitle,
            @Nullable Drawable appIcon,
            @Nullable CharSequence packageName,
            @Nullable CharSequence settingsActivity) {
        if (appName == null) {
            preference.setTitle(R.string.credman_app_list_preference_none);
        } else {
            preference.setTitle(appName);
        }

        if (appIcon == null) {
            preference.setIcon(null);
        } else {
            preference.setIcon(Utils.getSafeIcon(appIcon));
        }

        preference.setSummary(appSubtitle);

        if (preference instanceof PrimaryProviderPreference) {
            PrimaryProviderPreference primaryPref = (PrimaryProviderPreference) preference;
            primaryPref.setIconSize(TwoTargetPreference.ICON_SIZE_MEDIUM);
            primaryPref.setDelegate(
                    new PrimaryProviderPreference.Delegate() {
                        public void onOpenButtonClicked() {
                            CombinedProviderInfo.launchSettingsActivityIntent(
                                    mContext, packageName, settingsActivity, getUser());
                        }

                        public void onChangeButtonClicked() {
                            startActivity(createIntentToOpenPicker());
                        }
                    });

            // Hide the open button if there is no defined settings activity.
            primaryPref.setOpenButtonVisible(!TextUtils.isEmpty(settingsActivity));
            primaryPref.setButtonsCompactMode(appName != null);
        }
    }

    private @Nullable CombinedProviderInfo getTopProvider() {
        return CombinedProviderInfo.getTopProvider(getAllProviders(getUser()));
    }

    @Override
    protected DefaultAppInfo getDefaultAppInfo() {
        return null;
    }

    private List<CombinedProviderInfo> getAllProviders(int userId) {
        final List<AutofillServiceInfo> autofillProviders =
                AutofillServiceInfo.getAvailableServices(mContext, userId);
        final String selectedAutofillProvider =
                Settings.Secure.getStringForUser(
                        mContext.getContentResolver(),
                        DefaultCombinedPicker.AUTOFILL_SETTING,
                        userId);

        final List<CredentialProviderInfo> credManProviders = new ArrayList<>();
        if (mCredentialManager != null) {
            credManProviders.addAll(
                    mCredentialManager.getCredentialProviderServices(
                            userId, CredentialManager.PROVIDER_FILTER_USER_PROVIDERS_ONLY));
        }

        return CombinedProviderInfo.buildMergedList(
                autofillProviders, credManProviders, selectedAutofillProvider);
    }

    @Override
    protected boolean showLabelAsTitle() {
        return true;
    }

    @Override
    protected boolean showAppSummary() {
        return true;
    }

    protected int getUser() {
        return UserHandle.myUserId();
    }

    /** Creates an intent to open the credential picker. */
    private Intent createIntentToOpenPicker() {
        final Context context =
                mContext.createContextAsUser(UserHandle.of(getUser()), /* flags= */ 0);
        return new Intent(context, CredentialsPickerActivity.class);
    }
}
