/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.wifi.savedaccesspoints2;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowInteractionJankMonitor;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@Ignore("b/314867581")
@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowInteractionJankMonitor.class)
public class SavedAccessPointsWifiSettings2Test {

    @Mock
    private SubscribedAccessPointsPreferenceController2 mSubscribedApController;
    @Mock
    private SavedAccessPointsPreferenceController2 mSavedApController;

    private TestFragment mSettings;
    private Context mContext;
    private FragmentActivity mActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mSettings = spy(new TestFragment());
        mActivity = Robolectric.setupActivity(FragmentActivity.class);

        doReturn(mSubscribedApController).when(mSettings)
                .use(SubscribedAccessPointsPreferenceController2.class);
        doReturn(mSavedApController).when(mSettings)
                .use(SavedAccessPointsPreferenceController2.class);
    }

    @Test
    public void verifyConstants() {
        assertThat(mSettings.getMetricsCategory()).isEqualTo(MetricsEvent.WIFI_SAVED_ACCESS_POINTS);
        assertThat(mSettings.getPreferenceScreenResId())
                .isEqualTo(R.xml.wifi_display_saved_access_points2);
    }

    @Test
    public void getTag_shouldReturnRightTag() {
        assertThat(mSettings.getLogTag()).isEqualTo(SavedAccessPointsWifiSettings2.TAG);
    }

    @Test
    public void onAttach_shouldCallSavedControllerSetHost() {
        mSettings.onAttach(mContext);

        verify(mSavedApController, times(1)).setHost(any());
    }

    @Test
    public void onAttach_shouldCallSubscriptionControllerSetHost() {
        mSettings.onAttach(mContext);

        verify(mSubscribedApController, times(1)).setHost(any());
    }

    @Test
    @Ignore
    public void onCreate_shouldNewSavedNetworkTracker() {
        mSettings = new TestFragment();
        final FragmentManager fragmentManager = mActivity.getSupportFragmentManager();
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(mSettings, null /* tag */);
        fragmentTransaction.commit();
        final Bundle bundle = new Bundle();

        mSettings.onCreate(bundle);

        assertThat(mSettings.mSavedNetworkTracker).isNotNull();
    }

    @Test
    @Ignore
    public void onDestroy_shouldTerminateWorkerThread() {
        mSettings = new TestFragment();
        final FragmentManager fragmentManager = mActivity.getSupportFragmentManager();
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(mSettings, null /* tag */);
        fragmentTransaction.commit();
        final Bundle bundle = new Bundle();
        mSettings.onCreate(bundle);

        mSettings.onDestroy();

        assertThat(mSettings.mWorkerThread.getState()).isEqualTo(Thread.State.TERMINATED);
    }

    public static class TestFragment extends SavedAccessPointsWifiSettings2 {

        public <T extends AbstractPreferenceController> T use(Class<T> clazz) {
            return super.use(clazz);
        }
    }
}
