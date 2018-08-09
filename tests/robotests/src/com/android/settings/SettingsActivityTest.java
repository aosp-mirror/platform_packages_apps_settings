/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.Settings.Global;
import android.view.View;

import com.android.settings.search.SearchActivity;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SettingsActivityTest {

    @Mock
    private FragmentManager mFragmentManager;
    @Mock
    private ActivityManager.TaskDescription mTaskDescription;
    @Mock
    private Bitmap mBitmap;
    @Mock
    private View mSearchBar;
    private SettingsActivity mActivity;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mActivity = spy(new SettingsActivity());
        doReturn(mBitmap).when(mActivity).getBitmapFromXmlResource(anyInt());
        doReturn(mContext.getContentResolver()).when(mActivity).getContentResolver();
        doReturn(mSearchBar).when(mActivity).findViewById(R.id.search_bar);
    }

    @Test
    public void setSearchBarVisibility_deviceNotProvisioned_shouldDisableSearch() {
        Global.putInt(mContext.getContentResolver(), Global.DEVICE_PROVISIONED, 0);

        mActivity.setSearchBarVisibility();

        verify(mSearchBar).setVisibility(View.INVISIBLE);
    }

    @Test
    public void setSearchBarVisibility_deviceProvisioned_shouldEnableSearch() {
        Global.putInt(mContext.getContentResolver(), Global.DEVICE_PROVISIONED, 1);

        mActivity.setSearchBarVisibility();

        verify(mSearchBar).setVisibility(View.VISIBLE);
    }

    @Test
    public void launchSettingFragment_nullExtraShowFragment_shouldNotCrash()
            throws ClassNotFoundException {
        when(mActivity.getFragmentManager()).thenReturn(mFragmentManager);
        when(mFragmentManager.beginTransaction()).thenReturn(mock(FragmentTransaction.class));

        doReturn(RuntimeEnvironment.application.getClassLoader()).when(mActivity).getClassLoader();

        mActivity.launchSettingFragment(null, true, mock(Intent.class));
    }

    @Test
    public void testSetTaskDescription_IconChanged() {
        mActivity.setTaskDescription(mTaskDescription);

        verify(mTaskDescription).setIcon(nullable(Bitmap.class));
    }

    @Test
    public void testSaveState_EnabledHomeSaved() {
        mActivity.mDisplayHomeAsUpEnabled = true;
        Bundle bundle = new Bundle();
        mActivity.saveState(bundle);

        assertThat((boolean) bundle.get(SettingsActivity.SAVE_KEY_SHOW_HOME_AS_UP)).isTrue();
    }

    @Test
    public void testOnClick() {
        doReturn("com.android.settings").when(mActivity).getPackageName();

        mActivity.onClick(null);

        Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(intent.getComponent()).isEqualTo(
                new ComponentName("com.android.settings", SearchActivity.class.getName()));
    }
}
