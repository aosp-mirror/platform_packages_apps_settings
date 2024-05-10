package com.android.settings.wifi.tether;

import android.net.wifi.WifiClient;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerExecutor;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Wrapper for {@link android.net.wifi.WifiManager.SoftApCallback} to pass the robo test
 */
public class WifiTetherSoftApManager {

    private WifiManager mWifiManager;
    private WifiTetherSoftApCallback mWifiTetherSoftApCallback;

    private WifiManager.SoftApCallback mSoftApCallback;
    private Handler mHandler;

    WifiTetherSoftApManager(WifiManager wifiManager,
            WifiTetherSoftApCallback wifiTetherSoftApCallback) {
        mWifiManager = wifiManager;
        mWifiTetherSoftApCallback = wifiTetherSoftApCallback;
        mSoftApCallback = new WifiManagerSoftApCallback(this);
        mHandler = new Handler();
    }

    public void registerSoftApCallback() {
        mWifiManager.registerSoftApCallback(new HandlerExecutor(mHandler), mSoftApCallback);
    }

    public void unRegisterSoftApCallback() {
        mWifiManager.unregisterSoftApCallback(mSoftApCallback);
    }

    void onStateChanged(int state, int failureReason) {
        mWifiTetherSoftApCallback.onStateChanged(state, failureReason);
    }

    void onConnectedClientsChanged(List<WifiClient> clients) {
        mWifiTetherSoftApCallback.onConnectedClientsChanged(clients);
    }

    public interface WifiTetherSoftApCallback {
        void onStateChanged(int state, int failureReason);

        /**
         * Called when the connected clients to soft AP changes.
         *
         * @param clients the currently connected clients
         */
        void onConnectedClientsChanged(List<WifiClient> clients);
    }

    // TODO(b/246537032):Need to declare the service callback as static class and use
    //  WeakReference to avoid the callback link being occupied by WifiManager
    private static final class WifiManagerSoftApCallback implements
            WifiManager.SoftApCallback {
        WeakReference<WifiTetherSoftApManager> mMyClass;

        WifiManagerSoftApCallback(WifiTetherSoftApManager controller) {
            mMyClass = new WeakReference<>(controller);
        }

        @Override
        public void onStateChanged(int state, int failureReason) {
            WifiTetherSoftApManager controller = mMyClass.get();
            if (controller == null) return;

            controller.onStateChanged(state, failureReason);
        }

        @Override
        public void onConnectedClientsChanged(List<WifiClient> clients) {
            WifiTetherSoftApManager controller = mMyClass.get();
            if (controller == null) return;

            controller.onConnectedClientsChanged(clients);
        }
    }
}
