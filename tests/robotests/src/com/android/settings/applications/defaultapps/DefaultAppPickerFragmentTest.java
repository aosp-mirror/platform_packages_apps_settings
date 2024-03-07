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

package com.android.settings.applications.defaultapps;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.UserManager;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.applications.DefaultAppInfo;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class DefaultAppPickerFragmentTest {

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private UserManager mUserManager;

    private FakeFeatureFactory mFeatureFactory;
    private FragmentActivity mActivity;
    private TestFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mActivity = spy(Robolectric.buildActivity(FragmentActivity.class).get());
        mFragment = spy(new TestFragment());

        when(mActivity.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        doReturn(mActivity).when(mFragment).getContext();
        doReturn(mScreen).when(mFragment).getPreferenceScreen();
    }

    @Test
    public void clickPreference_hasConfirmation_shouldShowConfirmation() {
        mFragment.onAttach((Context) mActivity);
        final SelectorWithWidgetPreference pref =
                new SelectorWithWidgetPreference(RuntimeEnvironment.application);
        pref.setKey("TEST");
        doReturn("confirmation_text").when(mFragment)
                .getConfirmationMessage(any(DefaultAppInfo.class));
        doReturn(mActivity).when(mFragment).getActivity();

        mFragment.onRadioButtonClicked(pref);
    }

    @Test
    public void onRadioButtonConfirmed_shouldLog() {
        mFragment.onAttach((Context) mActivity);
        mFragment.onRadioButtonConfirmed("test_pkg");

        verify(mFeatureFactory.metricsFeatureProvider).action(
                SettingsEnums.PAGE_UNKNOWN,
                MetricsProto.MetricsEvent.ACTION_SETTINGS_UPDATE_DEFAULT_APP,
                mFragment.getMetricsCategory(),
                "test_pkg",
                0);
    }

    public static class TestFragment extends DefaultAppPickerFragment {

        boolean setDefaultAppKeyCalled;

        @Override
        public int getMetricsCategory() {
            return 0;
        }

        @Override
        protected int getPreferenceScreenResId() {
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
            setDefaultAppKeyCalled = true;
            return true;
        }

        @Override
        public Context getContext() {
            return RuntimeEnvironment.application;
        }
    }
}
