package com.android.settings.testutils.shadow;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadows.ShadowService;

/**
 * Shadow for {@link TileService}.
 */
@Implements(TileService.class)
public class ShadowTileService extends ShadowService {

    @RealObject TileService realService;

    private Tile mTile;

    public void __constructor__() { }

    @Implementation
    public final Tile getQsTile() {
        return mTile;
    }

    @Implementation
    public final void startActivityAndCollapse(Intent intent) {
        realService.startActivity(intent);
    }

    // Non-Android setter.
    public void setTile(Tile tile) {
        mTile = tile;
    }
}
