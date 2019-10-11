package com.android.settings.localepicker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import android.app.Activity;

import com.android.internal.app.LocaleStore;

import org.junit.Before;
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

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final ActivityController<LocalePickerWithRegionActivity> mActivityController =
                Robolectric.buildActivity(LocalePickerWithRegionActivity.class);
        mActivity = spy(mActivityController.get());
    }

    @Test
    public void onLocaleSelected_resultShouldBeOK() {
        final ShadowActivity shadowActivity = Shadows.shadowOf(mActivity);
        mActivity.onLocaleSelected(mock(LocaleStore.LocaleInfo.class));

        assertEquals(Activity.RESULT_OK, shadowActivity.getResultCode());
    }

    @Test
    public void onLocaleSelected_localeInfoShouldBeSentBack() {
        final ShadowActivity shadowActivity = Shadows.shadowOf(mActivity);
        mActivity.onLocaleSelected(mock(LocaleStore.LocaleInfo.class));

        assertNotNull(shadowActivity.getResultIntent().getSerializableExtra(
                LocaleListEditor.INTENT_LOCALE_KEY));
    }
}
