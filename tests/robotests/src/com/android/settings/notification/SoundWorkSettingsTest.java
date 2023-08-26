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

package com.android.settings.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.UserHandle;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;

import com.android.settings.DefaultRingtonePreference;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class SoundWorkSettingsTest {

    @Mock
    private FragmentActivity mActivity;

    @Mock
    private MetricsFeatureProvider mMetricsFeatureProvider;

    private FakeFeatureFactory mFeatureFactory;
    private SoundWorkSettings mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFragment = spy(new SoundWorkSettings());
        when(mFragment.getActivity()).thenReturn(mActivity);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mMetricsFeatureProvider = mFeatureFactory.getMetricsFeatureProvider();
        ReflectionHelpers.setField(mFragment, "mMetricsFeatureProvider", mMetricsFeatureProvider);
    }

    @Test
    public void onPreferenceTreeClick_isRingtonePreference_shouldStartActivity() {
        final DefaultRingtonePreference ringtonePreference = mock(DefaultRingtonePreference.class);
        when(mMetricsFeatureProvider.logClickedPreference(any(Preference.class),
                anyInt())).thenReturn(true);

        mFragment.onPreferenceTreeClick(ringtonePreference);

        verify(mActivity).startActivityForResultAsUser(any(), anyInt(), any(),
                any(UserHandle.class));
    }
}
