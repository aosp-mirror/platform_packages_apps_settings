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

package com.android.settings.development;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.RemoteException;
import android.os.storage.IStorageManager;
import android.sysprop.CryptoProperties;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class FileEncryptionPreferenceControllerTest {

    @Mock
    private Preference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private IStorageManager mStorageManager;

    private Context mContext;
    private FileEncryptionPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new FileEncryptionPreferenceController(mContext);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
            .thenReturn(mPreference);
    }

    @Test
    public void isAvailable_storageManagerNull_shouldBeFalse() {
        ReflectionHelpers.setField(mController, "mStorageManager", null);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_notConvertibleToFBE_shouldBeFalse() throws RemoteException {
        ReflectionHelpers.setField(mController, "mStorageManager", mStorageManager);
        when(mStorageManager.isConvertibleToFBE()).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_convertibleToFBE_shouldBeTrue() throws RemoteException {
        ReflectionHelpers.setField(mController, "mStorageManager", mStorageManager);
        when(mStorageManager.isConvertibleToFBE()).thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_settingIsNotFile_shouldDoNothing() throws RemoteException {
        ReflectionHelpers.setField(mController, "mStorageManager", mStorageManager);
        when(mStorageManager.isConvertibleToFBE()).thenReturn(true);
        mController.displayPreference(mPreferenceScreen);
        CryptoProperties.type(CryptoProperties.type_values.NONE);

        mController.updateState(mPreference);

        verify(mPreference, never()).setEnabled(anyBoolean());
        verify(mPreference, never()).setSummary(anyString());
    }

    @Test
    public void updateState_settingIsFile_shouldSetSummaryAndDisablePreference()
            throws RemoteException {
        ReflectionHelpers.setField(mController, "mStorageManager", mStorageManager);
        when(mStorageManager.isConvertibleToFBE()).thenReturn(true);
        mController.displayPreference(mPreferenceScreen);
        CryptoProperties.type(CryptoProperties.type_values.FILE);

        mController.updateState(mPreference);

        verify(mPreference).setEnabled(false);
        verify(mPreference).setSummary(mContext.getString(R.string.convert_to_file_encryption_done));
    }
}
