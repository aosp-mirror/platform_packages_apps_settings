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

import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

/**
 * Common base for testing subclasses of {@link FailedPasswordWipePreferenceControllerBase}.
 */
public abstract class FailedPasswordWipePreferenceControllerTestBase {

    protected final String mKey;
    protected final int mStringResourceId;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    protected Context mContext;
    protected FakeFeatureFactory mFeatureFactory;

    protected FailedPasswordWipePreferenceControllerBase mController;

    public FailedPasswordWipePreferenceControllerTestBase(String key, int stringResourceId) {
        mKey = key;
        mStringResourceId = stringResourceId;
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
    }

    public abstract void setMaximumFailedPasswordsBeforeWipe(int maximum);

    @Test
    public void testUpdateState() {
        final Preference preference = new Preference(mContext, null, 0, 0);
        preference.setVisible(false);

        setMaximumFailedPasswordsBeforeWipe(10);
        when(mContext.getResources().getQuantityString(mStringResourceId, 10, 10))
                .thenReturn("10 attempts");
        mController.updateState(preference);
        assertThat(preference.getTitle()).isEqualTo("10 attempts");
        assertThat(preference.isVisible()).isTrue();

        setMaximumFailedPasswordsBeforeWipe(0);
        mController.updateState(preference);
        assertThat(preference.isVisible()).isFalse();
    }

    @Test
    public void testIsAvailable() {
        assertThat(mController.isAvailable()).isTrue();
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
