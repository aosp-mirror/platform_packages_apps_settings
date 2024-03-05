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

package com.android.settings.privatespace;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settingslib.widget.FooterPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class PrivateSpaceFooterPreferenceControllerTest {
    @Mock private Context mContext;
    @Mock FooterPreference mFooterPreference;
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private PrivateSpaceFooterPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        final String preferenceKey = "private_space_footer";

        mController = new PrivateSpaceFooterPreferenceController(mContext, preferenceKey);
    }

    @Test
    public void getAvailabilityStatus_whenFlagsEnabled_returnsAvailable() {
        mSetFlagsRule.enableFlags(android.multiuser.Flags.FLAG_ENABLE_PRIVATE_SPACE_FEATURES);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_whenFlagsDisabled_returnsUnsupportedOnDevice() {
        mSetFlagsRule.disableFlags(android.multiuser.Flags.FLAG_ENABLE_PRIVATE_SPACE_FEATURES);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void setUpFooter_setsLearnMoreTextAndAction() {
        mSetFlagsRule.enableFlags(android.multiuser.Flags.FLAG_ENABLE_PRIVATE_SPACE_FEATURES);

        mController.setupFooter(mFooterPreference);
        verify(mFooterPreference).setLearnMoreAction(any());
        verify(mFooterPreference).setLearnMoreText("Learn more about private space");
    }
}
