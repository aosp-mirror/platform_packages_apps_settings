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

package com.android.settings.applications.specialaccess.zenaccess;

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.ActivityManager;
import android.content.Context;
import android.provider.Settings;

import androidx.lifecycle.LifecycleOwner;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.shadow.ShadowActivityManager;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowActivityManager.class,
})
public class ZenAccessSettingObserverMixinTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private ZenAccessSettingObserverMixin.Listener mListener;

    private Context mContext;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private ZenAccessSettingObserverMixin mMixin;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);

        mMixin = new ZenAccessSettingObserverMixin(mContext, mListener);

        mLifecycle.addObserver(mMixin);
    }

    @Test
    public void onStart_shouldRegisterListener() {
        final ShadowActivityManager sam = Shadow.extract(
                mContext.getSystemService(ActivityManager.class));
        sam.setIsLowRamDevice(true);

        mLifecycle.handleLifecycleEvent(ON_START);

        mContext.getContentResolver().notifyChange(Settings.Secure.getUriFor(
                Settings.Secure.ENABLED_NOTIFICATION_LISTENERS), null);
        ShadowLooper.idleMainLooper();

        verify(mListener).onZenAccessPolicyChanged();
    }

    @Test
    public void onStop_shouldUnregisterListener() {
        mLifecycle.handleLifecycleEvent(ON_START);
        mLifecycle.handleLifecycleEvent(ON_STOP);

        mContext.getContentResolver().notifyChange(Settings.Secure.getUriFor(
                Settings.Secure.ENABLED_NOTIFICATION_LISTENERS), null);

        verify(mListener, never()).onZenAccessPolicyChanged();
    }
}
