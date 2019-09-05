package com.android.settings.applications;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settingslib.applications.StorageStatsSource.AppStorageStats;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AppStorageSizesControllerTest {

    private static final String COMPUTING = "Computing…";
    private static final String INVALID_SIZE = "Couldn’t compute package size.";
    private AppStorageSizesController mController;
    private Context mContext;

    private Preference mAppPreference;
    private Preference mCachePreference;
    private Preference mDataPreference;
    private Preference mTotalPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mAppPreference = new Preference(mContext);
        mCachePreference = new Preference(mContext);
        mDataPreference = new Preference(mContext);
        mTotalPreference = new Preference(mContext);

        mController = new AppStorageSizesController.Builder()
                .setAppSizePreference(mAppPreference)
                .setCacheSizePreference(mCachePreference)
                .setDataSizePreference(mDataPreference)
                .setTotalSizePreference(mTotalPreference)
                .setErrorString(R.string.invalid_size_value)
                .setComputingString(R.string.computing_size)
                .build();
    }

    @Test
    public void requestingUpdateBeforeValuesSetIsComputing() {
        mController.updateUi(mContext);

        assertThat(mAppPreference.getSummary()).isEqualTo(COMPUTING);
        assertThat(mCachePreference.getSummary()).isEqualTo(COMPUTING);
        assertThat(mDataPreference.getSummary()).isEqualTo(COMPUTING);
        assertThat(mTotalPreference.getSummary()).isEqualTo(COMPUTING);
    }

    @Test
    public void requestingUpdateAfterFailureHasErrorText() {
        mController.setResult(null);
        mController.updateUi(mContext);

        assertThat(mAppPreference.getSummary()).isEqualTo(INVALID_SIZE);
        assertThat(mCachePreference.getSummary()).isEqualTo(INVALID_SIZE);
        assertThat(mDataPreference.getSummary()).isEqualTo(INVALID_SIZE);
        assertThat(mTotalPreference.getSummary()).isEqualTo(INVALID_SIZE);
    }

    @Test
    public void properlyPopulatedAfterValidEntry() {
        AppStorageStats result = mock(AppStorageStats.class);
        when(result.getCodeBytes()).thenReturn(1L);
        when(result.getCacheBytes()).thenReturn(10L);
        when(result.getDataBytes()).thenReturn(100L);
        when(result.getTotalBytes()).thenReturn(101L);

        mController.setResult(result);
        mController.updateUi(mContext);

        assertThat(mAppPreference.getSummary()).isEqualTo("1 B");
        assertThat(mCachePreference.getSummary()).isEqualTo("10 B");
        assertThat(mDataPreference.getSummary()).isEqualTo("90 B");
        assertThat(mTotalPreference.getSummary()).isEqualTo("101 B");
    }

    @Test
    public void fakeCacheFlagSetsCacheToZero() {
        AppStorageStats result = mock(AppStorageStats.class);
        when(result.getCodeBytes()).thenReturn(1L);
        when(result.getCacheBytes()).thenReturn(10L);
        when(result.getDataBytes()).thenReturn(100L);
        when(result.getTotalBytes()).thenReturn(101L);

        mController.setResult(result);
        mController.setCacheCleared(true);
        mController.updateUi(mContext);

        assertThat(mAppPreference.getSummary()).isEqualTo("1 B");
        assertThat(mCachePreference.getSummary()).isEqualTo("0 B");
        assertThat(mDataPreference.getSummary()).isEqualTo("90 B");
        assertThat(mTotalPreference.getSummary()).isEqualTo("91 B");
    }

    @Test
    public void fakeDataFlagSetsDataAndCacheToZero() {
        AppStorageStats result = mock(AppStorageStats.class);
        when(result.getCodeBytes()).thenReturn(1L);
        when(result.getCacheBytes()).thenReturn(10L);
        when(result.getDataBytes()).thenReturn(100L);
        when(result.getTotalBytes()).thenReturn(101L);

        mController.setResult(result);
        mController.setDataCleared(true);
        mController.updateUi(mContext);

        assertThat(mAppPreference.getSummary()).isEqualTo("1 B");
        assertThat(mCachePreference.getSummary()).isEqualTo("0 B");
        assertThat(mDataPreference.getSummary()).isEqualTo("0 B");
        assertThat(mTotalPreference.getSummary()).isEqualTo("1 B");
    }
}
