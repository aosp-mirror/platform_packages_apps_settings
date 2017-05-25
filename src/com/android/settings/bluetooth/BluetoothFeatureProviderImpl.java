package com.android.settings.bluetooth;

/**
 * Impl for bluetooth feature provider
 */
public class BluetoothFeatureProviderImpl implements BluetoothFeatureProvider {

    @Override
    public boolean isPairingPageEnabled() {
        return false;
    }

    @Override
    public boolean isDeviceDetailPageEnabled() {
        return false;
    }
}
