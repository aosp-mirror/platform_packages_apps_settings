package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowEntityHeaderController;

import com.android.settings.wifi.WifiDialog.WifiDialogListener;
import com.android.settingslib.wifi.AccessPoint;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;


@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = ShadowEntityHeaderController.class)
public class WifiDialogTest {
    @Mock private AccessPoint mockAccessPoint;

    private Context mContext = RuntimeEnvironment.application;

    private WifiDialogListener mListener = new WifiDialogListener() {
        @Override
        public void onForget(WifiDialog dialog) {
        }

        @Override
        public void onSubmit(WifiDialog dialog) {
        }
    };

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void createFullscreen_setsFullscreenTheme() {
        WifiDialog fullscreen = WifiDialog.createFullscreen(mContext, mListener, mockAccessPoint,
                WifiConfigUiBase.MODE_CONNECT);
        assertThat(fullscreen.getContext().getThemeResId())
                .isEqualTo(R.style.Theme_Settings_NoActionBar);
    }

    @Test
    public void createModal_usesDefaultTheme() {
        WifiDialog modal = WifiDialog
                .createModal(mContext, mListener, mockAccessPoint, WifiConfigUiBase.MODE_CONNECT);

        WifiDialog wifiDialog = new WifiDialog(mContext, mListener, mockAccessPoint,
                WifiConfigUiBase.MODE_CONNECT, 0 /* style */, false /* hideSubmitButton */);
        assertThat(modal.getContext().getThemeResId())
                .isEqualTo(wifiDialog.getContext().getThemeResId());
    }
}
