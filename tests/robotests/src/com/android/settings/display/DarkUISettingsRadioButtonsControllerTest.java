package com.android.settings.display;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import android.app.UiModeManager;
import android.content.Context;
import androidx.preference.Preference;
import com.android.settings.R;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class DarkUISettingsRadioButtonsControllerTest {

    @Mock
    private UiModeManager mUiModeManager;
    @Mock
    private Preference mFooter;
    private Context mContext;
    private DarkUISettingsRadioButtonsController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new DarkUISettingsRadioButtonsController(mContext, mFooter);
        mController.mManager = mUiModeManager;
    }

    @Test
    public void footerUpdatesCorrectly() {
        doReturn(UiModeManager.MODE_NIGHT_YES).when(mUiModeManager).getNightMode();
        mController.updateFooter();
        verify(mFooter).setSummary(eq(R.string.dark_ui_settings_dark_summary));

        doReturn(UiModeManager.MODE_NIGHT_NO).when(mUiModeManager).getNightMode();
        mController.updateFooter();
        verify(mFooter).setSummary(eq(R.string.dark_ui_settings_light_summary));
    }

    public int getCurrentMode() {
        final UiModeManager uiModeManager = mContext.getSystemService(UiModeManager.class);
        return uiModeManager.getNightMode();
    }
}
