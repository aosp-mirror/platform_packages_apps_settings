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

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceAvailabilityObserver;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Common base for testing subclasses of {@link FailedPasswordWipePreferenceControllerBase}.
 */
public abstract class FailedPasswordWipePreferenceControllerTestBase {

    protected final String mKey;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    protected Context mContext;
    protected FakeFeatureFactory mFeatureFactory;
    @Mock private PreferenceAvailabilityObserver mObserver;

    protected FailedPasswordWipePreferenceControllerBase mController;

    public FailedPasswordWipePreferenceControllerTestBase(String key) {
        mKey = key;
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
    }

    @Test
    public void testGetAvailabilityObserver() {
        mController.setAvailabilityObserver(mObserver);
        assertThat(mController.getAvailabilityObserver()).isEqualTo(mObserver);
    }

    public abstract void setMaximumFailedPasswordsBeforeWipe(int maximum);

    @Test
    public void testUpdateState() {
        final Preference preference = new Preference(mContext, null, 0, 0);

        setMaximumFailedPasswordsBeforeWipe(10);
        when(mContext.getResources().getQuantityString(
                R.plurals.enterprise_privacy_number_failed_password_wipe, 10, 10))
                .thenReturn("10 attempts");
        mController.updateState(preference);
        assertThat(preference.getSummary()).isEqualTo("10 attempts");
    }

    @Test
    public void testIsAvailable() {
        mController.setAvailabilityObserver(mObserver);

        setMaximumFailedPasswordsBeforeWipe(0);
        assertThat(mController.isAvailable()).isFalse();
        verify(mObserver).onPreferenceAvailabilityUpdated(mKey, false);

        setMaximumFailedPasswordsBeforeWipe(10);
        assertThat(mController.isAvailable()).isTrue();
        verify(mObserver).onPreferenceAvailabilityUpdated(mKey, true);
    }

    @Test
    public void testHandlePreferenceTreeClick() {
        assertThat(mController.handlePreferenceTreeClick(new Preference(mContext, null, 0, 0)))
                .isFalse();
    }

    @Test
    public void testGetPreferenceKey() {
        assertThat(mController.getPreferenceKey()).isEqualTo(mKey);
    }
}
