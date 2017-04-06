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

package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.NetworkScorerAppData;
import android.os.RemoteException;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.network.NetworkScoreManagerWrapper;
import com.android.settings.utils.NotificationChannelHelper;
import com.android.settings.utils.NotificationChannelHelper.NotificationChannelWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class NotifyOpenNetworkPreferenceControllerTest {

    private static final String TEST_SCORER_PACKAGE = "Test Package";
    private static final String TEST_SCORER_CLASS = "Test Class";
    private static final String TEST_SCORER_LABEL = "Test Label";
    private static final String NOTIFICATION_ID = "Notification Id";
    private static final CharSequence NOTIFICATION_NAME = "Notification Name";

    private Context mContext;
    private NotifyOpenNetworksPreferenceController mController;
    @Mock private NetworkScoreManagerWrapper mNetworkScorer;
    @Mock private NotificationChannelHelper mNotificationChannelHelper;
    @Mock private PackageManager mPackageManager;
    @Mock private NotificationChannelWrapper mChannel;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new NotifyOpenNetworksPreferenceController(
                mContext, mNetworkScorer, mNotificationChannelHelper, mPackageManager);
        ComponentName scorer = new ComponentName(TEST_SCORER_PACKAGE, TEST_SCORER_CLASS);

        NetworkScorerAppData scorerAppData = new NetworkScorerAppData(
                0, scorer, TEST_SCORER_LABEL, null /* enableUseOpenWifiActivity */,
                NOTIFICATION_ID);
        when(mNetworkScorer.getActiveScorer()).thenReturn(scorerAppData);
    }

    @Test
    public void testIsAvailable_shouldReturnFalseWhenScorerDoesNotExist()
            throws RemoteException {
        when(mNetworkScorer.getActiveScorer()).thenReturn(null);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testIsAvailable_shouldReturnFalseWhenNotificationChannelIdDoesNotExist()
            throws RemoteException {
        ComponentName scorer = new ComponentName(TEST_SCORER_PACKAGE, TEST_SCORER_CLASS);
        NetworkScorerAppData scorerAppData = new NetworkScorerAppData(
                0, scorer, TEST_SCORER_LABEL, null /* enableUseOpenWifiActivity */,
                null /* networkAvailableNotificationChannelId */);
        when(mNetworkScorer.getActiveScorer()).thenReturn(scorerAppData);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testIsAvailable_shouldReturnFalseWhenNotificationChannelDoesNotExist()
            throws RemoteException {
        when(mNotificationChannelHelper.getNotificationChannelForPackage(
                anyString(), anyInt(), anyString(), anyBoolean())).thenReturn(null);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testIsAvailable_shouldReturnTrueWhenNotificationChannelExists()
            throws RemoteException {
        when(mNotificationChannelHelper.getNotificationChannelForPackage(
                anyString(), anyInt(), anyString(), anyBoolean())).thenReturn(mChannel);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void handlePreferenceTreeClick_nonMatchingKey_shouldDoNothing() {
        final Preference pref = new Preference(mContext);

        assertThat(mController.handlePreferenceTreeClick(pref)).isFalse();
    }

    @Test
    public void handlePreferenceTreeClick_nullScorer_shouldDoNothing() {
        final Preference pref = new Preference(mContext);
        pref.setKey(mController.getPreferenceKey());
        when(mNetworkScorer.getActiveScorer()).thenReturn(null);

        assertThat(mController.handlePreferenceTreeClick(pref)).isFalse();
    }

    @Test
    public void handlePreferenceTreeClick_matchingKeyAndScorerExists_shouldLaunchActivity()
            throws RemoteException {
        final Preference pref = new Preference(mContext);
        pref.setKey(mController.getPreferenceKey());
        when(mNotificationChannelHelper.getNotificationChannelForPackage(
                anyString(), anyInt(), anyString(), anyBoolean())).thenReturn(mChannel);

        assertThat(mController.handlePreferenceTreeClick(pref)).isTrue();
    }

    @Test
    public void updateState_notificationsEnabled_shouldShowEnabledSummary() throws RemoteException {
        final Preference pref = new Preference(mContext);
        pref.setKey(mController.getPreferenceKey());
        when(mNotificationChannelHelper.getNotificationChannelForPackage(
                anyString(), anyInt(), anyString(), anyBoolean())).thenReturn(mChannel);
        when(mChannel.getImportance()).thenReturn(NotificationManager.IMPORTANCE_DEFAULT);
        mController.updateState(pref);

        assertThat(pref.getSummary()).isEqualTo(
                mContext.getString(R.string.notification_toggle_on));
    }

    @Test
    public void updateState_notificationsEnabled_shouldShowDisabledSummary()
            throws RemoteException {
        final Preference pref = new Preference(mContext);
        pref.setKey(mController.getPreferenceKey());
        when(mNotificationChannelHelper.getNotificationChannelForPackage(
                anyString(), anyInt(), anyString(), anyBoolean())).thenReturn(mChannel);
        when(mChannel.getImportance()).thenReturn(NotificationManager.IMPORTANCE_NONE);
        mController.updateState(pref);

        assertThat(pref.getSummary()).isEqualTo(
                mContext.getString(R.string.notification_toggle_off));
    }

}
