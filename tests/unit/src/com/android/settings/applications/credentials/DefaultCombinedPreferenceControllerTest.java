/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.R;
import com.android.settings.testutils.ResourcesUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DefaultCombinedPreferenceControllerTest {

    private Context mContext;
    private PrimaryProviderPreference.Delegate mDelegate;
    private AttributeSet mAttributes;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        if (Looper.myLooper() == null) {
            Looper.prepare(); // needed to create the preference screen
        }
        mDelegate =
                new PrimaryProviderPreference.Delegate() {
                    public void onOpenButtonClicked() {}

                    public void onChangeButtonClicked() {}
                };
    }

    @Test
    public void ensureSettingIntentNullForNewDesign() {
        if (!PrimaryProviderPreference.shouldUseNewSettingsUi()) {
            return;
        }

        // The setting intent should be null for the new design since this
        // is handled by the delegate for the PrimaryProviderPreference.
        DefaultCombinedPreferenceController dcpc =
                new DefaultCombinedPreferenceController(mContext);
        assertThat(dcpc.getSettingIntent(null).getPackage()).isNull();
    }

    @Test
    public void ensureSettingIntentNotNullForOldDesign() {
        if (PrimaryProviderPreference.shouldUseNewSettingsUi()) {
            return;
        }

        // For the old design the setting intent should still be used.
        DefaultCombinedPreferenceController dcpc =
                new DefaultCombinedPreferenceController(mContext);
        assertThat(dcpc.getSettingIntent(null).getPackage()).isNotNull();
    }

    @Test
    public void ensureSettingsActivityIntentCreatedSuccessfully() {
        // Ensure that the settings activity is only created if we haved the right combination
        // of package and class name.
        assertThat(CombinedProviderInfo.createSettingsActivityIntent(null, null)).isNull();
        assertThat(CombinedProviderInfo.createSettingsActivityIntent("", null)).isNull();
        assertThat(CombinedProviderInfo.createSettingsActivityIntent("", "")).isNull();
        assertThat(CombinedProviderInfo.createSettingsActivityIntent("com.test", "")).isNull();
        assertThat(CombinedProviderInfo.createSettingsActivityIntent("com.test", "ClassName"))
                .isNotNull();
    }

    @Test
    public void ensureUpdatePreferenceForProviderPopulatesInfo() {
        if (!PrimaryProviderPreference.shouldUseNewSettingsUi()) {
            return;
        }

        DefaultCombinedPreferenceController dcpc =
                new DefaultCombinedPreferenceController(mContext);
        PrimaryProviderPreference ppp = createTestPreference();
        Drawable appIcon = mContext.getResources().getDrawable(R.drawable.ic_settings_delete);

        // Update the preference to use the provider and make sure the view
        // was updated.
        dcpc.updatePreferenceForProvider(ppp, "App Name", "Subtitle", appIcon, null, null);
        assertThat(ppp.getTitle().toString()).isEqualTo("App Name");
        assertThat(ppp.getSummary().toString()).isEqualTo("Subtitle");
        assertThat(ppp.getIcon()).isEqualTo(appIcon);

        // Set the preference back to none and make sure the view was updated.
        dcpc.updatePreferenceForProvider(ppp, null, null, null, null, null);
        assertThat(ppp.getTitle().toString()).isEqualTo("None");
        assertThat(ppp.getSummary()).isNull();
        assertThat(ppp.getIcon()).isNull();
    }

    private PrimaryProviderPreference createTestPreference() {
        int layoutId =
                ResourcesUtils.getResourcesId(
                        mContext, "layout", "preference_credential_manager_with_buttons");
        PreferenceViewHolder holder =
                PreferenceViewHolder.createInstanceForTests(
                        LayoutInflater.from(mContext).inflate(layoutId, null));
        PreferenceViewHolder holderForTest = spy(holder);
        View gearView = new View(mContext, null);
        int gearId = ResourcesUtils.getResourcesId(mContext, "id", "settings_button");
        when(holderForTest.findViewById(gearId)).thenReturn(gearView);

        PrimaryProviderPreference ppp = new PrimaryProviderPreference(mContext, mAttributes);
        ppp.setDelegate(mDelegate);
        ppp.onBindViewHolder(holderForTest);
        return ppp;
    }
}
