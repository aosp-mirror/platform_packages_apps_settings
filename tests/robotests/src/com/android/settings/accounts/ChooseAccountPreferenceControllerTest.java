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

package com.android.settings.accounts;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import android.accounts.AuthenticatorDescription;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.SyncAdapterType;
import android.graphics.drawable.ColorDrawable;
import android.os.UserHandle;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.shadow.ShadowAccountManager;
import com.android.settings.testutils.shadow.ShadowContentResolver;
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAccountManager.class, ShadowContentResolver.class,
        ShadowRestrictedLockUtilsInternal.class})
public class ChooseAccountPreferenceControllerTest {

    private Context mContext;
    private ChooseAccountPreferenceController mController;
    private Activity mActivity;
    private PreferenceManager mPreferenceManager;
    private PreferenceScreen mPreferenceScreen;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = spy(new ChooseAccountPreferenceController(mContext, "controller_key"));
        mActivity = Robolectric.setupActivity(FragmentActivity.class);
        mPreferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = mPreferenceManager.createPreferenceScreen(mContext);
    }

    @After
    public void tearDown() {
        ShadowContentResolver.reset();
        ShadowAccountManager.reset();
        ShadowRestrictedLockUtilsInternal.clearDisabledTypes();
    }

    @Test
    public void getAvailabilityStatus_byDefault_shouldBeShown() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void handlePreferenceTreeClick_notProviderPreference_shouldReturnFalse() {
        final Preference preference = new Preference(mContext);
        assertThat(mController.handlePreferenceTreeClick(preference)).isFalse();
    }

    @Test
    public void handlePreferenceTreeClick_isProviderPreference_shouldFinishFragment() {
        final ProviderPreference providerPreference = new ProviderPreference(mContext,
                "account_type", null /* icon */, "provider_name");

        mController.initialize(null, null, null, mActivity);
        mController.handlePreferenceTreeClick(providerPreference);

        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void updateAuthDescriptions_oneProvider_shouldNotAddPreference() {
        final AuthenticatorDescription authDesc = new AuthenticatorDescription("com.acct1",
                "com.android.settings",
                R.string.header_add_an_account, 0, 0, 0, false);
        ShadowAccountManager.addAuthenticator(authDesc);

        final SyncAdapterType[] syncAdapters = {new SyncAdapterType("authority" /* authority */,
                "com.acct1" /* accountType */, false /* userVisible */,
                true /* supportsUploading */)};
        ShadowContentResolver.setSyncAdapterTypes(syncAdapters);

        ShadowRestrictedLockUtilsInternal.setHasSystemFeature(true);
        ShadowRestrictedLockUtilsInternal.setDevicePolicyManager(mock(DevicePolicyManager.class));
        ShadowRestrictedLockUtilsInternal.setDisabledTypes(new String[] {"test_type"});

        doReturn("label").when(mController).getLabelForType(anyString());

        mController.initialize(null, null, new UserHandle(3), mActivity);
        mController.displayPreference(mPreferenceScreen);

        assertThat(mActivity.isFinishing()).isTrue();
        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void updateAuthDescriptions_oneAdminDisabledProvider_shouldNotAddPreference() {
        final AuthenticatorDescription authDesc = new AuthenticatorDescription("com.acct1",
                "com.android.settings",
                R.string.header_add_an_account, 0, 0, 0, false);
        ShadowAccountManager.addAuthenticator(authDesc);

        final SyncAdapterType[] syncAdapters = {new SyncAdapterType("authority" /* authority */,
                "com.acct1" /* accountType */, false /* userVisible */,
                true /* supportsUploading */)};
        ShadowContentResolver.setSyncAdapterTypes(syncAdapters);

        ShadowRestrictedLockUtilsInternal.setHasSystemFeature(true);
        ShadowRestrictedLockUtilsInternal.setDevicePolicyManager(mock(DevicePolicyManager.class));
        ShadowRestrictedLockUtilsInternal.setDisabledTypes(new String[] {"com.acct1"});

        doReturn("label").when(mController).getLabelForType(anyString());

        mController.initialize(null, null, new UserHandle(3), mActivity);
        mController.displayPreference(mPreferenceScreen);

        assertThat(mActivity.isFinishing()).isTrue();
        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void updateAuthDescriptions_noProvider_shouldNotAddPreference() {
        final AuthenticatorDescription authDesc = new AuthenticatorDescription("com.acct1",
                "com.android.settings",
                R.string.header_add_an_account, 0, 0, 0, false);
        ShadowAccountManager.addAuthenticator(authDesc);

        final SyncAdapterType[] syncAdapters = {new SyncAdapterType("authority" /* authority */,
                "com.acct1" /* accountType */, false /* userVisible */,
                true /* supportsUploading */)};
        ShadowContentResolver.setSyncAdapterTypes(syncAdapters);

        doReturn("label").when(mController).getLabelForType(anyString());

        mController.initialize(new String[] {"test_authoritiy1"}, null, new UserHandle(3),
                mActivity);
        mController.displayPreference(mPreferenceScreen);

        assertThat(mActivity.isFinishing()).isTrue();
        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void
    updateAuthDescriptions_twoProvider_shouldAddTwoPreference() {
        final AuthenticatorDescription authDesc = new AuthenticatorDescription("com.acct1",
                "com.android.settings",
                R.string.header_add_an_account, 0, 0, 0, false);
        final AuthenticatorDescription authDesc2 = new AuthenticatorDescription("com.acct2",
                "com.android.settings",
                R.string.header_add_an_account, 0, 0, 0, false);
        ShadowAccountManager.addAuthenticator(authDesc);
        ShadowAccountManager.addAuthenticator(authDesc2);

        final SyncAdapterType[] syncAdapters = {new SyncAdapterType("authority" /* authority */,
                "com.acct1" /* accountType */, false /* userVisible */,
                true /* supportsUploading */), new SyncAdapterType("authority2" /* authority */,
                "com.acct2" /* accountType */, false /* userVisible */,
                true /* supportsUploading */)};
        ShadowContentResolver.setSyncAdapterTypes(syncAdapters);

        doReturn("label").when(mController).getLabelForType(anyString());
        doReturn("label2").when(mController).getLabelForType(anyString());
        doReturn(new ColorDrawable()).when(mController).getDrawableForType(anyString());
        doReturn(new ColorDrawable()).when(mController).getDrawableForType(anyString());

        mController.initialize(null, null, new UserHandle(3), mActivity);
        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(2);
    }
}
