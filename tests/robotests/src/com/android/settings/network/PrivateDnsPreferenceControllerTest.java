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

import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_OFF;
import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
import static android.provider.Settings.Global.PRIVATE_DNS_DEFAULT_MODE;
import static android.provider.Settings.Global.PRIVATE_DNS_MODE;
import static android.provider.Settings.Global.PRIVATE_DNS_SPECIFIER;

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.DISABLED_FOR_USER;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.LinkProperties;
import android.net.Network;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowContentResolver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
    ShadowUserManager.class,
    ShadowDevicePolicyManager.class
})
public class PrivateDnsPreferenceControllerTest {

    private final static String HOSTNAME = "dns.example.com";
    private final static List<InetAddress> NON_EMPTY_ADDRESS_LIST;
    static {
        try {
            NON_EMPTY_ADDRESS_LIST = Arrays.asList(
                    InetAddress.getByAddress(new byte[] { 8, 8, 8, 8 }));
        } catch (UnknownHostException e) {
            throw new RuntimeException("Invalid hardcoded IP addresss: " + e);
        }
    }

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private Network mNetwork;
    @Mock
    private Preference mPreference;
    @Mock
    private UserManager mUserManager;
    @Captor
    private ArgumentCaptor<NetworkCallback> mCallbackCaptor;
    private PrivateDnsPreferenceController mController;
    private Context mContext;
    private ContentResolver mContentResolver;
    private ShadowContentResolver mShadowContentResolver;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;
    private ShadowUserManager mShadowUserManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mContentResolver = mContext.getContentResolver();
        mShadowContentResolver = Shadow.extract(mContentResolver);
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mConnectivityManager);
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
        doNothing().when(mConnectivityManager).registerDefaultNetworkCallback(
                mCallbackCaptor.capture(), nullable(Handler.class));

        when(mScreen.findPreference(anyString())).thenReturn(mPreference);

        mController = spy(new PrivateDnsPreferenceController(mContext));

        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mLifecycle.addObserver(mController);

        mShadowUserManager = ShadowUserManager.getShadow();
    }

    private void updateLinkProperties(LinkProperties lp) {
        NetworkCallback nc = mCallbackCaptor.getValue();
        // The network callback that has been captured by the captor is the `mNetworkCallback'
        // member of mController. mController being a spy, it has copied that member from the
        // original object it was spying on, which means the object returned by the captor
        // has a reference to the original object instead of the mock as its outer instance
        // and will call methods and modify members of the original object instead of the spy,
        // so methods subsequently called on the spy will not be aware of the changes. To work
        // around this, the following code will create a new instance of the same class with
        // the same code, but it sets the spy as the outer instance.
        // A more recent version of Mockito would have made possible to create the spy with
        // spy(PrivateDnsPreferenceController.class, withSettings().useConstructor(mContext))
        // and that would have solved the problem by removing the original object entirely
        // in a more elegant manner, but useConstructor(Object...) is only available starting
        // with Mockito 2.7.14. Other solutions involve modifying the code under test for
        // the sake of the test.
        nc = mock(nc.getClass(), withSettings().useConstructor().outerInstance(mController)
                .defaultAnswer(CALLS_REAL_METHODS));
        nc.onLinkPropertiesChanged(mNetwork, lp);
    }

    @Test
    public void getAvailibilityStatus_availableByDefault() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void getAvailabilityStatus_unsupportedWhenSet() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_disabledForGuestUser() {
        doReturn(true).when(mUserManager).isGuestUser();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_FOR_USER);
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
        mLifecycle.handleLifecycleEvent(ON_START);
        setPrivateDnsMode(PRIVATE_DNS_MODE_OPPORTUNISTIC);
        setPrivateDnsProviderHostname(HOSTNAME);
        mController.updateState(mPreference);
        verify(mController, atLeastOnce()).getSummary();
        verify(mPreference).setSummary(getResourceString(R.string.private_dns_mode_opportunistic));

        LinkProperties lp = mock(LinkProperties.class);
        when(lp.getValidatedPrivateDnsServers()).thenReturn(NON_EMPTY_ADDRESS_LIST);
        updateLinkProperties(lp);
        mController.updateState(mPreference);
        verify(mPreference).setSummary(getResourceString(R.string.private_dns_mode_on));

        reset(mPreference);
        lp = mock(LinkProperties.class);
        when(lp.getValidatedPrivateDnsServers()).thenReturn(Collections.emptyList());
        updateLinkProperties(lp);
        mController.updateState(mPreference);
        verify(mPreference).setSummary(getResourceString(R.string.private_dns_mode_opportunistic));
    }

    @Test
    public void getSummary_PrivateDnsModeProviderHostname() {
        mLifecycle.handleLifecycleEvent(ON_START);
        setPrivateDnsMode(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
        setPrivateDnsProviderHostname(HOSTNAME);
        mController.updateState(mPreference);
        verify(mController, atLeastOnce()).getSummary();
        verify(mPreference).setSummary(
                getResourceString(R.string.private_dns_mode_provider_failure));

        LinkProperties lp = mock(LinkProperties.class);
        when(lp.getValidatedPrivateDnsServers()).thenReturn(NON_EMPTY_ADDRESS_LIST);
        updateLinkProperties(lp);
        mController.updateState(mPreference);
        verify(mPreference).setSummary(HOSTNAME);

        reset(mPreference);
        lp = mock(LinkProperties.class);
        when(lp.getValidatedPrivateDnsServers()).thenReturn(Collections.emptyList());
        updateLinkProperties(lp);
        mController.updateState(mPreference);
        verify(mPreference).setSummary(
                getResourceString(R.string.private_dns_mode_provider_failure));
    }

    @Test
    public void getSummary_PrivateDnsDefaultMode() {
        // Default mode is opportunistic, unless overridden by a Settings push.
        setPrivateDnsMode("");
        setPrivateDnsProviderHostname("");
        mController.updateState(mPreference);
        verify(mController, atLeastOnce()).getSummary();
        verify(mPreference).setSummary(getResourceString(R.string.private_dns_mode_opportunistic));

        reset(mController);
        reset(mPreference);
        // Pretend an emergency gservices setting has disabled default-opportunistic.
        Settings.Global.putString(mContentResolver, PRIVATE_DNS_DEFAULT_MODE, PRIVATE_DNS_MODE_OFF);
        mController.updateState(mPreference);
        verify(mController, atLeastOnce()).getSummary();
        verify(mPreference).setSummary(getResourceString(R.string.private_dns_mode_off));

        reset(mController);
        reset(mPreference);
        // The user interacting with the Private DNS menu, explicitly choosing
        // opportunistic mode, will be able to use despite the change to the
        // default setting above.
        setPrivateDnsMode(PRIVATE_DNS_MODE_OPPORTUNISTIC);
        mController.updateState(mPreference);
        verify(mController, atLeastOnce()).getSummary();
        verify(mPreference).setSummary(getResourceString(R.string.private_dns_mode_opportunistic));
    }

    @Test
    public void isEnabled_canBeDisabledByAdmin() {
        final int userId = UserHandle.myUserId();
        final List<UserManager.EnforcingUser> enforcingUsers = Collections.singletonList(
                new UserManager.EnforcingUser(userId,
                        UserManager.RESTRICTION_SOURCE_DEVICE_OWNER)
        );
        mShadowUserManager.setUserRestrictionSources(
                UserManager.DISALLOW_CONFIG_PRIVATE_DNS,
                UserHandle.of(userId),
                enforcingUsers);

        ShadowDevicePolicyManager.getShadow().setDeviceOwnerComponentOnAnyUser(
                new ComponentName("test", "test"));

        mController.updateState(mPreference);
        verify(mPreference).setEnabled(false);
    }

    @Test
    public void isEnabled_isEnabledByDefault() {
        mController.updateState(mPreference);
        verify(mPreference).setEnabled(true);
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
