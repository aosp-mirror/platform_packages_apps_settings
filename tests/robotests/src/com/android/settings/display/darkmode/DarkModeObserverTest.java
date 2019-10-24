/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.display.darkmode;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.provider.Settings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;


@RunWith(RobolectricTestRunner.class)
public class DarkModeObserverTest {
    private Context mContext;
    private ContentObserver mContentObserver;
    private DarkModeObserver mDarkModeObserver;
    private Runnable mCallback;
    private Uri mUri = Settings.Secure.getUriFor(Settings.Secure.UI_NIGHT_MODE);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mDarkModeObserver = new DarkModeObserver(mContext);
        mContentObserver = mDarkModeObserver.getContentObserver();
        mCallback = mock(Runnable.class);
    }

    @Test
    public void callbackTest_subscribedCallbackCalled() {
        mDarkModeObserver.subscribe(mCallback);
        mContentObserver.onChange(false, mUri);
        Mockito.verify(mCallback, times(2)).run();
    }

    @Test
    public void callbackTest_unsubscribedCallNotbackCalled() {
        mDarkModeObserver.subscribe(mCallback);
        mContentObserver.onChange(false, mUri);
        mDarkModeObserver.unsubscribe();
        mContentObserver.onChange(false, mUri);
        Mockito.verify(mCallback, times(2)).run();
    }
}
