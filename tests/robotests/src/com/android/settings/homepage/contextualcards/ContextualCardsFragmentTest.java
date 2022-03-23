/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.homepage.contextualcards;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.loader.app.LoaderManager;

import com.android.settings.homepage.contextualcards.ContextualCardsFragment.ScreenOffReceiver;
import com.android.settings.slices.SlicesFeatureProvider;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowFragment.class, ContextualCardsFragmentTest.ShadowLoaderManager.class,
        ContextualCardsFragmentTest.ShadowContextualCardManager.class})
public class ContextualCardsFragmentTest {

    @Mock
    private FragmentActivity mActivity;
    private Context mContext;
    private ContextualCardsFragment mFragment;
    private SlicesFeatureProvider mSlicesFeatureProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mSlicesFeatureProvider = FakeFeatureFactory.setupForTest().slicesFeatureProvider;

        mFragment = spy(new ContextualCardsFragment());
        doReturn(mActivity).when(mFragment).getActivity();
        mFragment.onCreate(null);
    }

    @Test
    public void onStart_shouldRegisterBothReceivers() {
        mFragment.onStart();

        verify(mActivity).registerReceiver(eq(mFragment.mKeyEventReceiver),
                any(IntentFilter.class));
        verify(mActivity).registerReceiver(eq(mFragment.mScreenOffReceiver),
                any(IntentFilter.class));
    }

    @Test
    public void onStop_shouldUnregisterKeyEventReceiver() {
        mFragment.onStart();
        mFragment.onStop();

        verify(mActivity).unregisterReceiver(eq(mFragment.mKeyEventReceiver));
    }

    @Test
    public void onDestroy_shouldUnregisterScreenOffReceiver() {
        mFragment.onStart();
        mFragment.onDestroy();

        verify(mActivity).unregisterReceiver(any(ScreenOffReceiver.class));
    }

    @Test
    public void onStart_needRestartLoader_shouldClearRestartLoaderNeeded() {
        mFragment.sRestartLoaderNeeded = true;

        mFragment.onStart();

        assertThat(mFragment.sRestartLoaderNeeded).isFalse();
    }

    @Test
    public void onReceive_homeKey_shouldResetSession() {
        final Intent intent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        intent.putExtra("reason", "homekey");
        mFragment.onStart();

        mFragment.mKeyEventReceiver.onReceive(mContext, intent);

        assertThat(mFragment.sRestartLoaderNeeded).isTrue();
        verify(mSlicesFeatureProvider, times(2)).newUiSession();
        verify(mActivity).unregisterReceiver(any(ScreenOffReceiver.class));
    }

    @Test
    public void onReceive_recentApps_shouldResetSession() {
        final Intent intent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        intent.putExtra("reason", "recentapps");
        mFragment.onStart();

        mFragment.mKeyEventReceiver.onReceive(mContext, intent);

        assertThat(mFragment.sRestartLoaderNeeded).isTrue();
        verify(mSlicesFeatureProvider, times(2)).newUiSession();
        verify(mActivity).unregisterReceiver(any(ScreenOffReceiver.class));
    }

    @Test
    public void onReceive_otherKey_shouldNotResetSession() {
        final Intent intent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        intent.putExtra("reason", "other");
        mFragment.onStart();

        mFragment.mKeyEventReceiver.onReceive(mContext, intent);

        assertThat(mFragment.sRestartLoaderNeeded).isFalse();
        verify(mSlicesFeatureProvider).newUiSession();
        verify(mActivity, never()).unregisterReceiver(any(ScreenOffReceiver.class));
    }

    @Test
    public void onReceive_screenOff_shouldResetSession() {
        final Intent intent = new Intent(Intent.ACTION_SCREEN_OFF);
        mFragment.onStart();

        mFragment.mScreenOffReceiver.onReceive(mContext, intent);

        assertThat(mFragment.sRestartLoaderNeeded).isTrue();
        verify(mSlicesFeatureProvider, times(2)).newUiSession();
        verify(mActivity).unregisterReceiver(any(ScreenOffReceiver.class));
    }

    @Implements(value = LoaderManager.class)
    static class ShadowLoaderManager {

        @Mock
        private static LoaderManager sLoaderManager;

        @Implementation
        public static <T extends LifecycleOwner & ViewModelStoreOwner> LoaderManager getInstance(
                T owner) {
            return sLoaderManager;
        }
    }

    @Implements(value = ContextualCardManager.class)
    public static class ShadowContextualCardManager {

        public ShadowContextualCardManager() {
        }

        @Implementation
        protected void setupController(int cardType) {
            // do nothing
        }

        @Implementation
        protected void loadContextualCards(LoaderManager loaderManager,
                boolean restartLoaderNeeded) {
            // do nothing
        }
    }
}
