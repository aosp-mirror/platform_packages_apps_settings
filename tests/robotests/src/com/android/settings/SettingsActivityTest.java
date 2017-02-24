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

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SettingsActivityTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private FragmentManager mFragmentManager;

    private SettingsActivity mActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        final FakeFeatureFactory factory =
            (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        when(factory.dashboardFeatureProvider.isEnabled()).thenReturn(true);
    }

    @Test
    public void testQueryTextChange_shouldUpdate() {
        final String testQuery = "abc";
        mActivity = new SettingsActivity();

        assertThat(mActivity.mSearchQuery).isNull();
        try {
            mActivity.onQueryTextChange(testQuery);
        } catch (NullPointerException e) {
            // Expected, because searchFeatureProvider is not wired up.
        }

        assertThat(mActivity.mSearchQuery).isEqualTo(testQuery);
    }

    @Test
    public void launchSettingFragment_nullExtraShowFragment_shouldNotCrash()
            throws ClassNotFoundException {
        mActivity = spy(new SettingsActivity());
        when(mActivity.getFragmentManager()).thenReturn(mFragmentManager);
        when(mFragmentManager.beginTransaction()).thenReturn(mock(FragmentTransaction.class));

        doReturn(RuntimeEnvironment.application.getClassLoader()).when(mActivity).getClassLoader();

        mActivity.launchSettingFragment(null, true, mock(Intent.class));
    }
}
