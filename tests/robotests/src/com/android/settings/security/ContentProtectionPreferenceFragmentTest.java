/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.security;

import static android.app.settings.SettingsEnums.CONTENT_PROTECTION_PREFERENCE;

import static com.android.settings.security.ContentProtectionPreferenceFragment.KEY_WORK_PROFILE_SWITCH;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.testutils.shadow.ShadowDashboardFragment;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowDashboardFragment.class,
            ShadowUtils.class,
        })
public class ContentProtectionPreferenceFragmentTest {
    private static final int TEST_PRIMARY_USER_ID = 10;
    private static final int TEST_MANAGED_PROFILE_ID = 11;

    private ContentProtectionPreferenceFragment mFragment;
    @Mock private UserManager mMockUserManager;
    private Context mContext;
    private PreferenceScreen mScreen;
    private SwitchPreference mWorkProfileSwitch;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        mFragment = spy(new ContentProtectionPreferenceFragment());
        mScreen = spy(new PreferenceScreen(mContext, /* attrs= */ null));

        doReturn(mContext).when(mFragment).getContext();
        doReturn(mScreen).when(mFragment).getPreferenceScreen();

        mWorkProfileSwitch = new SwitchPreference(mContext);
        mWorkProfileSwitch.setVisible(false);
        doReturn(mWorkProfileSwitch).when(mScreen).findPreference(KEY_WORK_PROFILE_SWITCH);

        doReturn(mMockUserManager).when(mContext).getSystemService(UserManager.class);
        doReturn(TEST_PRIMARY_USER_ID).when(mMockUserManager).getUserHandle();
        UserInfo primaryUser =
                new UserInfo(
                        TEST_PRIMARY_USER_ID,
                        null,
                        UserInfo.FLAG_INITIALIZED | UserInfo.FLAG_PRIMARY);
        doReturn(primaryUser).when(mMockUserManager).getUserInfo(TEST_PRIMARY_USER_ID);
        UserInfo managedProfile =
                new UserInfo(
                        TEST_MANAGED_PROFILE_ID,
                        null,
                        UserInfo.FLAG_INITIALIZED | UserInfo.FLAG_MANAGED_PROFILE);
        doReturn(managedProfile).when(mMockUserManager).getUserInfo(TEST_MANAGED_PROFILE_ID);
    }

    @Test
    public void onActivityCreated_workProfileDisplayWorkSwitch() {
        UserHandle[] userHandles =
                new UserHandle[] {
                    new UserHandle(TEST_PRIMARY_USER_ID), new UserHandle(TEST_MANAGED_PROFILE_ID)
                };
        doReturn(Arrays.asList(userHandles)).when(mMockUserManager).getUserProfiles();

        assertThat(Utils.getManagedProfile(mMockUserManager).getIdentifier())
                .isEqualTo(TEST_MANAGED_PROFILE_ID);

        mFragment.onActivityCreated(null);

        assertThat(mWorkProfileSwitch.isVisible()).isTrue();
        assertThat(mWorkProfileSwitch.isChecked()).isFalse();
        assertThat(mWorkProfileSwitch.isEnabled()).isFalse();
    }

    @Test
    public void onActivityCreated_fullyManagedMode_bottomSwitchInvisible() {
        final ComponentName componentName =
                ComponentName.unflattenFromString("com.android.test/.DeviceAdminReceiver");
        ShadowUtils.setDeviceOwnerComponent(componentName);

        mFragment.onActivityCreated(null);

        assertThat(mWorkProfileSwitch.isVisible()).isFalse();
    }

    @Test
    public void onActivityCreated_personalProfileHideWorkSwitch() {
        UserHandle[] userHandles = new UserHandle[] {new UserHandle(TEST_PRIMARY_USER_ID)};
        doReturn(Arrays.asList(userHandles)).when(mMockUserManager).getUserProfiles();

        assertThat(Utils.getManagedProfile(mMockUserManager)).isNull();

        mFragment.onActivityCreated(null);

        assertThat(mWorkProfileSwitch.isVisible()).isFalse();
    }

    @Test
    public void getMetricsCategory() {
        assertThat(mFragment.getMetricsCategory()).isEqualTo(CONTENT_PROTECTION_PREFERENCE);
    }

    @Test
    public void getPreferenceScreenResId() {
        assertThat(mFragment.getPreferenceScreenResId())
                .isEqualTo(R.layout.content_protection_preference_fragment);
    }

    @Test
    public void getNonIndexableKeys_existInXmlLayout() {
        final List<String> nonIndexableKeys =
                ContentProtectionPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(
                        mContext);
        final List<String> allKeys =
                XmlTestUtils.getKeysFromPreferenceXml(
                        mContext, R.layout.content_protection_preference_fragment);

        assertThat(allKeys).containsAtLeastElementsIn(nonIndexableKeys);
    }

    @Test
    public void searchIndexProvider_shouldIndexResource() {
        final List<SearchIndexableResource> indexRes =
                ContentProtectionPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER
                        .getXmlResourcesToIndex(mContext, /* enabled= */ true);

        assertThat(indexRes).isNotNull();
        assertThat(indexRes).isNotEmpty();
        assertThat(indexRes.get(0).xmlResId).isEqualTo(mFragment.getPreferenceScreenResId());
    }
}

