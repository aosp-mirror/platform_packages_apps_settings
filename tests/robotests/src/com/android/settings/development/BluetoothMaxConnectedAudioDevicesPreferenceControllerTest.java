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

package com.android.settings.development;

import static com.android.settings.development
        .BluetoothMaxConnectedAudioDevicesPreferenceController.MAX_CONNECTED_AUDIO_DEVICES_PROPERTY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.os.SystemProperties;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BluetoothMaxConnectedAudioDevicesPreferenceControllerTest {

  private static final int TEST_MAX_CONNECTED_AUDIO_DEVICES = 3;

  @Mock
  private PreferenceScreen mPreferenceScreen;
  @Spy
  private Context mSpyContext = RuntimeEnvironment.application;
  @Spy
  private Resources mSpyResources = RuntimeEnvironment.application.getResources();

  private ListPreference mPreference;
  private BluetoothMaxConnectedAudioDevicesPreferenceController mController;

  private CharSequence[] mListValues;
  private CharSequence[] mListEntries;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    doReturn(mSpyResources).when(mSpyContext).getResources();
    // Get XML values without mock
    // Setup test list preference using XML values
    mPreference = new ListPreference(mSpyContext);
    mPreference.setEntries(R.array.bluetooth_max_connected_audio_devices);
    mPreference.setEntryValues(R.array.bluetooth_max_connected_audio_devices_values);
    // Stub default max connected audio devices to a test controlled value
    doReturn(TEST_MAX_CONNECTED_AUDIO_DEVICES).when(mSpyResources).getInteger(
        com.android.internal.R.integer.config_bluetooth_max_connected_audio_devices);
    // Init the actual controller
    mController = new BluetoothMaxConnectedAudioDevicesPreferenceController(mSpyContext);
    // Construct preference in the controller via a mocked preference screen object
    when(mPreferenceScreen.findPreference(mController.getPreferenceKey())).thenReturn(
        mPreference);
    mController.displayPreference(mPreferenceScreen);
    mListValues = mPreference.getEntryValues();
    mListEntries = mPreference.getEntries();
  }

  @Test
  public void verifyResourceSizeAndRange() {
    // Verify normal list entries and default preference entries have the same size
    assertThat(mListEntries.length).isEqualTo(mListValues.length);
    // Verify that list entries are formatted correctly
    final String defaultEntry = String.format(mListEntries[0].toString(),
        TEST_MAX_CONNECTED_AUDIO_DEVICES);
    assertThat(mListEntries[0]).isEqualTo(defaultEntry);
    // Update the preference
    mController.updateState(mPreference);
    // Verify default preference value, entry and summary
    assertThat(mPreference.getValue()).isEqualTo(mListValues[0]);
    assertThat(mPreference.getEntry()).isEqualTo(mListEntries[0]);
    assertThat(mPreference.getSummary()).isEqualTo(mListEntries[0]);
    // Verify that default system property is empty
    assertThat(SystemProperties.get(MAX_CONNECTED_AUDIO_DEVICES_PROPERTY)).isEmpty();
    // Verify default property integer value
    assertThat(SystemProperties.getInt(MAX_CONNECTED_AUDIO_DEVICES_PROPERTY,
        TEST_MAX_CONNECTED_AUDIO_DEVICES)).isEqualTo(TEST_MAX_CONNECTED_AUDIO_DEVICES);
  }

  @Test
  public void onPreferenceChange_setNumberOfDevices() {
    for (final CharSequence newValue : mListValues) {
      // Change preference using a list value
      mController.onPreferenceChange(mPreference, newValue);
      // Verify that value is set on the preference
      assertThat(mPreference.getValue()).isEqualTo(newValue);
      int index = mPreference.findIndexOfValue(newValue.toString());
      assertThat(mPreference.getEntry()).isEqualTo(mListEntries[index]);
      // Verify that system property is set correctly after the change
      final String currentValue = SystemProperties.get(MAX_CONNECTED_AUDIO_DEVICES_PROPERTY);
      assertThat(currentValue).isEqualTo(mListValues[index]);
    }
  }

  @Test
  public void updateState_NumberOfDevicesUpdated_shouldSetPreference() {
    for (int i = 0; i < mListValues.length; ++i) {
      final String propertyValue = mListValues[i].toString();
      SystemProperties.set(MAX_CONNECTED_AUDIO_DEVICES_PROPERTY, propertyValue);
      // Verify that value is set on the preference
      mController.updateState(mPreference);
      assertThat(mPreference.getValue()).isEqualTo(mListValues[i]);
      assertThat(mPreference.getEntry()).isEqualTo(mListEntries[i]);
      assertThat(mPreference.getSummary()).isEqualTo(mListEntries[i]);
      // Verify that property value remain unchanged
      assertThat(SystemProperties.get(MAX_CONNECTED_AUDIO_DEVICES_PROPERTY))
          .isEqualTo(propertyValue);
    }
  }

  @Test
  public void updateState_noValueSet_shouldSetDefaultTo1device() {
    SystemProperties.set(MAX_CONNECTED_AUDIO_DEVICES_PROPERTY, "garbage");
    mController.updateState(mPreference);

    // Verify that preference is reset back to default and property is reset to default
    assertThat(mPreference.getValue()).isEqualTo(mListValues[0]);
    assertThat(mPreference.getEntry()).isEqualTo(mListEntries[0]);
    assertThat(mPreference.getSummary()).isEqualTo(mListEntries[0]);
    assertThat(SystemProperties.get(MAX_CONNECTED_AUDIO_DEVICES_PROPERTY)).isEmpty();
  }

  @Test
  public void onDeveloperOptionsSwitchDisabled_shouldDisablePreference() {
    mController.onDeveloperOptionsSwitchDisabled();

    assertThat(mPreference.isEnabled()).isFalse();
    // Verify that preference is reset back to default and property is reset to default
    assertThat(mPreference.getValue()).isEqualTo(mListValues[0]);
    assertThat(mPreference.getEntry()).isEqualTo(mListEntries[0]);
    assertThat(mPreference.getSummary()).isEqualTo(mListEntries[0]);
    assertThat(SystemProperties.get(MAX_CONNECTED_AUDIO_DEVICES_PROPERTY)).isEmpty();
  }

  @Test
  public void onDeveloperOptionsSwitchEnabled_shouldEnablePreference() {
    for (int i = 0; i < mListValues.length; ++i) {
      final String initialValue = mListValues[i].toString();
      mController.onDeveloperOptionsSwitchDisabled();
      assertThat(mPreference.isEnabled()).isFalse();

      SystemProperties.set(MAX_CONNECTED_AUDIO_DEVICES_PROPERTY, initialValue);
      mController.onDeveloperOptionsSwitchEnabled();

      assertThat(mPreference.isEnabled()).isTrue();
      assertThat(mPreference.getValue()).isEqualTo(mListValues[i]);
      assertThat(mPreference.getEntry()).isEqualTo(mListEntries[i]);
      assertThat(mPreference.getSummary()).isEqualTo(mListEntries[i]);
      // Verify that property value remain unchanged
      assertThat(SystemProperties.get(MAX_CONNECTED_AUDIO_DEVICES_PROPERTY))
          .isEqualTo(initialValue);
    }
  }
}
