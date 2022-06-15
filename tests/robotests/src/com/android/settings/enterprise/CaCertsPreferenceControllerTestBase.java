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

package com.android.settings.enterprise;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Base test class for testing {@link CaCertsPreferenceControllerBase}'s subclass.
 */
public abstract class CaCertsPreferenceControllerTestBase {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    protected Context mContext;
    protected FakeFeatureFactory mFeatureFactory;
    protected CaCertsPreferenceControllerBase mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mController = createController();
    }

    @Test
    public void testUpdateState() {
        final Preference preference = new Preference(mContext, null, 0, 0);

        when(mContext.getResources().getQuantityString(R.plurals.enterprise_privacy_number_ca_certs,
                10, 10)).thenReturn("10 certs");
        mockGetNumberOfCaCerts(10);
        mController.updateState(preference);
        assertThat(preference.getSummary()).isEqualTo("10 certs");
    }

    @Test
    public void testIsAvailable() {
        mockGetNumberOfCaCerts(0);
        assertThat(mController.isAvailable()).isFalse();

        mockGetNumberOfCaCerts(10);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testHandlePreferenceTreeClick() {
        assertThat(mController.handlePreferenceTreeClick(new Preference(mContext, null, 0, 0)))
                .isFalse();
    }

    @Test
    public void testGetPreferenceKey() {
        assertThat(mController.getPreferenceKey()).isEqualTo(getPreferenceKey());
    }

    abstract void mockGetNumberOfCaCerts(int numOfCaCerts);

    abstract String getPreferenceKey();

    abstract CaCertsPreferenceControllerBase createController();
}
