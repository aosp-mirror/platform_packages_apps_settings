/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.settings.network;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.lifecycle.Lifecycle;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class ProxySubscriptionManagerTest {

    private Context mContext;
    @Mock
    private ActiveSubscriptionsListener mActiveSubscriptionsListener;
    @Mock
    private GlobalSettingsChangeListener mAirplaneModeOnSettingsChangeListener;

    @Mock
    private Lifecycle mLifecycle_ON_PAUSE;
    @Mock
    private Lifecycle mLifecycle_ON_RESUME;
    @Mock
    private Lifecycle mLifecycle_ON_DESTROY;

    private Client mClient1;
    private Client mClient2;

    @Before
    @UiThreadTest
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());

        doReturn(Lifecycle.State.CREATED).when(mLifecycle_ON_PAUSE).getCurrentState();
        doReturn(Lifecycle.State.STARTED).when(mLifecycle_ON_RESUME).getCurrentState();
        doReturn(Lifecycle.State.DESTROYED).when(mLifecycle_ON_DESTROY).getCurrentState();

        mClient1 = new Client();
        mClient1.setLifecycle(mLifecycle_ON_RESUME);
        mClient2 = new Client();
        mClient2.setLifecycle(mLifecycle_ON_RESUME);
    }

    private ProxySubscriptionManager getInstance(Context context) {
        ProxySubscriptionManager proxy =
                Mockito.mock(ProxySubscriptionManager.class, Mockito.CALLS_REAL_METHODS);
        proxy.init(context, mActiveSubscriptionsListener, mAirplaneModeOnSettingsChangeListener);
        proxy.notifySubscriptionInfoMightChanged();
        return proxy;
    }

    public class Client implements ProxySubscriptionManager.OnActiveSubscriptionChangedListener {
        private Lifecycle lifeCycle;
        private int numberOfCallback;

        public void onChanged() {
            numberOfCallback++;
        }

        public Lifecycle getLifecycle() {
            return lifeCycle;
        }

        public int getCallbackCount() {
            return numberOfCallback;
        }

        public void setLifecycle(Lifecycle lifecycle) {
            lifeCycle = lifecycle;
        }
    }

    @Test
    @UiThreadTest
    public void addActiveSubscriptionsListener_addOneClient_getNoCallback() {
        ProxySubscriptionManager proxy = getInstance(mContext);

        proxy.addActiveSubscriptionsListener(mClient1);
        assertThat(mClient1.getCallbackCount()).isEqualTo(0);
    }

    @Test
    @UiThreadTest
    public void addActiveSubscriptionsListener_addOneClient_changeOnSimGetCallback() {
        ProxySubscriptionManager proxy = getInstance(mContext);

        proxy.addActiveSubscriptionsListener(mClient1);
        assertThat(mClient1.getCallbackCount()).isEqualTo(0);

        proxy.notifySubscriptionInfoMightChanged();
        assertThat(mClient1.getCallbackCount()).isEqualTo(1);
    }

    @Test
    @UiThreadTest
    public void addActiveSubscriptionsListener_addOneClient_noCallbackUntilUiResume() {
        ProxySubscriptionManager proxy = getInstance(mContext);

        mClient1.setLifecycle(mLifecycle_ON_PAUSE);

        proxy.addActiveSubscriptionsListener(mClient1);
        assertThat(mClient1.getCallbackCount()).isEqualTo(0);

        proxy.notifySubscriptionInfoMightChanged();
        assertThat(mClient1.getCallbackCount()).isEqualTo(0);

        mClient1.setLifecycle(mLifecycle_ON_RESUME);
        proxy.onStart();
        Assert.assertTrue(mClient1.getCallbackCount() > 0);

        mClient1.setLifecycle(mLifecycle_ON_PAUSE);
        proxy.onStop();
        int latestCallbackCount = mClient1.getCallbackCount();

        proxy.notifySubscriptionInfoMightChanged();
        assertThat(mClient1.getCallbackCount()).isEqualTo(latestCallbackCount);
    }

    @Test
    @UiThreadTest
    public void addActiveSubscriptionsListener_addTwoClient_eachClientGetNoCallback() {
        ProxySubscriptionManager proxy = getInstance(mContext);

        proxy.addActiveSubscriptionsListener(mClient1);
        assertThat(mClient1.getCallbackCount()).isEqualTo(0);

        proxy.addActiveSubscriptionsListener(mClient2);
        assertThat(mClient1.getCallbackCount()).isEqualTo(0);
        assertThat(mClient2.getCallbackCount()).isEqualTo(0);
    }

    @Test
    @UiThreadTest
    public void addActiveSubscriptionsListener_addTwoClient_callbackOnlyWhenResume() {
        ProxySubscriptionManager proxy = getInstance(mContext);

        proxy.addActiveSubscriptionsListener(mClient1);
        assertThat(mClient1.getCallbackCount()).isEqualTo(0);

        proxy.addActiveSubscriptionsListener(mClient2);
        assertThat(mClient1.getCallbackCount()).isEqualTo(0);
        assertThat(mClient2.getCallbackCount()).isEqualTo(0);

        mClient1.setLifecycle(mLifecycle_ON_PAUSE);
        proxy.onStop();
        assertThat(mClient1.getCallbackCount()).isEqualTo(0);
        assertThat(mClient2.getCallbackCount()).isEqualTo(0);

        proxy.notifySubscriptionInfoMightChanged();
        assertThat(mClient1.getCallbackCount()).isEqualTo(0);
        assertThat(mClient2.getCallbackCount()).isEqualTo(1);

        mClient1.setLifecycle(mLifecycle_ON_RESUME);
        proxy.onStart();
        Assert.assertTrue(mClient1.getCallbackCount() > 0);
        assertThat(mClient2.getCallbackCount()).isEqualTo(1);
    }

    @Test
    @UiThreadTest
    public void removeActiveSubscriptionsListener_removedClient_noCallback() {
        ProxySubscriptionManager proxy = getInstance(mContext);

        proxy.addActiveSubscriptionsListener(mClient1);
        assertThat(mClient1.getCallbackCount()).isEqualTo(0);

        proxy.notifySubscriptionInfoMightChanged();
        assertThat(mClient1.getCallbackCount()).isEqualTo(1);

        proxy.removeActiveSubscriptionsListener(mClient1);
        assertThat(mClient1.getCallbackCount()).isEqualTo(1);

        proxy.notifySubscriptionInfoMightChanged();
        assertThat(mClient1.getCallbackCount()).isEqualTo(1);
    }

    @Test
    @UiThreadTest
    public void notifySubscriptionInfoMightChanged_destroyedClient_autoRemove() {
        ProxySubscriptionManager proxy = getInstance(mContext);

        proxy.addActiveSubscriptionsListener(mClient1);
        assertThat(mClient1.getCallbackCount()).isEqualTo(0);

        proxy.notifySubscriptionInfoMightChanged();
        assertThat(mClient1.getCallbackCount()).isEqualTo(1);

        mClient1.setLifecycle(mLifecycle_ON_DESTROY);
        proxy.notifySubscriptionInfoMightChanged();
        assertThat(mClient1.getCallbackCount()).isEqualTo(1);

        mClient1.setLifecycle(mLifecycle_ON_RESUME);
        proxy.notifySubscriptionInfoMightChanged();
        assertThat(mClient1.getCallbackCount()).isEqualTo(1);
    }
}
