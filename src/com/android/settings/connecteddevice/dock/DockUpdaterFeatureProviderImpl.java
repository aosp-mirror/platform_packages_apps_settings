package com.android.settings.connecteddevice.dock;

import android.content.Context;

import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.overlay.DockUpdaterFeatureProvider;

/**
 * Impl for {@link DockUpdaterFeatureProvider}
 */
public class DockUpdaterFeatureProviderImpl implements DockUpdaterFeatureProvider {

    @Override
    public DockUpdater getConnectedDockUpdater(Context context,
            DevicePreferenceCallback devicePreferenceCallback) {
        final DockUpdater updater = new DockUpdater() {
        };
        return updater;
    }

    @Override
    public DockUpdater getSavedDockUpdater(Context context,
            DevicePreferenceCallback devicePreferenceCallback) {
        final DockUpdater updater = new DockUpdater() {
        };
        return updater;
    }
}
