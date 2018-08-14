package com.android.settings.location;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.graphics.drawable.IconCompat;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.SliceTester;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
import androidx.slice.widget.SliceLiveData;

@RunWith(SettingsRobolectricTestRunner.class)
public class LocationSliceBuilderTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);

        // Prevent crash in SliceMetadata.
        Resources resources = spy(mContext.getResources());
        doReturn(60).when(resources).getDimensionPixelSize(anyInt());
        doReturn(resources).when(mContext).getResources();

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
    }

    @Test
    public void getLocationSlice_correctSliceContent() {
        final Slice LocationSlice = LocationSliceBuilder.getSlice(mContext);
        final SliceMetadata metadata = SliceMetadata.from(mContext, LocationSlice);

        final List<SliceAction> toggles = metadata.getToggles();
        assertThat(toggles).isEmpty();

        final SliceAction primaryAction = metadata.getPrimaryAction();
        final IconCompat expectedToggleIcon = IconCompat.createWithResource(mContext,
                R.drawable.ic_signal_location);
        assertThat(primaryAction.getIcon().toString()).isEqualTo(expectedToggleIcon.toString());

        final List<SliceItem> sliceItems = LocationSlice.getItems();
        SliceTester.assertTitle(sliceItems, mContext.getString(R.string.location_settings_title));
    }
}
