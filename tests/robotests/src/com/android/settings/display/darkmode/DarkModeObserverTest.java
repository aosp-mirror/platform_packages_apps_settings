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

import static android.provider.Settings.Secure.DARK_THEME_CUSTOM_END_TIME;
import static android.provider.Settings.Secure.DARK_THEME_CUSTOM_START_TIME;
import static android.provider.Settings.Secure.UI_NIGHT_MODE;
import static android.provider.Settings.Secure.UI_NIGHT_MODE_CUSTOM_TYPE;
import static android.provider.Settings.Secure.getUriFor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.ParameterizedRobolectricTestRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(ParameterizedRobolectricTestRunner.class)
public class DarkModeObserverTest {
    @ParameterizedRobolectricTestRunner.Parameters(name = "uri: {0}")
    public static List<Object[]> params() {
        return Arrays.asList(
                new Object[]{getUriFor(UI_NIGHT_MODE)},
                new Object[]{getUriFor(UI_NIGHT_MODE_CUSTOM_TYPE)},
                new Object[]{getUriFor(DARK_THEME_CUSTOM_START_TIME)},
                new Object[]{getUriFor(DARK_THEME_CUSTOM_END_TIME)});
    }

    private final Uri mUri;

    private Context mContext;
    private ContentObserver mContentObserver;
    private DarkModeObserver mDarkModeObserver;
    private Runnable mCallback;

    public DarkModeObserverTest(Uri uri) {
        mUri = uri;
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mDarkModeObserver = new DarkModeObserver(mContext);
        mContentObserver = mDarkModeObserver.getContentObserver();
        mCallback = mock(Runnable.class);
    }

    @Test
    public void callbackTest_subscribedCallbackCalled() {
        mDarkModeObserver.subscribe(mCallback);

        mContentObserver.onChange(false, mUri);

        // 1x from subscribe. 1x from onChange
        Mockito.verify(mCallback, times(2)).run();
    }

    @Test
    public void callbackTest_unsubscribedAfterSubscribe_noMoreCallbackCalled() {
        mDarkModeObserver.subscribe(mCallback);
        mDarkModeObserver.unsubscribe();

        mContentObserver.onChange(false, mUri);

        // 1x from subscribe.
        Mockito.verify(mCallback).run();
    }
}
