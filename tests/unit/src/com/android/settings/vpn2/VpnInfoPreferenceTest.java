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

package com.android.settings.vpn2;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.ResourcesUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class VpnInfoPreferenceTest {

    private Context mContext;
    private Resources mResources;
    private VpnInfoPreference mVpnInfoPreference;
    private AttributeSet mAttrs;
    private PreferenceViewHolder mHolder;
    private View mWarningButton;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mResources = spy(mContext.getResources());
        when(mContext.getResources()).thenReturn(mResources);

        final int helpUrlId = ResourcesUtils.getResourcesId(
                mContext, "string", "help_url_insecure_vpn");
        when(mResources.getString(helpUrlId)).thenReturn("https://www.google.com/");

        mVpnInfoPreference = new VpnInfoPreference(mContext, mAttrs);
        LayoutInflater inflater = mContext.getSystemService(LayoutInflater.class);

        // The VpnInfoPreference is a RestrictedPreference, which is a TwoTargetPreference
        final int layoutId = ResourcesUtils.getResourcesId(
                mContext, "layout", "preference_two_target");
        View view = inflater.inflate(
                layoutId,
                null /* root */, false /* attachToRoot */);
        mHolder = spy(PreferenceViewHolder.createInstanceForTests(view));
        final int warningButtonId = ResourcesUtils.getResourcesId(
                mContext, "id", "warning_button");
        mWarningButton = spy(new View(mContext));
        when(mWarningButton.getId()).thenReturn(warningButtonId);
        when(mHolder.findViewById(warningButtonId)).thenReturn(mWarningButton);
    }

    @Test
    public void onBindViewHolder_notInsecureVpn_iconInvisible() {
        mVpnInfoPreference.setInsecureVpn(false);

        mVpnInfoPreference.onBindViewHolder(mHolder);

        verify(mWarningButton).setVisibility(View.GONE);
        verify(mWarningButton).setEnabled(false);
    }

    @Test
    public void onBindViewHolder_emptyUrl_iconInvisible() {
        final int helpUrlId = ResourcesUtils.getResourcesId(
                mContext, "string", "help_url_insecure_vpn");
        when(mResources.getString(helpUrlId)).thenReturn("");
        VpnInfoPreference vpnInfoPreference = new VpnInfoPreference(mContext, mAttrs);

        vpnInfoPreference.setInsecureVpn(true);

        vpnInfoPreference.onBindViewHolder(mHolder);

        verify(mWarningButton).setVisibility(View.GONE);
        verify(mWarningButton).setEnabled(false);
    }

    @Test
    public void onBindViewHolder_insecureVpn_iconVisible() {
        mVpnInfoPreference.setInsecureVpn(true);

        mVpnInfoPreference.onBindViewHolder(mHolder);

        verify(mWarningButton).setVisibility(View.VISIBLE);
        verify(mWarningButton).setEnabled(true);
    }

    @Test
    public void onBindViewHolder_dividerInvisible() {
        mVpnInfoPreference.onBindViewHolder(mHolder);

        final int dividerId = ResourcesUtils.getResourcesId(mContext, "id", "two_target_divider");
        final View divider = mHolder.findViewById(dividerId);
        assertEquals(View.GONE, divider.getVisibility());
    }
}
