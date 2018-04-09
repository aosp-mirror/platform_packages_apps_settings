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

package com.android.settings.network;

import static android.arch.lifecycle.Lifecycle.Event.ON_START;
import static android.arch.lifecycle.Lifecycle.Event.ON_STOP;
import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_OFF;
import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
import static android.provider.Settings.Global.PRIVATE_DNS_MODE;
import static android.provider.Settings.Global.PRIVATE_DNS_SPECIFIER;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowContentResolver;


@RunWith(SettingsRobolectricTestRunner.class)
public class PrivateDnsPreferenceControllerTest {

    private final static String HOSTNAME = "dns.example.com";

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private Preference mPreference;
    private PrivateDnsPreferenceController mController;
    private Context mContext;
    private ContentResolver mContentResolver;
    private ShadowContentResolver mShadowContentResolver;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mContentResolver = mContext.getContentResolver();
        mShadowContentResolver = Shadow.extract(mContentResolver);

        when(mScreen.findPreference(anyString())).thenReturn(mPreference);

        mController = spy(new PrivateDnsPreferenceController(mContext));
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mLifecycle.addObserver(mController);
    }

    @Test
    public void goThroughLifecycle_shouldRegisterUnregisterSettingsObserver() {
        mLifecycle.handleLifecycleEvent(ON_START);
        verify(mContext, atLeastOnce()).getContentResolver();
        assertThat(mShadowContentResolver.getContentObservers(
                Settings.Global.getUriFor(PRIVATE_DNS_MODE))).isNotEmpty();
        assertThat(mShadowContentResolver.getContentObservers(
                Settings.Global.getUriFor(PRIVATE_DNS_SPECIFIER))).isNotEmpty();


        mLifecycle.handleLifecycleEvent(ON_STOP);
        verify(mContext, atLeastOnce()).getContentResolver();
        assertThat(mShadowContentResolver.getContentObservers(
                Settings.Global.getUriFor(PRIVATE_DNS_MODE))).isEmpty();
        assertThat(mShadowContentResolver.getContentObservers(
                Settings.Global.getUriFor(PRIVATE_DNS_SPECIFIER))).isEmpty();
    }

    @Test
    public void getSummary_PrivateDnsModeOff() {
        setPrivateDnsMode(PRIVATE_DNS_MODE_OFF);
        setPrivateDnsProviderHostname(HOSTNAME);
        mController.updateState(mPreference);
        verify(mController, atLeastOnce()).getSummary();
        verify(mPreference).setSummary(getResourceString(R.string.private_dns_mode_off));
    }

    @Test
    public void getSummary_PrivateDnsModeOpportunistic() {
        setPrivateDnsMode(PRIVATE_DNS_MODE_OPPORTUNISTIC);
        setPrivateDnsProviderHostname(HOSTNAME);
        mController.updateState(mPreference);
        verify(mController, atLeastOnce()).getSummary();
        verify(mPreference).setSummary(getResourceString(R.string.private_dns_mode_opportunistic));
    }

    @Test
    public void getSummary_PrivateDnsModeProviderHostname() {
        setPrivateDnsMode(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
        setPrivateDnsProviderHostname(HOSTNAME);
        mController.updateState(mPreference);
        verify(mController, atLeastOnce()).getSummary();
        verify(mPreference).setSummary(HOSTNAME);
    }

    private void setPrivateDnsMode(String mode) {
        Settings.Global.putString(mContentResolver, PRIVATE_DNS_MODE, mode);
    }

    private void setPrivateDnsProviderHostname(String name) {
        Settings.Global.putString(mContentResolver, PRIVATE_DNS_SPECIFIER, name);
    }

    private String getResourceString(int which) {
        return mContext.getResources().getString(which);
    }
}
