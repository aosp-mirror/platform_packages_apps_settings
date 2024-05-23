/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.vpn2;

import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.test.AndroidTestCase;
import android.text.TextUtils;

import androidx.test.filters.SmallTest;

import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnProfile;

import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PreferenceListTest extends AndroidTestCase {
    private static final String TAG = "PreferenceListTest";

    @Mock VpnSettings mSettings;

    final Map<String, LegacyVpnPreference> mLegacyMocks = new HashMap<>();
    final Map<AppVpnInfo, AppPreference> mAppMocks = new HashMap<>();

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mLegacyMocks.clear();
        mAppMocks.clear();

        doAnswer(invocation -> {
            final String key = ((VpnProfile)(invocation.getArguments()[0])).key;
            if (!mLegacyMocks.containsKey(key)) {
                mLegacyMocks.put(key, mock(LegacyVpnPreference.class));
            }
            return mLegacyMocks.get(key);
        }).when(mSettings).findOrCreatePreference(any(VpnProfile.class), anyBoolean());

        doAnswer(invocation -> {
            final AppVpnInfo key = (AppVpnInfo)(invocation.getArguments()[0]);
            if (!mAppMocks.containsKey(key)) {
                mAppMocks.put(key, mock(AppPreference.class));
            }
            return mAppMocks.get(key);
        }).when(mSettings).findOrCreatePreference(any(AppVpnInfo.class));

        doNothing().when(mSettings).setShownPreferences(any());
        doReturn(true).when(mSettings).canAddPreferences();
    }

    @SmallTest
    public void testNothingShownByDefault() {
        final VpnSettings.UpdatePreferences updater = new VpnSettings.UpdatePreferences(mSettings);
        updater.run();

        verify(mSettings, never()).findOrCreatePreference(any(VpnProfile.class), anyBoolean());
        assertEquals(0, mLegacyMocks.size());
        assertEquals(0, mAppMocks.size());
    }

    @SmallTest
    public void testDisconnectedLegacyVpnShown() {
        final VpnProfile vpnProfile = new VpnProfile("test-disconnected");

        final VpnSettings.UpdatePreferences updater = new VpnSettings.UpdatePreferences(mSettings);
        updater.legacyVpns(
                /* vpnProfiles */ Collections.<VpnProfile>singletonList(vpnProfile),
                /* connectedLegacyVpns */ Collections.<String, LegacyVpnInfo>emptyMap(),
                /* lockdownVpnKey */ null);
        updater.run();

        verify(mSettings, times(1)).findOrCreatePreference(any(VpnProfile.class), eq(true));
        assertEquals(1, mLegacyMocks.size());
        assertEquals(0, mAppMocks.size());
    }

    @SmallTest
    public void testConnectedLegacyVpnShownIfDeleted() {
        final LegacyVpnInfo connectedLegacyVpn =new LegacyVpnInfo();
        connectedLegacyVpn.key = "test-connected";

        final VpnSettings.UpdatePreferences updater = new VpnSettings.UpdatePreferences(mSettings);
        updater.legacyVpns(
                /* vpnProfiles */ Collections.<VpnProfile>emptyList(),
                /* connectedLegacyVpns */ new HashMap<String, LegacyVpnInfo>() {{
                    put(connectedLegacyVpn.key, connectedLegacyVpn);
                }},
                /* lockdownVpnKey */ null);
        updater.run();

        verify(mSettings, times(1)).findOrCreatePreference(any(VpnProfile.class), eq(false));
        assertEquals(1, mLegacyMocks.size());
        assertEquals(0, mAppMocks.size());
    }

    @SmallTest
    public void testConnectedLegacyVpnShownExactlyOnce() {
        final VpnProfile vpnProfile = new VpnProfile("test-no-duplicates");
        final LegacyVpnInfo connectedLegacyVpn = new LegacyVpnInfo();
        connectedLegacyVpn.key = new String(vpnProfile.key);

        final VpnSettings.UpdatePreferences updater = new VpnSettings.UpdatePreferences(mSettings);
        updater.legacyVpns(
                /* vpnProfiles */ Collections.<VpnProfile>singletonList(vpnProfile),
                /* connectedLegacyVpns */ new HashMap<String, LegacyVpnInfo>() {{
                    put(connectedLegacyVpn.key, connectedLegacyVpn);
                }},
                /* lockdownVpnKey */ null);
        updater.run();

        final ArgumentMatcher<VpnProfile> equalsFake = arg -> {
            if (arg == vpnProfile) return true;
            if (arg == null) return false;
            return TextUtils.equals(arg.key, vpnProfile.key);
        };

        // The VPN profile should have been used to create a preference and set up at laest once
        // with update=true to fill in all the fields.
        verify(mSettings, atLeast(1)).findOrCreatePreference(argThat(equalsFake), eq(true));

        // ...But no other VPN profile key should ever have been passed in.
        verify(mSettings, never()).findOrCreatePreference(not(argThat(equalsFake)), anyBoolean());

        // And so we should still have exactly 1 preference created.
        assertEquals(1, mLegacyMocks.size());
        assertEquals(0, mAppMocks.size());
    }
}
