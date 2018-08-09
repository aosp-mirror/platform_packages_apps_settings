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

import static android.service.quicksettings.TileService.ACTION_QS_TILE_PREFERENCES;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import androidx.annotation.VisibleForTesting;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settings.development.autofill.AutofillCategoryController;
import com.android.settings.development.autofill.AutofillLoggingLevelPreferenceController;
import com.android.settings.development.autofill.AutofillResetOptionsPreferenceController;
import com.android.settings.development.bluetooth.AbstractBluetoothDialogPreferenceController;
import com.android.settings.development.bluetooth.AbstractBluetoothPreferenceController;
import com.android.settings.development.bluetooth.BluetoothBitPerSampleDialogPreferenceController;
import com.android.settings.development.bluetooth.BluetoothChannelModeDialogPreferenceController;
import com.android.settings.development.bluetooth.BluetoothCodecDialogPreferenceController;
import com.android.settings.development.bluetooth.BluetoothHDAudioPreferenceController;
import com.android.settings.development.bluetooth.BluetoothQualityDialogPreferenceController;
import com.android.settings.development.bluetooth.BluetoothSampleRateDialogPreferenceController;
import com.android.settings.development.qstile.DevelopmentTiles;
import com.android.settings.development.storage.SharedDataPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.actionbar.SearchMenuController;
import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.development.DevelopmentSettingsEnabler;
import com.android.settingslib.development.SystemPropPoker;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.OnMainSwitchChangeListener;

import com.google.android.setupcompat.util.WizardManagerHelper;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class DevelopmentSettingsDashboardFragment extends RestrictedDashboardFragment
        implements OnMainSwitchChangeListener, OemUnlockDialogHost, AdbDialogHost,
        AdbClearKeysDialogHost, LogPersistDialogHost,
        BluetoothHwOffloadRebootDialog.OnHwOffloadDialogListener,
        AbstractBluetoothPreferenceController.Callback {

    private static final String TAG = "DevSettingsDashboard";

    private final BluetoothA2dpConfigStore mBluetoothA2dpConfigStore =
            new BluetoothA2dpConfigStore();

    private boolean mIsAvailable = true;
    private SettingsMainSwitchBar mSwitchBar;
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
                    synchronized (mBluetoothA2dpConfigStore) {
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
                    synchronized (mBluetoothA2dpConfigStore) {
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

    private final Runnable mSystemPropertiesChanged = new Runnable() {
        @Override
        public void run() {
            synchronized (this) {
                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        updatePreferenceStates();
                    });
                }
            }
        }
    };

    public DevelopmentSettingsDashboardFragment() {
        super(UserManager.DISALLOW_DEBUGGING_FEATURES);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        SearchMenuController.init(this);
        if (Utils.isMonkeyRunning()) {
            getActivity().finish();
            return;
        }
    }

    @Override
    public void onActivityCreated(Bundle icicle) {
        super.onActivityCreated(icicle);
        // Apply page-level restrictions
        setIfOnlyAvailableForAdmins(true);
        if (isUiRestricted() || !WizardManagerHelper.isDeviceProvisioned(getActivity())) {
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
        // Set up primary switch
        mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        mSwitchBar.setTitle(getContext().getString(R.string.developer_options_main_switch_title));
        mSwitchBar.show();
        mSwitchBarController = new DevelopmentSwitchBarController(
                this /* DevelopmentSettings */, mSwitchBar, mIsAvailable,
                getSettingsLifecycle());

        // Restore UI state based on whether developer options is enabled
        if (DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(getContext())) {
            enableDeveloperOptions();
            handleQsTileLongPressActionIfAny();
        } else {
            disableDeveloperOptions();
        }
    }

    @Override
    protected boolean shouldSkipForInitialSUW() {
        return true;
    }

    /**
     * Long-pressing a developer options quick settings tile will by default (see
     * QS_TILE_PREFERENCES in the manifest) take you to the developer options page.
     * Some tiles may want to go into their own page within the developer options.
     */
    private void handleQsTileLongPressActionIfAny() {
        Intent intent = getActivity().getIntent();
        if (intent == null || !TextUtils.equals(ACTION_QS_TILE_PREFERENCES, intent.getAction())) {
            return;
        }

        Log.d(TAG, "Developer options started from qstile long-press");
        final ComponentName componentName = (ComponentName) intent.getParcelableExtra(
                Intent.EXTRA_COMPONENT_NAME);
        if (componentName == null) {
            return;
        }

        if (DevelopmentTiles.WirelessDebugging.class.getName().equals(
                componentName.getClassName()) && getDevelopmentOptionsController(
                WirelessDebuggingPreferenceController.class).isAvailable()) {
            Log.d(TAG, "Long press from wireless debugging qstile");
            new SubSettingLauncher(getContext())
                    .setDestination(WirelessDebuggingFragment.class.getName())
                    .setSourceMetricsCategory(SettingsEnums.SETTINGS_ADB_WIRELESS)
                    .launch();
        }
        // Add other qstiles here
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        registerReceivers();
        SystemProperties.addChangeCallback(mSystemPropertiesChanged);
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
        SystemProperties.removeChangeCallback(mSystemPropertiesChanged);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DEVELOPMENT;
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
                final BluetoothA2dpHwOffloadPreferenceController a2dpController =
                        getDevelopmentOptionsController(
                                BluetoothA2dpHwOffloadPreferenceController.class);
                final BluetoothLeAudioHwOffloadPreferenceController leAudioController =
                        getDevelopmentOptionsController(
                                BluetoothLeAudioHwOffloadPreferenceController.class);
                // If hardware offload isn't default value, we must reboot after disable
                // developer options. Show a dialog for the user to confirm.
                if ((a2dpController == null || a2dpController.isDefaultValue())
                        && (leAudioController == null || leAudioController.isDefaultValue())) {
                    disableDeveloperOptions();
                } else {
                    DisableDevSettingsDialogFragment.show(this /* host */);
                }
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
        final LogPersistPreferenceController controller = getDevelopmentOptionsController(
                LogPersistPreferenceController.class);
        controller.onDisableLogPersistDialogConfirmed();
    }

    @Override
    public void onDisableLogPersistDialogRejected() {
        final LogPersistPreferenceController controller = getDevelopmentOptionsController(
                LogPersistPreferenceController.class);
        controller.onDisableLogPersistDialogRejected();
    }

    @Override
    public void onHwOffloadDialogConfirmed() {
        final BluetoothA2dpHwOffloadPreferenceController a2dpController =
                getDevelopmentOptionsController(BluetoothA2dpHwOffloadPreferenceController.class);
        a2dpController.onHwOffloadDialogConfirmed();

        final BluetoothLeAudioHwOffloadPreferenceController leAudioController =
                getDevelopmentOptionsController(
                    BluetoothLeAudioHwOffloadPreferenceController.class);
        leAudioController.onHwOffloadDialogConfirmed();
    }

    @Override
    public void onHwOffloadDialogCanceled() {
        final BluetoothA2dpHwOffloadPreferenceController a2dpController =
                getDevelopmentOptionsController(BluetoothA2dpHwOffloadPreferenceController.class);
        a2dpController.onHwOffloadDialogCanceled();

        final BluetoothLeAudioHwOffloadPreferenceController leAudioController =
                getDevelopmentOptionsController(
                    BluetoothLeAudioHwOffloadPreferenceController.class);
        leAudioController.onHwOffloadDialogCanceled();
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
    public int getHelpResource() {
        return 0;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return Utils.isMonkeyRunning() ? R.xml.placeholder_prefs : R.xml.development_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        if (Utils.isMonkeyRunning()) {
            mPreferenceControllers = new ArrayList<>();
            return null;
        }
        mPreferenceControllers = buildPreferenceControllers(context, getActivity(),
                getSettingsLifecycle(), this /* devOptionsDashboardFragment */,
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

    private void enableDeveloperOptions() {
        if (Utils.isMonkeyRunning()) {
            return;
        }
        DevelopmentSettingsEnabler.setDevelopmentSettingsEnabled(getContext(), true);
        for (AbstractPreferenceController controller : mPreferenceControllers) {
            if (controller instanceof DeveloperOptionsPreferenceController) {
                ((DeveloperOptionsPreferenceController) controller).onDeveloperOptionsEnabled();
            }
        }
    }

    private void disableDeveloperOptions() {
        if (Utils.isMonkeyRunning()) {
            return;
        }
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

    void onEnableDevelopmentOptionsConfirmed() {
        enableDeveloperOptions();
    }

    void onEnableDevelopmentOptionsRejected() {
        // Reset the toggle
        mSwitchBar.setChecked(false);
    }

    void onDisableDevelopmentOptionsConfirmed() {
        disableDeveloperOptions();
    }

    void onDisableDevelopmentOptionsRejected() {
        // Reset the toggle
        mSwitchBar.setChecked(true);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Activity activity, Lifecycle lifecycle, DevelopmentSettingsDashboardFragment fragment,
            BluetoothA2dpConfigStore bluetoothA2dpConfigStore) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new MemoryUsagePreferenceController(context));
        controllers.add(new BugReportPreferenceController(context));
        controllers.add(new BugReportHandlerPreferenceController(context));
        controllers.add(new SystemServerHeapDumpPreferenceController(context));
        controllers.add(new RebootWithMtePreferenceController(context, fragment));
        controllers.add(new LocalBackupPasswordPreferenceController(context));
        controllers.add(new StayAwakePreferenceController(context, lifecycle));
        controllers.add(new HdcpCheckingPreferenceController(context));
        controllers.add(new BluetoothSnoopLogPreferenceController(context));
        controllers.add(new OemUnlockPreferenceController(context, activity, fragment));
        controllers.add(new PictureColorModePreferenceController(context, lifecycle));
        controllers.add(new WebViewAppPreferenceController(context));
        controllers.add(new CoolColorTemperaturePreferenceController(context));
     // controllers.add(new DisableAutomaticUpdatesPreferenceController(context));
        controllers.add(new SelectDSUPreferenceController(context));
        controllers.add(new AdbPreferenceController(context, fragment));
        controllers.add(new ClearAdbKeysPreferenceController(context, fragment));
        controllers.add(new WirelessDebuggingPreferenceController(context, lifecycle));
        controllers.add(new AdbAuthorizationTimeoutPreferenceController(context));
        controllers.add(new LocalTerminalPreferenceController(context));
        controllers.add(new BugReportInPowerPreferenceController(context));
        controllers.add(new AutomaticSystemServerHeapDumpPreferenceController(context));
        controllers.add(new MockLocationAppPreferenceController(context, fragment));
        controllers.add(new MockModemPreferenceController(context));
        controllers.add(new DebugViewAttributesPreferenceController(context));
        controllers.add(new SelectDebugAppPreferenceController(context, fragment));
        controllers.add(new WaitForDebuggerPreferenceController(context));
        controllers.add(new EnableGpuDebugLayersPreferenceController(context));
        controllers.add(new ForcePeakRefreshRatePreferenceController(context));
        controllers.add(new EnableVerboseVendorLoggingPreferenceController(context));
        controllers.add(new VerifyAppsOverUsbPreferenceController(context));
        controllers.add(new ArtVerifierPreferenceController(context));
        controllers.add(new LogdSizePreferenceController(context));
        controllers.add(new LogPersistPreferenceController(context, fragment, lifecycle));
        controllers.add(new CameraLaserSensorPreferenceController(context));
        controllers.add(new WifiDisplayCertificationPreferenceController(context));
        controllers.add(new WifiVerboseLoggingPreferenceController(context));
        controllers.add(new WifiScanThrottlingPreferenceController(context));
        controllers.add(new WifiNonPersistentMacRandomizationPreferenceController(context));
        controllers.add(new MobileDataAlwaysOnPreferenceController(context));
        controllers.add(new TetheringHardwareAccelPreferenceController(context));
        controllers.add(new BluetoothDeviceNoNamePreferenceController(context));
        controllers.add(new BluetoothAbsoluteVolumePreferenceController(context));
        controllers.add(new BluetoothAvrcpVersionPreferenceController(context));
        controllers.add(new BluetoothMapVersionPreferenceController(context));
        controllers.add(new BluetoothA2dpHwOffloadPreferenceController(context, fragment));
        controllers.add(new BluetoothLeAudioHwOffloadPreferenceController(context, fragment));
        controllers.add(new BluetoothMaxConnectedAudioDevicesPreferenceController(context));
        controllers.add(new NfcStackDebugLogPreferenceController(context));
        controllers.add(new ShowTapsPreferenceController(context));
        controllers.add(new PointerLocationPreferenceController(context));
        controllers.add(new ShowSurfaceUpdatesPreferenceController(context));
        controllers.add(new ShowLayoutBoundsPreferenceController(context));
        controllers.add(new ShowRefreshRatePreferenceController(context));
        controllers.add(new RtlLayoutPreferenceController(context));
        controllers.add(new WindowAnimationScalePreferenceController(context));
        controllers.add(new EmulateDisplayCutoutPreferenceController(context));
        controllers.add(new TransitionAnimationScalePreferenceController(context));
        controllers.add(new AnimatorDurationScalePreferenceController(context));
        controllers.add(new SecondaryDisplayPreferenceController(context));
        controllers.add(new GpuViewUpdatesPreferenceController(context));
        controllers.add(new HardwareLayersUpdatesPreferenceController(context));
        controllers.add(new DebugGpuOverdrawPreferenceController(context));
        controllers.add(new DebugNonRectClipOperationsPreferenceController(context));
        controllers.add(new ForceDarkPreferenceController(context));
        controllers.add(new EnableBlursPreferenceController(context));
        controllers.add(new ForceMSAAPreferenceController(context));
        controllers.add(new HardwareOverlaysPreferenceController(context));
        controllers.add(new SimulateColorSpacePreferenceController(context));
        controllers.add(new UsbAudioRoutingPreferenceController(context));
        controllers.add(new StrictModePreferenceController(context));
        controllers.add(new ProfileGpuRenderingPreferenceController(context));
        controllers.add(new KeepActivitiesPreferenceController(context));
        controllers.add(new BackgroundProcessLimitPreferenceController(context));
        controllers.add(new CachedAppsFreezerPreferenceController(context));
        controllers.add(new ShowFirstCrashDialogPreferenceController(context));
        controllers.add(new AppsNotRespondingPreferenceController(context));
        controllers.add(new NotificationChannelWarningsPreferenceController(context));
        controllers.add(new AllowAppsOnExternalPreferenceController(context));
        controllers.add(new ResizableActivityPreferenceController(context));
        controllers.add(new FreeformWindowsPreferenceController(context, fragment));
        controllers.add(new DesktopModePreferenceController(context, fragment));
        controllers.add(new NonResizableMultiWindowPreferenceController(context));
        controllers.add(new ShortcutManagerThrottlingPreferenceController(context));
        controllers.add(new EnableGnssRawMeasFullTrackingPreferenceController(context));
        controllers.add(new DefaultLaunchPreferenceController(context, "running_apps"));
        controllers.add(new DefaultLaunchPreferenceController(context, "demo_mode"));
        controllers.add(new DefaultLaunchPreferenceController(context, "quick_settings_tiles"));
        controllers.add(new DefaultLaunchPreferenceController(context, "feature_flags_dashboard"));
        controllers.add(new DefaultUsbConfigurationPreferenceController(context));
        controllers.add(new DefaultLaunchPreferenceController(context, "density"));
        controllers.add(new DefaultLaunchPreferenceController(context, "background_check"));
        controllers.add(new DefaultLaunchPreferenceController(context, "inactive_apps"));
        controllers.add(new AutofillCategoryController(context, lifecycle));
        controllers.add(new AutofillLoggingLevelPreferenceController(context, lifecycle));
        controllers.add(new AutofillResetOptionsPreferenceController(context));
        controllers.add(new BluetoothCodecDialogPreferenceController(context, lifecycle,
                bluetoothA2dpConfigStore, fragment));
        controllers.add(new BluetoothSampleRateDialogPreferenceController(context, lifecycle,
                bluetoothA2dpConfigStore));
        controllers.add(new BluetoothBitPerSampleDialogPreferenceController(context, lifecycle,
                bluetoothA2dpConfigStore));
        controllers.add(new BluetoothQualityDialogPreferenceController(context, lifecycle,
                bluetoothA2dpConfigStore));
        controllers.add(new BluetoothChannelModeDialogPreferenceController(context, lifecycle,
                bluetoothA2dpConfigStore));
        controllers.add(new BluetoothHDAudioPreferenceController(context, lifecycle,
                bluetoothA2dpConfigStore, fragment));
        controllers.add(new SharedDataPreferenceController(context));
        controllers.add(new OverlaySettingsPreferenceController(context));
        controllers.add(new StylusHandwritingPreferenceController(context));
        controllers.add(new IngressRateLimitPreferenceController((context)));
        controllers.add(new BackAnimationPreferenceController(context, fragment));

        return controllers;
    }

    @VisibleForTesting
    <T extends AbstractPreferenceController> T getDevelopmentOptionsController(Class<T> clazz) {
        return use(clazz);
    }

    @Override
    public void onBluetoothCodecChanged() {
        for (AbstractPreferenceController controller : mPreferenceControllers) {
            if (controller instanceof AbstractBluetoothDialogPreferenceController
                    && !(controller instanceof BluetoothCodecDialogPreferenceController)) {
                ((AbstractBluetoothDialogPreferenceController) controller)
                        .onBluetoothCodecUpdated();
            }
        }
    }

    @Override
    public void onBluetoothHDAudioEnabled(boolean enabled) {
        for (AbstractPreferenceController controller : mPreferenceControllers) {
            if (controller instanceof AbstractBluetoothDialogPreferenceController) {
                ((AbstractBluetoothDialogPreferenceController) controller).onHDAudioEnabled(
                        enabled);
            }
        }
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.development_settings) {

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(context);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(Context
                        context) {
                    return buildPreferenceControllers(context, null /* activity */,
                            null /* lifecycle */, null /* devOptionsDashboardFragment */,
                            null /* bluetoothA2dpConfigStore */);
                }
            };
}
