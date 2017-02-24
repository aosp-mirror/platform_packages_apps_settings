package com.android.settings.deviceinfo;


import android.content.Context;

import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class KernelVersionPreferenceControllerTest {

    @Mock
    private Context mContext;
    private KernelVersionPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new KernelVersionPreferenceController(mContext);
    }

    @Test
    public void alwaysAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }

}
