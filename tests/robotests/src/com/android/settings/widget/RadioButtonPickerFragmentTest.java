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

package com.android.settings.widget;

import android.app.Activity;
import android.content.Context;
import android.os.UserManager;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.applications.defaultapps.DefaultAppInfo;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class RadioButtonPickerFragmentTest {


    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Activity mActivity;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private UserManager mUserManager;

    private TestFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mActivity);
        mFragment = spy(new TestFragment());

        when(mActivity.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        doReturn(mActivity).when(mFragment).getContext();
        doReturn(mScreen).when(mFragment).getPreferenceScreen();
    }

    @Test
    public void onAttach_userIsInitialized() {
        mFragment.onAttach((Context) mActivity);

        verify(mActivity).getSystemService(Context.USER_SERVICE);
    }

    @Test
    public void displaySingleOption_shouldSelectRadioButton() {
        final RadioButtonPreference pref =
                new RadioButtonPreference(RuntimeEnvironment.application);
        when(mScreen.getPreferenceCount()).thenReturn(1);
        when(mScreen.getPreference(0)).thenReturn(pref);

        mFragment.mayCheckOnlyRadioButton();

        assertThat(pref.isChecked()).isTrue();
    }

    @Test
    public void clickPreference_shouldConfirm() {
        final RadioButtonPreference pref =
                new RadioButtonPreference(RuntimeEnvironment.application);
        pref.setKey("TEST");

        mFragment.onRadioButtonClicked(pref);

        assertThat(mFragment.setDefaultKeyCalled).isTrue();
    }

    public static class TestFragment extends RadioButtonPickerFragment {

        boolean setDefaultKeyCalled;

        @Override
        public int getMetricsCategory() {
            return 0;
        }

        @Override
        protected List<DefaultAppInfo> getCandidates() {
            return new ArrayList<>();
        }

        @Override
        protected String getDefaultKey() {
            return null;
        }

        @Override
        protected boolean setDefaultKey(String key) {
            setDefaultKeyCalled = true;
            return true;
        }

        @Override
        public Context getContext() {
            return RuntimeEnvironment.application;
        }
    }
}
