/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.display;

import static com.android.settings.display.SmartAutoRotatePreferenceFragment.AUTO_ROTATE_SWITCH_PREFERENCE_ID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.view.View;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.testutils.ResolveInfoBuilder;
import com.android.settings.widget.SettingsMainSwitchBar;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SmartAutoRotatePreferenceFragmentTest {

    private static final String PACKAGE_NAME = "package_name";

    private SmartAutoRotatePreferenceFragment mFragment;

    private SettingsMainSwitchBar mSwitchBar;

    @Mock
    private PackageManager mPackageManager;

    @Mock
    private View mView;

    @Mock
    private SettingsActivity mActivity;

    @Mock
    private Preference mRotateSwitchPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        final Context context = spy(RuntimeEnvironment.application);
        ContentResolver mContentResolver = RuntimeEnvironment.application.getContentResolver();
        when(context.getPackageManager()).thenReturn(mPackageManager);
        when(context.getContentResolver()).thenReturn(mContentResolver);
        doReturn(PACKAGE_NAME).when(mPackageManager).getRotationResolverPackageName();
        doReturn(PackageManager.PERMISSION_GRANTED).when(mPackageManager).checkPermission(
                Manifest.permission.CAMERA, PACKAGE_NAME);

        final ResolveInfo resolveInfo = new ResolveInfoBuilder(PACKAGE_NAME).build();
        resolveInfo.serviceInfo = new ServiceInfo();
        when(mPackageManager.resolveService(any(), anyInt())).thenReturn(resolveInfo);

        mFragment = spy(new SmartAutoRotatePreferenceFragment());
        when(mActivity.getPackageManager()).thenReturn(mPackageManager);
        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mFragment.getContext()).thenReturn(context);
        doReturn(mView).when(mFragment).getView();

        when(mFragment.findPreference(AUTO_ROTATE_SWITCH_PREFERENCE_ID)).thenReturn(
                mRotateSwitchPreference);

        mSwitchBar = spy(new SettingsMainSwitchBar(context));
        when(mActivity.getSwitchBar()).thenReturn(mSwitchBar);
        doReturn(mSwitchBar).when(mView).findViewById(R.id.switch_bar);
    }


    @Test
    public void createHeader_faceDetectionSupported_switchBarIsEnabled() {
        mFragment.createHeader(mActivity);

        verify(mSwitchBar, times(1)).show();
        verify(mRotateSwitchPreference, times(1)).setVisible(false);
    }

    @Test
    public void createHeader_faceDetectionUnSupported_switchBarIsDisabled() {
        doReturn(null).when(mPackageManager).getRotationResolverPackageName();

        mFragment.createHeader(mActivity);

        verify(mSwitchBar, never()).show();
        verify(mRotateSwitchPreference, never()).setVisible(false);
    }

}
