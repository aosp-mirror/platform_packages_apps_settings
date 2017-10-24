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
 *
 */

package com.android.settings.search;

import android.content.ContentResolver;
import android.content.Intent;
import android.os.Parcel;
import android.provider.Settings;
import android.content.Context;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.search.ResultPayload.Availability;
import com.android.settings.search.ResultPayload.SettingsSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static com.google.common.truth.Truth.assertThat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class InlineSwitchPayloadTest {

    private static final String DUMMY_SETTING = "inline_test";
    private static final int STANDARD_ON = 1;
    private static final int FLIPPED_ON = 0;

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void testConstructor_DataRetained() {
        final String uri = "test.com";
        final int type = ResultPayload.PayloadType.INLINE_SWITCH;
        final int source = SettingsSource.SECURE;
        final String intentKey = "key";
        final String intentVal = "value";
        final Intent intent = new Intent();
        intent.putExtra(intentKey, intentVal);

        InlineSwitchPayload payload = new InlineSwitchPayload(uri, source, 1, intent, true,
                1 /* default */);
        final Intent retainedIntent = payload.getIntent();
        assertThat(payload.mSettingKey).isEqualTo(uri);
        assertThat(payload.getType()).isEqualTo(type);
        assertThat(payload.mSettingSource).isEqualTo(source);
        assertThat(payload.isStandard()).isTrue();
        assertThat(payload.getAvailability()).isEqualTo(ResultPayload.Availability.AVAILABLE);
        assertThat(retainedIntent.getStringExtra(intentKey)).isEqualTo(intentVal);
    }

    @Test
    public void testParcelConstructor_DataRetained() {
        String uri = "test.com";
        int type = ResultPayload.PayloadType.INLINE_SWITCH;
        int source = SettingsSource.SECURE;
        final String intentKey = "key";
        final String intentVal = "value";
        final Intent intent = new Intent();
        intent.putExtra(intentKey, intentVal);
        Parcel parcel = Parcel.obtain();
        parcel.writeParcelable(intent, 0);
        parcel.writeString(uri);
        parcel.writeInt(source);
        parcel.writeInt(InlineSwitchPayload.TRUE);
        parcel.writeInt(InlineSwitchPayload.TRUE);
        parcel.writeInt(InlineSwitchPayload.TRUE);
        parcel.setDataPosition(0);

        InlineSwitchPayload payload = InlineSwitchPayload.CREATOR.createFromParcel(parcel);

        final Intent builtIntent = payload.getIntent();
        assertThat(payload.mSettingKey).isEqualTo(uri);
        assertThat(payload.getType()).isEqualTo(type);
        assertThat(payload.mSettingSource).isEqualTo(source);
        assertThat(payload.isStandard()).isTrue();
        assertThat(payload.getAvailability()).isEqualTo(Availability.AVAILABLE);
        assertThat(builtIntent.getStringExtra(intentKey)).isEqualTo(intentVal);
    }

    @Test
    public void testGetSystem_flippedSetting_returnsFlippedValue() {
        // Stores 1s as 0s, and vis versa
        InlineSwitchPayload payload = new InlineSwitchPayload(DUMMY_SETTING, SettingsSource.SYSTEM,
                FLIPPED_ON, null /* intent */, true, 1 /* default */);
        int currentValue = 1;
        Settings.System.putInt(mContext.getContentResolver(), DUMMY_SETTING, currentValue);

        int newValue = payload.getValue(mContext);

        assertThat(newValue).isEqualTo(1 - currentValue);
    }

    @Test
    public void testSetSystem_flippedSetting_updatesToFlippedValue() {
        // Stores 1s as 0s, and vis versa
        InlineSwitchPayload payload = new InlineSwitchPayload(DUMMY_SETTING, SettingsSource.SYSTEM,
                FLIPPED_ON, null /* intent */, true, 1 /* default */);
        int newValue = 1;
        ContentResolver resolver = mContext.getContentResolver();
        Settings.System.putInt(resolver, SCREEN_BRIGHTNESS_MODE, newValue);

        payload.setValue(mContext, newValue);
        int updatedValue = Settings.System.getInt(resolver, DUMMY_SETTING, -1);

        assertThat(updatedValue).isEqualTo(1 - newValue);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetSystem_negativeValue_ThrowsError() {
        InlineSwitchPayload payload = new InlineSwitchPayload(DUMMY_SETTING, SettingsSource.SYSTEM,
                STANDARD_ON, null /* intent */, true, 1 /* default */);

        payload.setValue(mContext, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetSystem_highValue_ThrowsError() {
        InlineSwitchPayload payload = new InlineSwitchPayload(DUMMY_SETTING, SettingsSource.SYSTEM,
                STANDARD_ON, null /* intent */, true, 1 /* default */);

        payload.setValue(mContext, 2);
    }
}
