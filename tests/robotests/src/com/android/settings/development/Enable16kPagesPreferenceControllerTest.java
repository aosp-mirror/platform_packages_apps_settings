/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.os.FileUtils;
import android.os.SystemProperties;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@RunWith(RobolectricTestRunner.class)
public class Enable16kPagesPreferenceControllerTest {

    @Mock private DevelopmentSettingsDashboardFragment mFragment;
    private Context mContext;
    private Enable16kPagesPreferenceController mController;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mController = spy(new Enable16kPagesPreferenceController(mContext, mFragment));
        doNothing().when(mController).applyPayload(any(), anyLong(), anyLong(), anyList());
    }

    @Test
    public void onSystemPropertyDisabled_shouldDisablePreference() {
        SystemProperties.set(Enable16kPagesPreferenceController.DEV_OPTION_PROPERTY, "false");
        assertThat(mController.isAvailable()).isEqualTo(false);
    }

    @Test
    public void onSystemPropertyEnabled_shouldEnablePreference() {
        SystemProperties.set(Enable16kPagesPreferenceController.DEV_OPTION_PROPERTY, "true");
        assertThat(mController.isAvailable()).isEqualTo(true);
    }

    // TODO(b/303280163) : add tests to based on page size and whether preference is turned on/off

    @Test
    public void validateUpdateParsing_validFile() throws IOException {
        // TODO(b/295573133) : Add tests to verify applyPayload arguments
        String filename = "valid_ota.zip";
        File updateFile = copyFromAssetToDataDir(filename);
        mController.applyUpdateFile(updateFile);
    }

    @Test
    public void validateUpdateParsing_emptyPayloadFile() {
        String filename = "empty_payload_ota.zip";
        File updateFile = copyFromAssetToDataDir(filename);
        assertThrows(IOException.class, () -> mController.applyUpdateFile(updateFile));
    }

    @Test
    public void validateUpdateParsing_noPayloadFile() {
        String filename = "no_payload_ota.zip";
        File updateFile = copyFromAssetToDataDir(filename);
        assertThrows(FileNotFoundException.class, () -> mController.applyUpdateFile(updateFile));
    }

    @Test
    public void validateUpdateParsing_noPropertiesFile() {
        String filename = "no_properties_ota.zip";
        File updateFile = copyFromAssetToDataDir(filename);
        assertThrows(FileNotFoundException.class, () -> mController.applyUpdateFile(updateFile));
    }

    private File copyFromAssetToDataDir(String filename) {
        try {
            InputStream in = mContext.getAssets().open(filename);
            File destination =
                    File.createTempFile(
                            "test_update", ".zip", new File(mContext.getDataDir().getPath()));
            FileUtils.setPermissions(
                    /* path= */ destination,
                    /* mode= */ FileUtils.S_IRWXU | FileUtils.S_IRGRP | FileUtils.S_IROTH,
                    /* uid= */ -1,
                    /* gid= */ -1);
            OutputStream out = new FileOutputStream(destination);
            FileUtils.copy(in, out);
            return destination;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
