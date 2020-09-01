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

package com.android.settings.security;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.display.AmbientDisplayAlwaysOnPreferenceController;
import com.android.settings.display.AmbientDisplayNotificationsPreferenceController;
import com.android.settings.gestures.DoubleTapScreenPreferenceController;
import com.android.settings.gestures.PickupGesturePreferenceController;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUtils.class, ShadowLockPatternUtils.class})
public class LockscreenDashboardFragmentTest {

    @Mock
    private LockPatternUtils mLockPatternUtils;
    private FakeFeatureFactory mFeatureFactory;
    private TestFragment mTestFragment;
    private Context mContext;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        when(mFeatureFactory.securityFeatureProvider.getLockPatternUtils(any(Context.class)))
                .thenReturn(mLockPatternUtils);
        mContext = RuntimeEnvironment.application;
        mTestFragment = spy(new TestFragment());
    }

    @Test
    public void containsNotificationSettingsForPrimaryUserAndWorkProfile() {
        List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(RuntimeEnvironment.application,
                mTestFragment.getPreferenceScreenResId());

        assertThat(keys).containsAllOf(LockscreenDashboardFragment.KEY_LOCK_SCREEN_NOTIFICATON,
                LockscreenDashboardFragment.KEY_LOCK_SCREEN_NOTIFICATON_WORK_PROFILE,
                LockscreenDashboardFragment.KEY_LOCK_SCREEN_NOTIFICATON_WORK_PROFILE_HEADER);
    }

    @Test
    public void onAttach_alwaysOn_shouldInvokeSetters() {
        final AmbientDisplayAlwaysOnPreferenceController controller = spy(
                new AmbientDisplayAlwaysOnPreferenceController(mContext, "key"));
        doReturn(controller).when(mTestFragment).use(
                AmbientDisplayAlwaysOnPreferenceController.class);

        mTestFragment.onAttach(mContext);
        verify(controller).setConfig(any());
        verify(controller).setCallback(any());
    }

    @Test
    public void onAttach_notifications_shouldInvokeSetters() {
        final AmbientDisplayNotificationsPreferenceController controller = spy(
                new AmbientDisplayNotificationsPreferenceController(mContext, "key"));
        doReturn(controller).when(mTestFragment).use(
                AmbientDisplayNotificationsPreferenceController.class);

        mTestFragment.onAttach(mContext);
        verify(controller).setConfig(any());
    }

    @Test
    public void onAttach_doubleTap_shouldInvokeSetters() {
        final DoubleTapScreenPreferenceController controller = spy(
                new DoubleTapScreenPreferenceController(mContext, "key"));
        doReturn(controller).when(mTestFragment).use(DoubleTapScreenPreferenceController.class);

        mTestFragment.onAttach(mContext);
        verify(controller).setConfig(any());
    }

    @Test
    public void onAttach_pickUp_shouldInvokeSetters() {
        final PickupGesturePreferenceController controller = spy(
                new PickupGesturePreferenceController(mContext, "key"));
        doReturn(controller).when(mTestFragment).use(PickupGesturePreferenceController.class);

        mTestFragment.onAttach(mContext);
        verify(controller).setConfig(any());
    }

    @Test
    public void isPageSearchable_notLocked_shouldBeSearchable() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(false);
        when(mLockPatternUtils.isLockScreenDisabled(anyInt())).thenReturn(true);

        assertThat(LockscreenDashboardFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext))
                .doesNotContain("security_lockscreen_settings_screen");
    }

    public static class TestFragment extends LockscreenDashboardFragment {
        @Override
        protected <T extends AbstractPreferenceController> T use(Class<T> clazz) {
            return super.use(clazz);
        }
    }
}
