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

package com.android.settings.security.trustagent;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.os.UserManager;
import android.service.trust.TrustAgentService;

import com.android.internal.widget.LockPatternUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class TrustAgentManagerTest {

    private static final String CANNED_PACKAGE_NAME = "com.test.package";
    private static final String CANNED_CLASS_NAME = "TestTrustAgent";
    private static final String CANNED_TRUST_AGENT_TITLE = "TestTrustAgentTitle";

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private Context mContext;
    @Mock
    private DevicePolicyManager mDevicePolicyManager;
    @Mock
    private LockPatternUtils mLockPatternUtils;
    @Mock
    private UserManager mUserManager;
    @Mock
    private UserInfo mUserInfo;
    @Mock
    private XmlResourceParser mXmlResourceParser;
    @Mock
    private Resources mResources;
    @Mock
    private TypedArray mTypedArray;

    private TrustAgentManager mTrustAgentManager;

    @Before
    public void setUp() throws NameNotFoundException {
        MockitoAnnotations.initMocks(this);
        mTrustAgentManager = new TrustAgentManager();
        when(mContext.getSystemService(DevicePolicyManager.class))
                .thenReturn(mDevicePolicyManager);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getSystemService(Context.USER_SERVICE))
                .thenReturn(mUserManager);
        when(mResources.obtainAttributes(any(), any())).thenReturn(mTypedArray);
        when(mPackageManager.getResourcesForApplication(any(ApplicationInfo.class)))
                .thenReturn(mResources);
        when(mPackageManager.getXml(any(), anyInt(), any())).thenReturn(mXmlResourceParser);
        when(mUserManager.getUserInfo(anyInt())).thenReturn(mUserInfo);
    }

    @Test
    public void shouldProvideTrust_doesProvideTrustWithPermission() {
        when(mPackageManager.checkPermission(TrustAgentManager.PERMISSION_PROVIDE_AGENT,
                CANNED_PACKAGE_NAME)).thenReturn(PackageManager.PERMISSION_GRANTED);

        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.packageName = CANNED_PACKAGE_NAME;
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = serviceInfo;

        assertThat(mTrustAgentManager.shouldProvideTrust(resolveInfo, mPackageManager)).isTrue();
    }

    @Test
    public void shouldProvideTrust_doesNotProvideTrustWithoutPermission() {
        when(mPackageManager.checkPermission(TrustAgentManager.PERMISSION_PROVIDE_AGENT,
                CANNED_PACKAGE_NAME)).thenReturn(PackageManager.PERMISSION_DENIED);

        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.packageName = CANNED_PACKAGE_NAME;
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = serviceInfo;

        assertThat(mTrustAgentManager.shouldProvideTrust(resolveInfo, mPackageManager)).isFalse();
    }

    @Test
    public void getAllActiveTrustAgentsAndComponentSet_returnsTrustAgents()
            throws XmlPullParserException, IOException {
        setUpGetActiveTrustAgents(true);

        assertThat(mTrustAgentManager.getActiveTrustAgents(mContext, mLockPatternUtils, false))
            .isNotEmpty();
    }

    @Test
    public void getActiveTrustAgentsAndComponentSet_componentSet_returnsTrustAgents()
            throws XmlPullParserException, IOException {
        setUpGetActiveTrustAgents(true);

        assertThat(mTrustAgentManager.getActiveTrustAgents(mContext, mLockPatternUtils, true))
            .isNotEmpty();
    }

    @Test
    public void getAllActiveTrustAgentsAndComponentNotSet_returnsTrustAgents()
            throws XmlPullParserException, IOException {
        setUpGetActiveTrustAgents(false);

        assertThat(mTrustAgentManager.getActiveTrustAgents(mContext, mLockPatternUtils, false))
            .isNotEmpty();
    }

    @Test
    public void getActiveTrustAgentsAndComponentSet_returnsEmpty()
            throws XmlPullParserException, IOException {
        setUpGetActiveTrustAgents(false);

        assertThat(mTrustAgentManager.getActiveTrustAgents(mContext, mLockPatternUtils, true))
            .isEmpty();
    }

    private void setUpGetActiveTrustAgents(boolean hasSettingsActivity)
            throws XmlPullParserException, IOException {
        String settingsActivity =
                hasSettingsActivity ? CANNED_PACKAGE_NAME + "." + CANNED_CLASS_NAME : "";
        when(mXmlResourceParser.next()).thenReturn(XmlPullParser.START_TAG);
        when(mXmlResourceParser.getName()).thenReturn("trust-agent");
        List<ResolveInfo> resolveInfos =
                Collections.singletonList(createResolveInfo(hasSettingsActivity));
        List<ComponentName> enabledTrustAgents =
                Collections.singletonList(
                      new ComponentName(CANNED_PACKAGE_NAME, CANNED_CLASS_NAME));

        when(mPackageManager.queryIntentServices(any(), anyInt())).thenReturn(resolveInfos);
        when(mLockPatternUtils.getEnabledTrustAgents(anyInt())).thenReturn(enabledTrustAgents);
        when(mUserInfo.isManagedProfile()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(null);
        when(mPackageManager.checkPermission(TrustAgentManager.PERMISSION_PROVIDE_AGENT,
                CANNED_PACKAGE_NAME)).thenReturn(PackageManager.PERMISSION_GRANTED);
        when(mTypedArray.getString(com.android.internal.R.styleable.TrustAgent_title))
            .thenReturn(CANNED_TRUST_AGENT_TITLE);
        when(mTypedArray.getString(com.android.internal.R.styleable.TrustAgent_settingsActivity))
            .thenReturn(settingsActivity);
    }

    private ResolveInfo createResolveInfo(boolean hasSettingsActivity) {
        ServiceInfo serviceInfo = new ServiceInfo();
        Bundle metaData = new Bundle();
        metaData.putInt(TrustAgentService.TRUST_AGENT_META_DATA, 1);
        serviceInfo.packageName = CANNED_PACKAGE_NAME;
        serviceInfo.name = CANNED_CLASS_NAME;
        serviceInfo.metaData = metaData;
        serviceInfo.applicationInfo = new ApplicationInfo();
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = serviceInfo;
        return resolveInfo;
    }
}
