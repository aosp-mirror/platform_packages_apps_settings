/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;

import com.android.settings.gestures.DoubleTapScreenPreferenceController;
import com.android.settings.gestures.PickupGesturePreferenceController;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class AmbientDisplaySettingsTest {

    private TestFragment mTestFragment;

    private Context mContext;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mTestFragment = spy(new TestFragment());
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

    public static class TestFragment extends AmbientDisplaySettings {
        @Override
        protected <T extends AbstractPreferenceController> T use(Class<T> clazz) {
            return super.use(clazz);
        }
    }
}