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

package com.android.settings.accounts;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.FooterPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class EnterpriseDisclosurePreferenceControllerTest {
    private static final String TEST_DISCLOSURE = "This is a test disclosure.";

    private Context mContext;
    private EnterpriseDisclosurePreferenceController mController;
    private FooterPreference mPreference;

    @Mock
    private PreferenceScreen mPreferenceScreen;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = spy(new EnterpriseDisclosurePreferenceController(mContext, "my_key"));
        mPreference = spy(new FooterPreference(mContext));
        when(mPreferenceScreen.findPreference(anyString())).thenReturn(mPreference);
    }

    @Test
    public void getAvailabilityStatus_hasDisclosure_shouldBeAvailable() {
        doReturn(TEST_DISCLOSURE).when(mController).getDisclosure();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noDisclosure_shouldBeDisabled() {
        doReturn(null).when(mController).getDisclosure();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void displayPreference_hasDisclosure_shouldSetTitle() {
        doReturn(TEST_DISCLOSURE).when(mController).getDisclosure();

        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreference.getTitle()).isEqualTo(TEST_DISCLOSURE);
    }

    @Test
    public void displayPreference_noDisclosure_shouldBeInvisible() {
        doReturn(null).when(mController).getDisclosure();

        mController.displayPreference(mPreferenceScreen);

        verify(mPreference, never()).setTitle(any());
    }
}
