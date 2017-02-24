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

import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

/**
 * Common base for testing subclasses of {@link AdminGrantedPermissionsPreferenceControllerBase}.
 */
public abstract class AdminGrantedPermissionsPreferenceControllerTestBase {

    protected final String mKey;
    protected final String[] mPermissions;
    protected final int mStringResourceId;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    protected Context mContext;
    private FakeFeatureFactory mFeatureFactory;

    protected AdminGrantedPermissionsPreferenceControllerBase mController;

    public AdminGrantedPermissionsPreferenceControllerTestBase(String key, String[] permissions,
            int stringResourceId) {
        mKey = key;
        mPermissions = permissions;
        mStringResourceId = stringResourceId;
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
    }

    private void setNumberOfPackagesWithAdminGrantedPermissions(int number) {
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                ((ApplicationFeatureProvider.NumberOfAppsCallback)
                        invocation.getArguments()[1]).onNumberOfAppsResult(number);
                return null;
            }}).when(mFeatureFactory.applicationFeatureProvider)
                    .calculateNumberOfAppsWithAdminGrantedPermissions(eq(mPermissions),
                            anyObject());
    }

    @Test
    public void testUpdateState() {
        final Preference preference = new Preference(mContext, null, 0, 0);
        preference.setVisible(false);

        setNumberOfPackagesWithAdminGrantedPermissions(20);
        when(mContext.getResources().getQuantityString(mStringResourceId, 20, 20))
                .thenReturn("20 packages");
        mController.updateState(preference);
        assertThat(preference.getTitle()).isEqualTo("20 packages");
        assertThat(preference.isVisible()).isTrue();

        setNumberOfPackagesWithAdminGrantedPermissions(0);
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
