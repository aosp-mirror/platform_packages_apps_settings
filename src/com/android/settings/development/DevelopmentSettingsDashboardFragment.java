/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.development;

import android.app.Activity;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.development.DevelopmentSettingsEnabler;
import com.android.settingslib.development.SystemPropPoker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DevelopmentSettingsDashboardFragment extends RestrictedDashboardFragment
        implements SwitchBar.OnSwitchChangeListener, OemUnlockDialogHost, AdbDialogHost,
        AdbClearKeysDialogHost, LogPersistDialogHost {

    private static final String TAG = "DevSettingsDashboard";

    private final Object mBluetoothA2dpLock = new Object();

    private boolean mIsAvailable = true;
    private SwitchBar mSwitchBar;
    private DevelopmentSwitchBarController mSwitchBarController;
    private List<AbstractPreferenceController> mPreferenceControllers = new ArrayList<>();
    private BluetoothA2dp mBluetoothA2dp;

    private final BroadcastReceiver mEnableAdbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            for (AbstractPreferenceController controller : mPreferenceControllers) {
                if (controller instanceof AdbOnChangeListener) {
                    ((AdbOnChangeListener) controller).onAdbSettingChanged();
                }
            }
        }
    };

    private final BroadcastReceiver mBluetoothA2dpReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "mBluetoothA2dpReceiver.onReceive intent=" + intent);
            String action = intent.getAction();

            if (BluetoothA2dp.ACTION_CODEC_CONFIG_CHANGED.equals(action)) {
                BluetoothCodecStatus codecStatus = intent.getParcelableExtra(
                        BluetoothCodecStatus.EXTRA_CODEC_STATUS);
                Log.d(TAG, "Received BluetoothCodecStatus=" + codecStatus);
                for (AbstractPreferenceController controller : mPreferenceControllers) {
                    if (controller instanceof BluetoothServiceConnectionListener) {
                        ((BluetoothServiceConnectionListener) controller).onBluetoothCodecUpdated();
                    }
                }
            }
        }
    };


    private final BluetoothProfile.ServiceListener mBluetoothA2dpServiceListener =
            new BluetoothProfile.ServiceListener() {
                @Override
                public void onServiceConnected(int profile,
                        BluetoothProfile proxy) {
                    synchronized (mBluetoothA2dpLock) {
                        mBluetoothA2dp = (BluetoothA2dp) proxy;
                    }
                    for (AbstractPreferenceController controller : mPreferenceControllers) {
                        if (controller instanceof BluetoothServiceConnectionListener) {
                            ((BluetoothServiceConnectionListener) controller)
                                    .onBluetoothServiceConnected(mBluetoothA2dp);
                        }
                    }
                }

                @Override
                public void onServiceDisconnected(int profile) {
                    synchronized (mBluetoothA2dpLock) {
                        mBluetoothA2dp = null;
                    }
                    for (AbstractPreferenceController controller : mPreferenceControllers) {
                        if (controller instanceof BluetoothServiceConnectionListener) {
                            ((BluetoothServiceConnectionListener) controller)
                                    .onBluetoothServiceDisconnected();
                        }
                    }
                }
            };

    public DevelopmentSettingsDashboardFragment() {
        super(UserManager.DISALLOW_DEBUGGING_FEATURES);
    }

    @Override
    public void onActivityCreated(Bundle icicle) {
        super.onActivityCreated(icicle);
        // Apply page-level restrictions
        setIfOnlyAvailableForAdmins(true);
        if (isUiRestricted() || !Utils.isDeviceProvisioned(getActivity())) {
            // Block access to developer options if the user is not the owner, if user policy
            // restricts it, or if the device has not been provisioned
            mIsAvailable = false;
            // Show error message
            if (!isUiRestrictedByOnlyAdmin()) {
                getEmptyTextView().setText(R.string.development_settings_not_available);
            }
            getPreferenceScreen().removeAll();
            return;
        }
        // Set up master switch
        mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        mSwitchBarController = new DevelopmentSwitchBarController(
                this /* DevelopmentSettings */, mSwitchBar, mIsAvailable, getLifecycle());
        mSwitchBar.show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        registerReceivers();

        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            adapter.getProfileProxy(getActivity(), mBluetoothA2dpServiceListener,
                    BluetoothProfile.A2DP);
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unregisterReceivers();

        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            adapter.closeProfileProxy(BluetoothProfile.A2DP, mBluetoothA2dp);
            mBluetoothA2dp = null;
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DEVELOPMENT;
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (switchView != mSwitchBar.getSwitch()) {
            return;
        }
        final boolean developmentEnabledState =
                DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(getContext());
        if (isChecked != developmentEnabledState) {
            if (isChecked) {
                EnableDevelopmentSettingWarningDialog.show(this /* host */);
            } else {
                DevelopmentSettingsEnabler.setDevelopmentSettingsEnabled(getContext(), false);
                final SystemPropPoker poker = SystemPropPoker.getInstance();
                poker.blockPokes();
                for (AbstractPreferenceController controller : mPreferenceControllers) {
                    if (controller instanceof DeveloperOptionsPreferenceController) {
                        ((DeveloperOptionsPreferenceController) controller)
                                .onDeveloperOptionsDisabled();
                    }
                }
                poker.unblockPokes();
                poker.poke();
            }
        }
    }

    @Override
    public void onOemUnlockDialogConfirmed() {
        final OemUnlockPreferenceController controller = getDevelopmentOptionsController(
                OemUnlockPreferenceController.class);
        controller.onOemUnlockConfirmed();
    }

    @Override
    public void onOemUnlockDialogDismissed() {
        final OemUnlockPreferenceController controller = getDevelopmentOptionsController(
                OemUnlockPreferenceController.class);
        controller.onOemUnlockDismissed();
    }

    @Override
    public void onEnableAdbDialogConfirmed() {
        final AdbPreferenceController controller = getDevelopmentOptionsController(
                AdbPreferenceController.class);
        controller.onAdbDialogConfirmed();

    }

    @Override
    public void onEnableAdbDialogDismissed() {
        final AdbPreferenceController controller = getDevelopmentOptionsController(
                AdbPreferenceController.class);
        controller.onAdbDialogDismissed();
    }

    @Override
    public void onAdbClearKeysDialogConfirmed() {
        final ClearAdbKeysPreferenceController controller = getDevelopmentOptionsController(
                ClearAdbKeysPreferenceController.class);
        controller.onClearAdbKeysConfirmed();
    }

    @Override
    public void onDisableLogPersistDialogConfirmed() {
        final LogPersistPreferenceControllerV2 controller = getDevelopmentOptionsController(
                LogPersistPreferenceControllerV2.class);
        controller.onDisableLogPersistDialogConfirmed();
    }

    @Override
    public void onDisableLogPersistDialogRejected() {
        final LogPersistPreferenceControllerV2 controller = getDevelopmentOptionsController(
                LogPersistPreferenceControllerV2.class);
        controller.onDisableLogPersistDialogRejected();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        boolean handledResult = false;
        for (AbstractPreferenceController controller : mPreferenceControllers) {
            if (controller instanceof OnActivityResultListener) {
                // We do not break early because it is possible for multiple controllers to
                // handle the same result code.
                handledResult |=
                        ((OnActivityResultListener) controller).onActivityResult(
                                requestCode, resultCode, data);
            }
        }
        if (!handledResult) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getHelpResource() {
        return 0;
    }

    @Override
    protected int getPreferenceScreenResId() {
        Log.d(TAG, "Creating pref screen");
        return R.xml.development_prefs;
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        mPreferenceControllers = buildPreferenceControllers(context, getActivity(), getLifecycle(),
                this /* devOptionsDashboardFragment */, mBluetoothA2dpLock,
                new BluetoothA2dpConfigStore());
        return mPreferenceControllers;
    }

    private void registerReceivers() {
        LocalBroadcastManager.getInstance(getContext())
                .registerReceiver(mEnableAdbReceiver, new IntentFilter(
                        AdbPreferenceController.ACTION_ENABLE_ADB_STATE_CHANGED));

        final IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothA2dp.ACTION_CODEC_CONFIG_CHANGED);
        getActivity().registerReceiver(mBluetoothA2dpReceiver, filter);
    }

    private void unregisterReceivers() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mEnableAdbReceiver);
        getActivity().unregisterReceiver(mBluetoothA2dpReceiver);
    }

    void onEnableDevelopmentOptionsConfirmed() {
        DevelopmentSettingsEnabler.setDevelopmentSettingsEnabled(getContext(), true);
        for (AbstractPreferenceController controller : mPreferenceControllers) {
            if (controller instanceof DeveloperOptionsPreferenceController) {
                ((DeveloperOptionsPreferenceController) controller).onDeveloperOptionsEnabled();
            }
        }
    }

    void onEnableDevelopmentOptionsRejected() {
        // Reset the toggle
        mSwitchBar.setChecked(false);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Activity activity, Lifecycle lifecycle, DevelopmentSettingsDashboardFragment fragment,
            Object bluetoothA2dpLock, BluetoothA2dpConfigStore bluetoothA2dpConfigStore) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new BugReportPreferenceControllerV2(context));
        controllers.add(new LocalBackupPasswordPreferenceController(context));
        controllers.add(new StayAwakePreferenceController(context, lifecycle));
        controllers.add(new HdcpCheckingPreferenceController(context));
        controllers.add(new BluetoothSnoopLogPreferenceController(context));
        controllers.add(new OemUnlockPreferenceController(context, activity, fragment));
        // running services
        controllers.add(new FileEncryptionPreferenceController(context));
        controllers.add(new PictureColorModePreferenceController(context, lifecycle));
        controllers.add(new WebViewAppPreferenceControllerV2(context));
        controllers.add(new CoolColorTemperaturePreferenceController(context));
        controllers.add(new DisableAutomaticUpdatesPreferenceController(context));
        // system ui demo mode
        // quick settings developer tiles
        controllers.add(new AdbPreferenceController(context, fragment));
        controllers.add(new ClearAdbKeysPreferenceController(context, fragment));
        controllers.add(new LocalTerminalPreferenceController(context));
        controllers.add(new BugReportInPowerPreferenceControllerV2(context));
        controllers.add(new MockLocationAppPreferenceController(context, fragment));
        controllers.add(new DebugViewAttributesPreferenceController(context));
        controllers.add(new SelectDebugAppPreferenceController(context, fragment));
        controllers.add(new WaitForDebuggerPreferenceController(context));
        controllers.add(new VerifyAppsOverUsbPreferenceControllerV2(context));
        controllers.add(new LogdSizePreferenceControllerV2(context));
        controllers.add(new LogPersistPreferenceControllerV2(context, fragment, lifecycle));
        controllers.add(new ConnectivityMonitorPreferenceControllerV2(context));
        controllers.add(new CameraLaserSensorPreferenceControllerV2(context));
        controllers.add(new CameraHalHdrPlusPreferenceControllerV2(context));
        // feature flags
        controllers.add(new WifiDisplayCertificationPreferenceController(context));
        controllers.add(new WifiVerboseLoggingPreferenceController(context));
        controllers.add(new WifiAggressiveHandoverPreferenceController(context));
        controllers.add(new WifiRoamScansPreferenceController(context));
        controllers.add(new MobileDataAlwaysOnPreferenceController(context));
        controllers.add(new TetheringHardwareAccelPreferenceController(context));
        controllers.add(new SelectUsbConfigPreferenceController(context, lifecycle));
        controllers.add(new BluetoothDeviceNoNamePreferenceController(context));
        controllers.add(new BluetoothAbsoluteVolumePreferenceController(context));
        controllers.add(new BluetoothInbandRingingPreferenceController(context));
        controllers.add(new BluetoothAvrcpVersionPreferenceController(context));
        //controllers.add(new BluetoothAudioCodecPreferenceController(context, lifecycle,
        //        bluetoothA2dpLock, bluetoothA2dpConfigStore));
        controllers.add(new BluetoothAudioSampleRatePreferenceController(context, lifecycle,
                bluetoothA2dpLock, bluetoothA2dpConfigStore));
        // bluetooth audio bits per sample
        // bluetooth audio channel mode
        // bluetooth audio ldac codec: playback quality
        controllers.add(new ShowTapsPreferenceController(context));
        controllers.add(new PointerLocationPreferenceController(context));
        controllers.add(new ShowSurfaceUpdatesPreferenceController(context));
        controllers.add(new ShowLayoutBoundsPreferenceController(context));
        controllers.add(new RtlLayoutPreferenceController(context));
        controllers.add(new WindowAnimationScalePreferenceController(context));
        controllers.add(new TransitionAnimationScalePreferenceController(context));
        controllers.add(new AnimatorDurationScalePreferenceController(context));
        controllers.add(new SecondaryDisplayPreferenceController(context));
        // smallest width
        controllers.add(new ForceGpuRenderingPreferenceController(context));
        controllers.add(new GpuViewUpdatesPreferenceController(context));
        controllers.add(new HardwareLayersUpdatesPreferenceController(context));
        controllers.add(new DebugGpuOverdrawPreferenceController(context));
        controllers.add(new DebugNonRectClipOperationsPreferenceController(context));
        controllers.add(new ForceMSAAPreferenceController(context));
        controllers.add(new HardwareOverlaysPreferenceController(context));
        controllers.add(new SimulateColorSpacePreferenceController(context));
        controllers.add(new SetGpuRendererPreferenceController(context));
        controllers.add(new UsbAudioRoutingPreferenceController(context));
        controllers.add(new StrictModePreferenceController(context));
        // profile gpu rendering
        controllers.add(new KeepActivitiesPreferenceController(context));
        // background process limit
        // background check
        controllers.add(new AppsNotRespondingPreferenceController(context));
        controllers.add(new NotificationChannelWarningsPreferenceController(context));
        // inactive apps
        controllers.add(new AllowAppsOnExternalPreferenceController(context));
        controllers.add(new ResizableActivityPreferenceController(context));
        controllers.add(new ShortcutManagerThrottlingPreferenceController(context));
        return controllers;
    }

    @VisibleForTesting
    <T extends AbstractPreferenceController> T getDevelopmentOptionsController(Class<T> clazz) {
        return getPreferenceController(clazz);
    }

    /**
     * For Search.
     */
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(context);
                }

                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {

                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.development_prefs;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> getPreferenceControllers(Context
                        context) {
                    return buildPreferenceControllers(context, null /* activity */,
                            null /* lifecycle */, null /* devOptionsDashboardFragment */,
                            null /* bluetoothA2dpLock */, null /* bluetoothA2dpConfigStore */);
                }
            };
}
