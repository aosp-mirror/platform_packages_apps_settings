/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.settings.development.graphicsdriver;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.ContentResolver;
import android.provider.Settings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class GraphicsDriverContentObserverTest {

    @Mock
    private ContentResolver mResolver;
    @Mock
    private GraphicsDriverContentObserver.OnGraphicsDriverContentChangedListener mListener;

    private GraphicsDriverContentObserver mGraphicsDriverContentObserver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mGraphicsDriverContentObserver = spy(new GraphicsDriverContentObserver(null, null));
    }

    @Test
    public void onChange_shouldCallListener() {
        mGraphicsDriverContentObserver.mListener = mListener;
        mGraphicsDriverContentObserver.onChange(true);

        verify(mListener).onGraphicsDriverContentChanged();
    }

    @Test
    public void register_shouldRegisterContentObserver() {
        mGraphicsDriverContentObserver.register(mResolver);

        verify(mResolver).registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.GAME_DRIVER_ALL_APPS), false,
                mGraphicsDriverContentObserver);
    }

    @Test
    public void unregister_shouldUnregisterContentObserver() {
        mGraphicsDriverContentObserver.unregister(mResolver);

        verify(mResolver).unregisterContentObserver(mGraphicsDriverContentObserver);
    }
}
