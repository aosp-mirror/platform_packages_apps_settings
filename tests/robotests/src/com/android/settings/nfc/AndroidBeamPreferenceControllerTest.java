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

package com.android.settings.nfc;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.shadow.ShadowNfcAdapter;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowNfcAdapter.class)
public class AndroidBeamPreferenceControllerTest {

    Context mContext;
    @Mock
    NfcManager mNfcManager;
    @Mock
    private UserManager mUserManager;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private PackageManager mPackageManager;

    private RestrictedPreference mAndroidBeamPreference;
    private AndroidBeamPreferenceController mAndroidBeamController;
    private ShadowNfcAdapter mShadowNfcAdapter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mShadowNfcAdapter = Shadow.extract(NfcAdapter.getDefaultAdapter(mContext));

        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mContext.getSystemService(Context.NFC_SERVICE)).thenReturn(mNfcManager);
        when(RestrictedLockUtilsInternal.hasBaseUserRestriction(mContext,
                UserManager.DISALLOW_OUTGOING_BEAM, UserHandle.myUserId())).thenReturn(false);

        mAndroidBeamController = new AndroidBeamPreferenceController(mContext,
                AndroidBeamPreferenceController.KEY_ANDROID_BEAM_SETTINGS);
        mAndroidBeamPreference = new RestrictedPreference(RuntimeEnvironment.application);
        when(mScreen.findPreference(mAndroidBeamController.getPreferenceKey())).thenReturn(
                mAndroidBeamPreference);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_NFC_BEAM)).thenReturn(true);

        Settings.Global.putString(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS,
                Settings.Global.RADIO_NFC);
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON,
                0);
        mAndroidBeamController.displayPreference(mScreen);
    }

    @Test
    public void isAvailable_hasNfc_shouldReturnTrue() {
        mShadowNfcAdapter.setEnabled(true);
        assertThat(mAndroidBeamController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_noNfcFeature_shouldReturnFalse() {
        mShadowNfcAdapter.setEnabled(true);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_NFC_BEAM)).thenReturn(false);
        assertThat(mAndroidBeamController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_noNfcAdapter_shouldReturnFalse() {
        ReflectionHelpers.setField(mAndroidBeamController, "mNfcAdapter", null);
        assertThat(mAndroidBeamController.isAvailable()).isFalse();
    }

    @Test
    public void isBeamEnable_disAllowBeam_shouldReturnFalse() {
        mShadowNfcAdapter.setAdapterState(NfcAdapter.STATE_OFF);

        when(RestrictedLockUtilsInternal.hasBaseUserRestriction(mContext,
                UserManager.DISALLOW_OUTGOING_BEAM, UserHandle.myUserId())).thenReturn(true);
        mAndroidBeamController.displayPreference(mScreen);

        assertThat(mAndroidBeamPreference.isEnabled()).isFalse();
    }

    @Test
    public void isBeamEnable_nfcStateOn_shouldReturnTrue() {
        mShadowNfcAdapter.setAdapterState(NfcAdapter.STATE_ON);
        try {
            mAndroidBeamController.onResume();
        } catch (NullPointerException e) {
            // skip because it's just test
            // it will meet NullPointerException in checkRestrictionAndSetDisabled
        }
        assertThat(mAndroidBeamPreference.isEnabled()).isTrue();
    }

    @Test
    public void isBeamEnable_nfcStateNotOn_shouldReturnFalse() {
        mShadowNfcAdapter.setAdapterState(NfcAdapter.STATE_OFF);
        mAndroidBeamController.onResume();
        assertThat(mAndroidBeamPreference.isEnabled()).isFalse();

        mShadowNfcAdapter.setAdapterState(NfcAdapter.STATE_TURNING_ON);
        mAndroidBeamController.onResume();
        assertThat(mAndroidBeamPreference.isEnabled()).isFalse();

        mShadowNfcAdapter.setAdapterState(NfcAdapter.STATE_TURNING_OFF);
        mAndroidBeamController.onResume();
        assertThat(mAndroidBeamPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateNonIndexableKeys_available_shouldNotUpdate() {
        mShadowNfcAdapter.setEnabled(true);
        final List<String> keys = new ArrayList<>();

        mAndroidBeamController.updateNonIndexableKeys(keys);

        assertThat(keys).isEmpty();
    }

    @Test
    public void updateNonIndexableKeys_notAvailable_shouldUpdate() {
        ReflectionHelpers.setField(mAndroidBeamController, "mNfcAdapter", null);
        final List<String> keys = new ArrayList<>();

        mAndroidBeamController.updateNonIndexableKeys(keys);

        assertThat(keys).hasSize(1);
    }
}
