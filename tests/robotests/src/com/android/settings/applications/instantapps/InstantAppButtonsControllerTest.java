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

package com.android.settings.applications.instantapps;

import static com.android.settings.applications.instantapps.InstantAppButtonsController
        .ShowDialogDelegate;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.View;
import android.widget.Button;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.applications.PackageManagerWrapper;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

/** Tests for the InstantAppButtonsController. */
@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = 23)
public class InstantAppButtonsControllerTest {

    private static final String TEST_INSTALLER_PACKAGE_NAME = "com.installer";
    private static final String TEST_INSTALLER_ACTIVITY_NAME = "com.installer.InstallerActivity";
    private static final ComponentName TEST_INSTALLER_COMPONENT =
            new ComponentName(
                    TEST_INSTALLER_PACKAGE_NAME,
                    TEST_INSTALLER_ACTIVITY_NAME);
    private static final String TEST_AIA_PACKAGE_NAME = "test.aia.package";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Context mockContext;
    @Mock
    PackageManager mockPackageManager;
    @Mock
    PackageManagerWrapper mockPackageManagerWrapper;
    @Mock
    View mockView;
    @Mock
    ShowDialogDelegate mockShowDialogDelegate;
    @Mock
    Button mockInstallButton;
    @Mock
    Button mockClearButton;
    @Mock
    MetricsFeatureProvider mockMetricsFeatureProvider;
    @Mock
    ResolveInfo mockResolveInfo;
    @Mock
    ActivityInfo mockActivityInfo;

    private PackageManager stubPackageManager;

    private FakeFeatureFactory fakeFeatureFactory;
    private TestFragment testFragment;
    private InstantAppButtonsController controller;


    private View.OnClickListener receivedListener;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        testFragment = new TestFragment();
        when(mockView.findViewById(R.id.install)).thenReturn(mockInstallButton);
        when(mockView.findViewById(R.id.clear_data)).thenReturn(mockClearButton);
        mockResolveInfo.activityInfo = mockActivityInfo;
        mockActivityInfo.packageName = TEST_INSTALLER_PACKAGE_NAME;
        mockActivityInfo.name = TEST_INSTALLER_ACTIVITY_NAME;
        when(mockContext.getPackageManager()).thenReturn(mockPackageManager);
        when(mockPackageManager.resolveActivity(any(), anyInt())).thenReturn(mockResolveInfo);
        controller = new InstantAppButtonsController(
                mockContext, testFragment, mockView, mockShowDialogDelegate);
        controller.setPackageName(TEST_AIA_PACKAGE_NAME);
        ReflectionHelpers.setField(
                controller, "mPackageManagerWrapper", mockPackageManagerWrapper);
        FakeFeatureFactory.setupForTest(mockContext);
    }

    @Test
    public void testInstallListenerTriggersInstall() {
        doAnswer(invocation -> {
            receivedListener = (View.OnClickListener) invocation.getArguments()[0];
            return null;
        }).when(mockInstallButton).setOnClickListener(any());
        controller.bindButtons();

        assertThat(receivedListener).isNotNull();
        receivedListener.onClick(mockInstallButton);
        assertThat(testFragment.getStartActivityIntent()).isNotNull();
        assertThat(testFragment.getStartActivityIntent().getComponent())
                .isEqualTo(TEST_INSTALLER_COMPONENT);
    }

    @Test
    public void testClearListenerShowsDialog() {
        doAnswer(invocation -> {
            receivedListener = (View.OnClickListener) invocation.getArguments()[0];
            return null;
        }).when(mockClearButton).setOnClickListener(any());
        controller.bindButtons();
        assertThat(receivedListener).isNotNull();
        receivedListener.onClick(mockClearButton);
        verify(mockShowDialogDelegate).showDialog(InstantAppButtonsController.DLG_CLEAR_APP);
    }

    @Test
    public void testDialogInterfaceOnClick_positiveClearsApp() {
        controller.onClick(mock(DialogInterface.class), DialogInterface.BUTTON_POSITIVE);
        verify(mockPackageManagerWrapper)
                .deletePackageAsUser(eq(TEST_AIA_PACKAGE_NAME), any(), anyInt(),anyInt());
    }

    @Test
    public void testDialogInterfaceOnClick_nonPositiveDoesNothing() {
        controller.onClick(mock(DialogInterface.class), DialogInterface.BUTTON_NEGATIVE);
        controller.onClick(mock(DialogInterface.class), DialogInterface.BUTTON_NEUTRAL);
        verifyZeroInteractions(mockPackageManagerWrapper);
    }
    @SuppressLint("ValidFragment")
    private class TestFragment extends Fragment {

        private Intent startActivityIntent;

        public Intent getStartActivityIntent() {
            return startActivityIntent;
        }

        @Override
        public void startActivity(Intent intent) {
            startActivityIntent = intent;
        }
    }
}
