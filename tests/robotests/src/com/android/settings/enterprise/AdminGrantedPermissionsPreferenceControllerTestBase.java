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
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.core.PreferenceAvailabilityObserver;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Common base for testing subclasses of {@link AdminGrantedPermissionsPreferenceControllerBase}.
 */
public abstract class AdminGrantedPermissionsPreferenceControllerTestBase {
    protected final String mKey;
    protected final String[] mPermissions;
    protected final String mPermissionGroup;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    protected Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    @Mock private PreferenceAvailabilityObserver mObserver;

    protected AdminGrantedPermissionsPreferenceControllerBase mController;

    public AdminGrantedPermissionsPreferenceControllerTestBase(String key, String[] permissions,
            String permissionGroup) {
        mKey = key;
        mPermissions = permissions;
        mPermissionGroup = permissionGroup;
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        mController = createController(true /* async */);
        mController.setAvailabilityObserver(mObserver);
    }

    @Test
    public void testGetAvailabilityObserver() {
        assertThat(mController.getAvailabilityObserver()).isEqualTo(mObserver);
    }

    private void setNumberOfPackagesWithAdminGrantedPermissions(int number, boolean async) {
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                ((ApplicationFeatureProvider.NumberOfAppsCallback)
                        invocation.getArguments()[2]).onNumberOfAppsResult(number);
                return null;
            }}).when(mFeatureFactory.applicationFeatureProvider)
                    .calculateNumberOfAppsWithAdminGrantedPermissions(eq(mPermissions),
                            eq(async), anyObject());
    }

    @Test
    public void testUpdateState() {
        final Preference preference = new Preference(mContext, null, 0, 0);
        preference.setVisible(true);

        setNumberOfPackagesWithAdminGrantedPermissions(0, true /* async */);
        mController.updateState(preference);
        assertThat(preference.isVisible()).isFalse();
        verify(mObserver).onPreferenceAvailabilityUpdated(mKey, false);

        setNumberOfPackagesWithAdminGrantedPermissions(20, true /* async */);
        when(mContext.getResources().getQuantityString(
                R.plurals.enterprise_privacy_number_packages_lower_bound, 20, 20))
                .thenReturn("minimum 20 apps");
        mController.updateState(preference);
        assertThat(preference.getSummary()).isEqualTo("minimum 20 apps");
        assertThat(preference.isVisible()).isTrue();
        verify(mObserver).onPreferenceAvailabilityUpdated(mKey, true);
    }

    @Test
    public void testIsAvailableSync() {
        final AdminGrantedPermissionsPreferenceControllerBase controller
                = createController(false /* async */);
        controller.setAvailabilityObserver(mObserver);

        setNumberOfPackagesWithAdminGrantedPermissions(0, false /* async */);
        assertThat(controller.isAvailable()).isFalse();
        verify(mObserver).onPreferenceAvailabilityUpdated(mKey, false);

        setNumberOfPackagesWithAdminGrantedPermissions(20, false /* async */);
        assertThat(controller.isAvailable()).isTrue();
        verify(mObserver).onPreferenceAvailabilityUpdated(mKey, true);
    }

    @Test
    public void testIsAvailableAsync() {
        setNumberOfPackagesWithAdminGrantedPermissions(0, true /* async */);
        assertThat(mController.isAvailable()).isTrue();
        verify(mObserver, never()).onPreferenceAvailabilityUpdated(eq(mKey), anyBoolean());

        setNumberOfPackagesWithAdminGrantedPermissions(20, true /* async */);
        assertThat(mController.isAvailable()).isTrue();
        verify(mObserver, never()).onPreferenceAvailabilityUpdated(eq(mKey), anyBoolean());
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

    protected abstract AdminGrantedPermissionsPreferenceControllerBase createController(
            boolean async);
}
