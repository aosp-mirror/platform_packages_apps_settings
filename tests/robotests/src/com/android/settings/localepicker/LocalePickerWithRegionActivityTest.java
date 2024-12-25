package com.android.settings.localepicker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import android.app.Activity;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import com.android.internal.app.LocaleStore;
import com.android.settings.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowActivity;

@RunWith(RobolectricTestRunner.class)
public class LocalePickerWithRegionActivityTest {

    private LocalePickerWithRegionActivity mActivity;

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final ActivityController<LocalePickerWithRegionActivity> mActivityController =
                Robolectric.buildActivity(LocalePickerWithRegionActivity.class);
        mActivity = spy(mActivityController.get());
    }

    @Test
    @DisableFlags(Flags.FLAG_REGIONAL_PREFERENCES_API_ENABLED)
    public void onLocaleSelected_resultShouldBeOK() {
        final ShadowActivity shadowActivity = Shadows.shadowOf(mActivity);
        mActivity.onLocaleSelected(mock(LocaleStore.LocaleInfo.class));

        assertEquals(Activity.RESULT_OK, shadowActivity.getResultCode());
    }

    @Test
    @DisableFlags(Flags.FLAG_REGIONAL_PREFERENCES_API_ENABLED)
    public void onLocaleSelected_localeInfoShouldBeSentBack() {
        final ShadowActivity shadowActivity = Shadows.shadowOf(mActivity);
        mActivity.onLocaleSelected(mock(LocaleStore.LocaleInfo.class));

        assertNotNull(shadowActivity.getResultIntent().getSerializableExtra(
                LocaleListEditor.INTENT_LOCALE_KEY));
    }
}
