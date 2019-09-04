package com.android.settings.location;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class LocationSliceTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
    }

    @Test
    public void getLocationSlice_correctSliceContent() {
        final Slice LocationSlice = new LocationSlice(mContext).getSlice();

        final SliceMetadata metadata = SliceMetadata.from(mContext, LocationSlice);
        assertThat(metadata.getTitle()).isEqualTo(
                mContext.getString(R.string.location_settings_title));

        final List<SliceAction> toggles = metadata.getToggles();
        assertThat(toggles).isEmpty();

        final SliceAction primaryAction = metadata.getPrimaryAction();
        final IconCompat expectedToggleIcon = IconCompat.createWithResource(mContext,
                com.android.internal.R.drawable.ic_signal_location);
        assertThat(primaryAction.getIcon().toString()).isEqualTo(expectedToggleIcon.toString());
    }
}
