/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.settings.connecteddevice.display;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.view.Display;

import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

/** Unit tests for {@link ExternalDisplayUpdater}.  */
@RunWith(AndroidJUnit4.class)
public class ExternalDisplayUpdaterTest extends ExternalDisplayTestBase {

    private ExternalDisplayUpdater mUpdater;
    @Mock
    private DevicePreferenceCallback mMockedCallback;
    @Mock
    private Drawable mMockedDrawable;
    private RestrictedPreference mPreferenceAdded;
    private RestrictedPreference mPreferenceRemoved;

    @Before
    public void setUp() throws RemoteException {
        super.setUp();
        mUpdater = new TestableExternalDisplayUpdater(mMockedCallback, /*metricsCategory=*/ 0);
    }

    @Test
    public void testPreferenceAdded() {
        doAnswer((v) -> {
            mPreferenceAdded = v.getArgument(0);
            return null;
        }).when(mMockedCallback).onDeviceAdded(any());
        mUpdater.initPreference(mContext, mMockedInjector);
        mUpdater.registerCallback();
        mHandler.flush();
        assertThat(mPreferenceAdded).isNotNull();
        var summary = mPreferenceAdded.getSummary();
        assertThat(summary).isNotNull();
        assertThat(summary.length()).isGreaterThan(0);
        var title = mPreferenceAdded.getTitle();
        assertThat(title).isNotNull();
        assertThat(title.length()).isGreaterThan(0);
    }

    @Test
    public void testPreferenceRemoved() {
        doAnswer((v) -> {
            mPreferenceAdded = v.getArgument(0);
            return null;
        }).when(mMockedCallback).onDeviceAdded(any());
        doAnswer((v) -> {
            mPreferenceRemoved = v.getArgument(0);
            return null;
        }).when(mMockedCallback).onDeviceRemoved(any());
        mUpdater.initPreference(mContext, mMockedInjector);
        mUpdater.registerCallback();
        mHandler.flush();
        assertThat(mPreferenceAdded).isNotNull();
        assertThat(mPreferenceRemoved).isNull();
        // Remove display
        doReturn(new Display[0]).when(mMockedInjector).getAllDisplays();
        doReturn(new Display[0]).when(mMockedInjector).getEnabledDisplays();
        mListener.onDisplayRemoved(1);
        mHandler.flush();
        assertThat(mPreferenceRemoved).isEqualTo(mPreferenceAdded);
    }

    class TestableExternalDisplayUpdater extends ExternalDisplayUpdater {
        TestableExternalDisplayUpdater(
                DevicePreferenceCallback callback,
                int metricsCategory) {
            super(callback, metricsCategory);
        }

        @Override
        @Nullable
        protected RestrictedLockUtils.EnforcedAdmin checkIfUsbDataSignalingIsDisabled(
                Context context) {
            // if null is returned - usb signalling is enabled
            return null;
        }

        @Override
        @Nullable
        protected Drawable getDrawable(Context context) {
            return mMockedDrawable;
        }
    }
}
