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
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.ResourcesUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PrimaryProviderPreferenceTest {

    private Context mContext;
    private PrimaryProviderPreference.Delegate mDelegate;
    private boolean mReceivedOpenButtonClicked = false;
    private boolean mReceivedChangeButtonClicked = false;
    private AttributeSet mAttributes;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        if (Looper.myLooper() == null) {
            Looper.prepare(); // needed to create the preference screen
        }
        mReceivedOpenButtonClicked = false;
        mReceivedChangeButtonClicked = false;
        mDelegate =
                new PrimaryProviderPreference.Delegate() {
                    public void onOpenButtonClicked() {
                        mReceivedOpenButtonClicked = true;
                    }

                    public void onChangeButtonClicked() {
                        mReceivedChangeButtonClicked = true;
                    }
                };
    }

    @Test
    public void ensureButtonsClicksCallDelegate_newDesign() {
        if (!PrimaryProviderPreference.shouldUseNewSettingsUi()) {
            return;
        }

        PrimaryProviderPreference ppp = createTestPreferenceWithNewLayout();

        // Test that all the views & buttons are bound correctly.
        assertThat(ppp.getOpenButton()).isNotNull();
        assertThat(ppp.getChangeButton()).isNotNull();
        assertThat(ppp.getButtonFrameView()).isNotNull();

        // Test that clicking the open button results in the delegate being
        // called.
        assertThat(mReceivedOpenButtonClicked).isFalse();
        ppp.getOpenButton().performClick();
        assertThat(mReceivedOpenButtonClicked).isTrue();

        // Test that clicking the change button results in the delegate being
        // called.
        assertThat(mReceivedChangeButtonClicked).isFalse();
        ppp.getChangeButton().performClick();
        assertThat(mReceivedChangeButtonClicked).isTrue();
    }

    @Test
    public void ensureButtonsClicksCallDelegate_newDesign_openButtonVisibility() {
        if (!PrimaryProviderPreference.shouldUseNewSettingsUi()) {
            return;
        }

        PrimaryProviderPreference ppp = createTestPreferenceWithNewLayout();

        // Test that the open button is visible.
        assertThat(ppp.getOpenButton()).isNotNull();
        assertThat(ppp.getOpenButton().getVisibility()).isEqualTo(View.GONE);

        // Show the button and make sure the view was updated.
        ppp.setOpenButtonVisible(true);
        assertThat(ppp.getOpenButton().getVisibility()).isEqualTo(View.VISIBLE);

        // Hide the button and make sure the view was updated.
        ppp.setOpenButtonVisible(false);
        assertThat(ppp.getOpenButton().getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void ensureButtonsClicksCallDelegate_newDesign_buttonsHidden() {
        if (!PrimaryProviderPreference.shouldUseNewSettingsUi()) {
            return;
        }

        PrimaryProviderPreference ppp = createTestPreferenceWithNewLayout();

        // Test that the buttons are visible.
        assertThat(ppp.getButtonFrameView()).isNotNull();
        assertThat(ppp.getButtonFrameView().getVisibility()).isEqualTo(View.GONE);
        assertThat(mReceivedChangeButtonClicked).isFalse();

        // If we show the buttons the visiblility should be updated.
        ppp.setButtonsVisible(true);
        assertThat(ppp.getButtonFrameView().getVisibility()).isEqualTo(View.VISIBLE);

        // If we hide the buttons the visibility should be updated.
        ppp.setButtonsVisible(false);
        assertThat(ppp.getButtonFrameView().getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void ensureButtonsClicksCallDelegate_oldDesign() {
        if (PrimaryProviderPreference.shouldUseNewSettingsUi()) {
            return;
        }

        PrimaryProviderPreference ppp = createTestPreference("preference_widget_gear");

        // Test that clicking the preference results in the delegate being
        // called.
        assertThat(mReceivedOpenButtonClicked).isFalse();
        ppp.getOnPreferenceClickListener().onPreferenceClick(ppp);
        assertThat(mReceivedOpenButtonClicked).isTrue();

        // Test that the gear button is present and visible.
        assertThat(ppp.getGearView()).isNotNull();
        assertThat(ppp.getGearView().getVisibility()).isEqualTo(View.VISIBLE);

        // Test that clicking the gear button results in the delegate being
        // called.
        assertThat(mReceivedChangeButtonClicked).isFalse();
        ppp.getGearView().performClick();
        assertThat(mReceivedChangeButtonClicked).isTrue();
    }

    private PrimaryProviderPreference createTestPreferenceWithNewLayout() {
        return createTestPreference("preference_credential_manager_with_buttons");
    }

    private PrimaryProviderPreference createTestPreference(String layoutName) {
        int layoutId = ResourcesUtils.getResourcesId(mContext, "layout", layoutName);
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
