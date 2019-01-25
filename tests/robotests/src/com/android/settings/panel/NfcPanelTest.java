package com.android.settings.panel;

import static com.google.common.truth.Truth.assertThat;

import android.net.Uri;

import com.android.settings.slices.CustomSliceRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;


import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class NfcPanelTest {

    private NfcPanel mPanel;

    @Before
    public void setUp() {
        mPanel = NfcPanel.create(RuntimeEnvironment.application);
    }

    @Test
    public void getSlices_containsNecessarySlices() {
        final List<Uri> uris = mPanel.getSlices();

        assertThat(uris).containsExactly(
                CustomSliceRegistry.NFC_SLICE_URI);
    }

    @Test
    public void getSeeMoreIntent_notNull() {
        assertThat(mPanel.getSeeMoreIntent()).isNotNull();
    }
}
