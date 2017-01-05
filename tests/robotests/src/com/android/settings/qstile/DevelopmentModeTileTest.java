package com.android.settings.qstile;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.service.quicksettings.Tile;

import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.shadow.ShadowTileService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = ShadowTileService.class)
public class DevelopmentModeTileTest {

    @Mock private Tile mTile;
    @Mock private DevelopmentModeTile.DevModeProperties mProps;

    private DevelopmentModeTile mDevelopmentModeTile;
    private ShadowTileService mShadowTileService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mDevelopmentModeTile = Robolectric.buildService(DevelopmentModeTile.class).get();

        ReflectionHelpers.setField(mDevelopmentModeTile, "mProps", mProps);
        mShadowTileService = (ShadowTileService) ShadowExtractor.extract(mDevelopmentModeTile);
        mShadowTileService.setTile(mTile);
    }

    @Test
    public void refresh() {
        verifyRefreshState(false, true, Tile.STATE_UNAVAILABLE);
        verifyRefreshState(false, false, Tile.STATE_UNAVAILABLE);
        verifyRefreshState(true, false, Tile.STATE_INACTIVE);
        verifyRefreshState(true, true, Tile.STATE_ACTIVE);
    }

    @Test
    public void onClick_startSetting() {
        when(mTile.getState()).thenReturn(Tile.STATE_UNAVAILABLE);
        mDevelopmentModeTile.onClick();

        Intent intent = mShadowTileService.getNextStartedActivity();
        assertEquals(DevelopmentTileConfigActivity.class.getName(),
                intent.getComponent().getClassName());
    }

    private void verifyRefreshState(boolean isSet, boolean allMatch, int expectedState) {
        reset(mProps, mTile);

        mProps.isSet = isSet;
        mProps.allMatch = allMatch;
        mDevelopmentModeTile.refresh();

        verify(mProps).refreshState(eq(mDevelopmentModeTile));
        verify(mTile).setState(eq(expectedState));
    }
}
