package com.android.settings.deviceinfo;

import android.app.Fragment;
import android.content.Context;

import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class FeedbackPreferenceControllerTest {
    @Mock
    private Fragment mFragment;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    private FeedbackPreferenceController mController;

    public FeedbackPreferenceControllerTest() {
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.mController = new FeedbackPreferenceController(this.mFragment, this.mContext);
    }

    @Test
    public void isAvailable_noReporterPackage_shouldReturnFalse() {
        when(this.mContext.getResources().getString(anyInt())).thenReturn("");
        assertThat(Boolean.valueOf(this.mController.isAvailable())).isFalse();
    }
}
