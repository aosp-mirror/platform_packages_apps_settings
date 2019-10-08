package com.android.settings.widget;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class DonutViewTest {
    @Test
    public void getPercentageStringSpannable_doesntCrashForMissingPercentage() {
        Context context = RuntimeEnvironment.application;

        DonutView.getPercentageStringSpannable(context.getResources(), "50%", "h");
    }
}
