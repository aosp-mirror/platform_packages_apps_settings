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

package com.android.settings;

import android.app.Activity;
import android.content.Context;
import android.content.IContentProvider;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.notification.LockScreenNotificationPreferenceController;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settingslib.drawer.DashboardCategory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(
    manifest = TestConfig.MANIFEST_PATH,
    sdk = TestConfig.SDK_VERSION,
    shadows = {ShadowLockPatternUtils.class}
)
public class SecuritySettingsTest {

    private static final String MOCK_SUMMARY = "summary";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private DashboardCategory mDashboardCategory;
    @Mock
    private SummaryLoader mSummaryLoader;

    private SecuritySettings.SummaryProvider mSummaryProvider;

    @Implements(com.android.settingslib.drawer.TileUtils.class)
    public static class ShadowTileUtils {
        @Implementation
        public static String getTextFromUri(Context context, String uriString,
                Map<String, IContentProvider> providerMap, String key) {
            return MOCK_SUMMARY;
        }
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
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
    public void testSetLockscreenPreferencesSummary_shouldSetSummaryFromLockScreenNotification() {
        final Preference preference = mock(Preference.class);
        final PreferenceGroup group = mock(PreferenceGroup.class);
        when(group.findPreference(SecuritySettings.KEY_LOCKSCREEN_PREFERENCES))
            .thenReturn(preference);
        final LockScreenNotificationPreferenceController controller =
            mock(LockScreenNotificationPreferenceController.class);

        final SecuritySettings securitySettings = new SecuritySettings();
        ReflectionHelpers.setField(securitySettings,
            "mLockScreenNotificationPreferenceController", controller);

        when(controller.getSummaryResource()).thenReturn(1234);
        securitySettings.setLockscreenPreferencesSummary(group);
        verify(preference).setSummary(1234);
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
}
