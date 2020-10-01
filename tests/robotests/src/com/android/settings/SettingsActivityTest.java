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

import static com.android.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.settings.core.OnActivityResultListener;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class SettingsActivityTest {

    @Mock
    private FragmentManager mFragmentManager;
    @Mock
    private ActivityManager.TaskDescription mTaskDescription;
    private SettingsActivity mActivity;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mActivity = spy(Robolectric.buildActivity(SettingsActivity.class).create().get());
    }

    @Test
    public void launchSettingFragment_nullExtraShowFragment_shouldNotCrash() {
        when(mActivity.getSupportFragmentManager()).thenReturn(mFragmentManager);
        doReturn(mContext.getContentResolver()).when(mActivity).getContentResolver();
        when(mFragmentManager.beginTransaction()).thenReturn(mock(FragmentTransaction.class));
        doReturn(RuntimeEnvironment.application.getClassLoader()).when(mActivity).getClassLoader();

        mActivity.launchSettingFragment(null, mock(Intent.class));
    }

    @Test
    public void setTaskDescription_shouldUpdateIcon() {
        mActivity.setTaskDescription(mTaskDescription);

        verify(mTaskDescription).setIcon(any());
    }

    @Test
    public void getSharedPreferences_intentExtraIsNull_shouldNotCrash() {
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_SHOW_FRAGMENT, (String)null);
        doReturn(intent).when(mActivity).getIntent();
        doReturn(mContext.getPackageName()).when(mActivity).getPackageName();
        FakeFeatureFactory.setupForTest();

        mActivity.getSharedPreferences(mContext.getPackageName() + "_preferences", 0);
    }

    @Test
    public void onActivityResult_shouldDelegateToListener() {
        final List<Fragment> fragments = new ArrayList<>();
        fragments.add(new Fragment());
        fragments.add(new ListenerFragment());

        final FragmentManager manager = mock(FragmentManager.class);
        when(mActivity.getSupportFragmentManager()).thenReturn(manager);
        when(manager.getFragments()).thenReturn(fragments);

        mActivity.onActivityResult(0, 0, new Intent());

        assertThat(((ListenerFragment) fragments.get(1)).mOnActivityResultCalled).isTrue();
    }

    public static class ListenerFragment extends Fragment implements OnActivityResultListener {

        private boolean mOnActivityResultCalled;

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            mOnActivityResultCalled = true;
        }
    }
}
