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

package com.android.settings.development.gamedriver;

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
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class GameDriverContentObserverTest {

    @Mock
    private ContentResolver mResolver;
    @Mock
    private GameDriverContentObserver.OnGameDriverContentChangedListener mListener;

    private GameDriverContentObserver mGameDriverContentObserver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mGameDriverContentObserver = spy(new GameDriverContentObserver(null, null));
    }

    @Test
    public void onChange_shouldCallListener() {
        mGameDriverContentObserver.mListener = mListener;
        mGameDriverContentObserver.onChange(true);

        verify(mListener).onGameDriverContentChanged();
    }

    @Test
    public void register_shouldRegisterContentObserver() {
        mGameDriverContentObserver.register(mResolver);

        verify(mResolver).registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.GAME_DRIVER_ALL_APPS), false,
                mGameDriverContentObserver);
    }

    @Test
    public void unregister_shouldUnregisterContentObserver() {
        mGameDriverContentObserver.unregister(mResolver);

        verify(mResolver).unregisterContentObserver(mGameDriverContentObserver);
    }
}
