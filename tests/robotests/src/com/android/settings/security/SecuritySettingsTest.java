/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManager.EnforcingUser;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = {
                ShadowLockPatternUtils.class,
                ShadowUserManager.class,
        })
public class SecuritySettingsTest {


    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private SummaryLoader mSummaryLoader;

    private SecuritySettings.SummaryProvider mSummaryProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest();
        mSummaryProvider = new SecuritySettings.SummaryProvider(mContext, mSummaryLoader);
    }

    @Test
    public void testSummaryProvider_notListening() {
        mSummaryProvider.setListening(false);

        verifyNoMoreInteractions(mSummaryLoader);
    }

    @Test
    public void testSummaryProvider_hasFingerPrint_hasStaticSummary() {
        final FingerprintManager fpm = mock(FingerprintManager.class);
        when(mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT))
                .thenReturn(true);

        // Cast to Object to workaround a robolectric casting bug
        when((Object) mContext.getSystemService(FingerprintManager.class)).thenReturn(fpm);
        when(fpm.isHardwareDetected()).thenReturn(true);

        mSummaryProvider.setListening(true);

        verify(mContext).getString(R.string.security_dashboard_summary);
    }

    @Test
    public void testSummaryProvider_noFpFeature_shouldSetSummaryWithNoFingerprint() {
        final FingerprintManager fpm = mock(FingerprintManager.class);
        when(mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT))
                .thenReturn(false);

        mSummaryProvider.setListening(true);

        verify(mContext).getString(R.string.security_dashboard_summary_no_fingerprint);
    }

    @Test
    public void testSummaryProvider_noFpHardware_shouldSetSummaryWithNoFingerprint() {
        final FingerprintManager fpm = mock(FingerprintManager.class);
        when(mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT))
                .thenReturn(true);

        // Cast to Object to workaround a robolectric casting bug
        when((Object) mContext.getSystemService(FingerprintManager.class)).thenReturn(fpm);
        when(fpm.isHardwareDetected()).thenReturn(false);

        mSummaryProvider.setListening(true);

        verify(mContext).getString(R.string.security_dashboard_summary_no_fingerprint);
    }

    @Test
    public void testInitTrustAgentPreference_secure_shouldSetSummaryToNumberOfTrustAgent() {
        final Preference preference = mock(Preference.class);
        final PreferenceScreen screen = mock(PreferenceScreen.class);
        when(screen.findPreference(SecuritySettings.KEY_MANAGE_TRUST_AGENTS))
                .thenReturn(preference);
        final LockPatternUtils utils = mock(LockPatternUtils.class);
        when(utils.isSecure(anyInt())).thenReturn(true);
        final Context context = ShadowApplication.getInstance().getApplicationContext();
        final Activity activity = mock(Activity.class);
        when(activity.getResources()).thenReturn(context.getResources());
        final SecuritySettings securitySettings = spy(new SecuritySettings());
        when(securitySettings.getActivity()).thenReturn(activity);

        ReflectionHelpers.setField(securitySettings, "mLockPatternUtils", utils);

        securitySettings.initTrustAgentPreference(screen, 0);
        verify(preference).setSummary(R.string.manage_trust_agents_summary);

        securitySettings.initTrustAgentPreference(screen, 2);
        verify(preference).setSummary(context.getResources().getQuantityString(
                R.plurals.manage_trust_agents_summary_on, 2, 2));
    }

    @Test
    public void testNonIndexableKeys_existInXmlLayout() {
        final Context context = spy(RuntimeEnvironment.application);
        UserManager manager = mock(UserManager.class);
        when(manager.isAdminUser()).thenReturn(false);
        doReturn(manager).when(context).getSystemService(Context.USER_SERVICE);
        final List<String> niks = SecuritySettings.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(context);

        final List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(context,
                R.xml.security_settings_misc);
        keys.addAll(XmlTestUtils.getKeysFromPreferenceXml(context,
                R.xml.location_settings));
        keys.addAll(XmlTestUtils.getKeysFromPreferenceXml(context,
                R.xml.encryption_and_credential));

        assertThat(keys).containsAllIn(niks);
    }

    @Test
    public void testUnifyLockRestriction() {
        // Set up instance under test.
        final Context context = spy(RuntimeEnvironment.application);
        final SecuritySettings securitySettings = spy(new SecuritySettings());
        when(securitySettings.getContext()).thenReturn(context);

        final int userId = 123;
        ReflectionHelpers.setField(securitySettings, "mProfileChallengeUserId", userId);

        final LockPatternUtils utils = mock(LockPatternUtils.class);
        when(utils.isSeparateProfileChallengeEnabled(userId)).thenReturn(true);
        ReflectionHelpers.setField(securitySettings, "mLockPatternUtils", utils);

        final RestrictedSwitchPreference unifyProfile = mock(RestrictedSwitchPreference.class);
        ReflectionHelpers.setField(securitySettings, "mUnifyProfile", unifyProfile);

        // Pretend that no admins enforce the restriction.
        ShadowUserManager.getShadow().setUserRestrictionSources(
                UserManager.DISALLOW_UNIFIED_PASSWORD,
                UserHandle.of(userId),
                Collections.emptyList());

        securitySettings.updateUnificationPreference();

        verify(unifyProfile).setDisabledByAdmin(null);

        reset(unifyProfile);

        // Pretend that the restriction is enforced by several admins. Having just one would
        // require more mocking of implementation details.
        final EnforcingUser enforcer1 = new EnforcingUser(
                userId, UserManager.RESTRICTION_SOURCE_PROFILE_OWNER);
        final EnforcingUser enforcer2 = new EnforcingUser(
                UserHandle.USER_SYSTEM, UserManager.RESTRICTION_SOURCE_DEVICE_OWNER);
        ShadowUserManager.getShadow().setUserRestrictionSources(
                UserManager.DISALLOW_UNIFIED_PASSWORD,
                UserHandle.of(userId),
                Arrays.asList(enforcer1, enforcer2));

        securitySettings.updateUnificationPreference();

        verify(unifyProfile).setDisabledByAdmin(EnforcedAdmin.MULTIPLE_ENFORCED_ADMIN);
    }
}
