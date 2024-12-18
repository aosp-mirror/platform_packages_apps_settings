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

package com.android.settings.bluetooth;

import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static android.os.UserManager.DISALLOW_CONFIG_BLUETOOTH;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.FeatureFlagUtils;
import android.util.Log;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.bluetooth.ui.model.FragmentTypeModel;
import com.android.settings.bluetooth.ui.view.DeviceDetailsFragmentFormatter;
import com.android.settings.connecteddevice.stylus.StylusDevicesController;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settings.flags.Flags;
import com.android.settings.inputmethod.KeyboardSettingsPreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.slices.SlicePreferenceController;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BluetoothDeviceDetailsFragment extends RestrictedDashboardFragment {
    public static final String KEY_DEVICE_ADDRESS = "device_address";
    private static final String TAG = "BTDeviceDetailsFrg";
    private static final int METADATA_FAST_PAIR_CUSTOMIZED_FIELDS = 25;

    @VisibleForTesting
    static int EDIT_DEVICE_NAME_ITEM_ID = Menu.FIRST;

    /**
     * An interface to let tests override the normal mechanism for looking up the
     * CachedBluetoothDevice and LocalBluetoothManager, and substitute their own mocks instead.
     * This is only needed in situations where you instantiate the fragment indirectly (eg via an
     * intent) and can't use something like spying on an instance you construct directly via
     * newInstance.
     */
    @VisibleForTesting
    interface TestDataFactory {
        CachedBluetoothDevice getDevice(String deviceAddress);

        LocalBluetoothManager getManager(Context context);

        UserManager getUserManager();
    }

    @VisibleForTesting
    static TestDataFactory sTestDataFactory;

    @VisibleForTesting
    String mDeviceAddress;
    @VisibleForTesting
    LocalBluetoothManager mManager;
    @VisibleForTesting
    CachedBluetoothDevice mCachedDevice;
    BluetoothAdapter mBluetoothAdapter;
    @VisibleForTesting
    DeviceDetailsFragmentFormatter mFormatter;

    @Nullable
    InputDevice mInputDevice;

    private UserManager mUserManager;
    int mExtraControlViewWidth = 0;
    boolean mExtraControlUriLoaded = false;

    private final BluetoothCallback mBluetoothCallback =
            new BluetoothCallback() {
                @Override
                public void onBluetoothStateChanged(int bluetoothState) {
                    if (bluetoothState == BluetoothAdapter.STATE_OFF) {
                        Log.i(TAG, "Bluetooth is off, exit activity.");
                        Activity activity = getActivity();
                        if (activity != null) {
                            activity.finish();
                        }
                    }
                }
            };

    private final BluetoothAdapter.OnMetadataChangedListener mExtraControlMetadataListener =
            (device, key, value) -> {
                if (key == METADATA_FAST_PAIR_CUSTOMIZED_FIELDS
                        && mExtraControlViewWidth > 0
                        && !mExtraControlUriLoaded) {
                    Log.i(TAG, "Update extra control UI because of metadata change.");
                    updateExtraControlUri(mExtraControlViewWidth);
                }
            };

    public BluetoothDeviceDetailsFragment() {
        super(DISALLOW_CONFIG_BLUETOOTH);
    }

    @VisibleForTesting
    LocalBluetoothManager getLocalBluetoothManager(Context context) {
        if (sTestDataFactory != null) {
            return sTestDataFactory.getManager(context);
        }
        return Utils.getLocalBtManager(context);
    }

    @VisibleForTesting
    @Nullable
    CachedBluetoothDevice getCachedDevice(String deviceAddress) {
        if (sTestDataFactory != null) {
            return sTestDataFactory.getDevice(deviceAddress);
        }
        BluetoothDevice remoteDevice =
                mManager.getBluetoothAdapter().getRemoteDevice(deviceAddress);
        if (remoteDevice == null) {
            return null;
        }
        CachedBluetoothDevice cachedDevice =
                mManager.getCachedDeviceManager().findDevice(remoteDevice);
        if (cachedDevice != null) {
            return cachedDevice;
        }
        Log.i(TAG, "Add device to cached device manager: " + remoteDevice.getAnonymizedAddress());
        return mManager.getCachedDeviceManager().addDevice(remoteDevice);
    }

    @VisibleForTesting
    UserManager getUserManager() {
        if (sTestDataFactory != null) {
            return sTestDataFactory.getUserManager();
        }

        return getSystemService(UserManager.class);
    }

    @Nullable
    @VisibleForTesting
    InputDevice getInputDevice(Context context) {
        InputManager im = context.getSystemService(InputManager.class);

        for (int deviceId : im.getInputDeviceIds()) {
            String btAddress = im.getInputDeviceBluetoothAddress(deviceId);

            if (btAddress != null && btAddress.equals(mDeviceAddress)) {
                return im.getInputDevice(deviceId);
            }
        }
        return null;
    }

    public static BluetoothDeviceDetailsFragment newInstance(String deviceAddress) {
        Bundle args = new Bundle(1);
        args.putString(KEY_DEVICE_ADDRESS, deviceAddress);
        BluetoothDeviceDetailsFragment fragment = new BluetoothDeviceDetailsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        mDeviceAddress = getArguments().getString(KEY_DEVICE_ADDRESS);
        mManager = getLocalBluetoothManager(context);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mCachedDevice = getCachedDevice(mDeviceAddress);
        mUserManager = getUserManager();

        if (FeatureFlagUtils.isEnabled(context,
                FeatureFlagUtils.SETTINGS_SHOW_STYLUS_PREFERENCES)) {
            mInputDevice = getInputDevice(context);
        }

        super.onAttach(context);
        if (mCachedDevice == null) {
            // Close this page if device is null with invalid device mac address
            Log.w(TAG, "onAttach() CachedDevice is null!");
            finish();
            return;
        }
        getController(
                AdvancedBluetoothDetailsHeaderController.class,
                controller -> controller.init(mCachedDevice, this));
        getController(
                LeAudioBluetoothDetailsHeaderController.class,
                controller -> controller.init(mCachedDevice, mManager, this));
        getController(
                KeyboardSettingsPreferenceController.class,
                controller -> controller.init(mCachedDevice));

        final BluetoothFeatureProvider featureProvider =
                FeatureFactory.getFeatureFactory().getBluetoothFeatureProvider();

        getController(
                BlockingPrefWithSliceController.class,
                controller ->
                        controller.setSliceUri(
                                featureProvider.getBluetoothDeviceSettingsUri(
                                        mCachedDevice.getDevice())));

        mManager.getEventManager().registerCallback(mBluetoothCallback);
        mBluetoothAdapter.addOnMetadataChangedListener(
                mCachedDevice.getDevice(),
                context.getMainExecutor(),
                mExtraControlMetadataListener);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mManager.getEventManager().unregisterCallback(mBluetoothCallback);
        BluetoothDevice device = mCachedDevice.getDevice();
        try {
            mBluetoothAdapter.removeOnMetadataChangedListener(
                    device, mExtraControlMetadataListener);
        } catch (IllegalArgumentException e) {
            Log.w(
                    TAG,
                    "Unable to unregister metadata change callback for "
                            + mCachedDevice,
                    e);
        }
    }

    private void updateExtraControlUri(int viewWidth) {
        BluetoothFeatureProvider featureProvider =
                FeatureFactory.getFeatureFactory().getBluetoothFeatureProvider();
        Uri controlUri = null;
        String uri = featureProvider.getBluetoothDeviceControlUri(mCachedDevice.getDevice());
        if (!TextUtils.isEmpty(uri)) {
            try {
                controlUri = Uri.parse(uri + viewWidth);
            } catch (NullPointerException exception) {
                Log.d(TAG, "unable to parse uri");
            }
        }
        mExtraControlUriLoaded |= controlUri != null;

        Uri finalControlUri = controlUri;
        getController(
                SlicePreferenceController.class,
                controller -> {
                    controller.setSliceUri(finalControlUri);
                    controller.onStart();
                    controller.displayPreference(getPreferenceScreen());
                });

        // Temporarily fix the issue that the page will be automatically scrolled to a wrong
        // position when entering the page. This will make sure the bluetooth header is shown on top
        // of the page.
        getController(
                LeAudioBluetoothDetailsHeaderController.class,
                controller -> controller.displayPreference(getPreferenceScreen()));
        getController(
                AdvancedBluetoothDetailsHeaderController.class,
                controller -> controller.displayPreference(getPreferenceScreen()));
        getController(
                BluetoothDetailsHeaderController.class,
                controller -> controller.displayPreference(getPreferenceScreen()));
    }

    protected <T extends AbstractPreferenceController> void getController(Class<T> clazz,
            Consumer<T> action) {
        T controller = use(clazz);
        if (controller != null) {
            action.accept(controller);
        }
    }

    private final ViewTreeObserver.OnGlobalLayoutListener mOnGlobalLayoutListener =
            new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    View view = getView();
                    if (view == null) {
                        return;
                    }
                    if (view.getWidth() <= 0) {
                        return;
                    }
                    mExtraControlViewWidth = view.getWidth() - getPaddingSize();
                    updateExtraControlUri(mExtraControlViewWidth);
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(
                            mOnGlobalLayoutListener);
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitleForInputDevice();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view != null) {
            view.getViewTreeObserver().addOnGlobalLayoutListener(mOnGlobalLayoutListener);
        }
        return view;
    }

    @Override
    public void onCreatePreferences(@NonNull Bundle savedInstanceState, @NonNull String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        if (Flags.enableBluetoothDeviceDetailsPolish()) {
            mFormatter.updateLayout(FragmentTypeModel.DeviceDetailsMainFragment.INSTANCE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        finishFragmentIfNecessary();
    }

    @VisibleForTesting
    void finishFragmentIfNecessary() {
        if (mCachedDevice.getBondState() == BOND_NONE) {
            finish();
            return;
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.BLUETOOTH_DEVICE_DETAILS;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.bluetooth_device_details_fragment;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!Flags.enableBluetoothDeviceDetailsPolish() && !mUserManager.isGuestUser()) {
            MenuItem item = menu.add(0, EDIT_DEVICE_NAME_ITEM_ID, 0,
                    R.string.bluetooth_rename_button);
            item.setIcon(com.android.internal.R.drawable.ic_mode_edit);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == EDIT_DEVICE_NAME_ITEM_ID) {
            RemoteDeviceNameDialogFragment.newInstance(mCachedDevice).show(
                    getFragmentManager(), RemoteDeviceNameDialogFragment.TAG);
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected void addPreferenceController(AbstractPreferenceController controller) {
        if (Flags.enableBluetoothDeviceDetailsPolish()) {
            List<String> keys =
                    mFormatter.getVisiblePreferenceKeys(
                            FragmentTypeModel.DeviceDetailsMainFragment.INSTANCE);
            Lifecycle lifecycle = getSettingsLifecycle();
            if (keys == null || keys.contains(controller.getPreferenceKey())) {
                super.addPreferenceController(controller);
            } else if (controller instanceof LifecycleObserver) {
                lifecycle.removeObserver((LifecycleObserver) controller);
            }
        } else {
            super.addPreferenceController(controller);
        }
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        List<String> invisibleProfiles = List.of();
        if (Flags.enableBluetoothDeviceDetailsPolish()) {
            mFormatter =
                    FeatureFactory.getFeatureFactory()
                            .getBluetoothFeatureProvider()
                            .getDeviceDetailsFragmentFormatter(
                                    requireContext(), this, mBluetoothAdapter, mCachedDevice);
            invisibleProfiles =
                    mFormatter.getInvisibleBluetoothProfiles(
                            FragmentTypeModel.DeviceDetailsMainFragment.INSTANCE);
        }
        ArrayList<AbstractPreferenceController> controllers = new ArrayList<>();

        if (mCachedDevice != null) {
            Lifecycle lifecycle = getSettingsLifecycle();
            controllers.add(new BluetoothDetailsHeaderController(context, this, mCachedDevice,
                    lifecycle));
            controllers.add(
                    new GeneralBluetoothDetailsHeaderController(
                            context, this, mCachedDevice, lifecycle));
            controllers.add(new BluetoothDetailsButtonsController(context, this, mCachedDevice,
                    lifecycle));
            controllers.add(new BluetoothDetailsCompanionAppsController(context, this,
                    mCachedDevice, lifecycle));
            controllers.add(new BluetoothDetailsAudioDeviceTypeController(context, this, mManager,
                    mCachedDevice, lifecycle));
            controllers.add(new BluetoothDetailsSpatialAudioController(context, this, mCachedDevice,
                    lifecycle));
            controllers.add(new BluetoothDetailsProfilesController(context, this, mManager,
                    mCachedDevice, lifecycle, invisibleProfiles));
            controllers.add(new BluetoothDetailsMacAddressController(context, this, mCachedDevice,
                    lifecycle));
            controllers.add(new StylusDevicesController(context, mInputDevice, mCachedDevice,
                    lifecycle));
            controllers.add(new BluetoothDetailsRelatedToolsController(context, this, mCachedDevice,
                    lifecycle));
            controllers.add(new BluetoothDetailsPairOtherController(context, this, mCachedDevice,
                    lifecycle));
            controllers.add(new BluetoothDetailsDataSyncController(context, this, mCachedDevice,
                    lifecycle));
            controllers.add(new BluetoothDetailsExtraOptionsController(context, this, mCachedDevice,
                    lifecycle));
            BluetoothDetailsHearingDeviceController hearingDeviceController =
                    new BluetoothDetailsHearingDeviceController(context, this, mManager,
                            mCachedDevice, lifecycle);
            controllers.add(hearingDeviceController);
            hearingDeviceController.initSubControllers(isLaunchFromHearingDevicePage());
            controllers.addAll(hearingDeviceController.getSubControllers());
        }
        return controllers;
    }

    private int getPaddingSize() {
        TypedArray resolvedAttributes =
                getContext().obtainStyledAttributes(
                        new int[]{
                                android.R.attr.listPreferredItemPaddingStart,
                                android.R.attr.listPreferredItemPaddingEnd
                        });
        int width = resolvedAttributes.getDimensionPixelSize(0, 0)
                + resolvedAttributes.getDimensionPixelSize(1, 0);
        resolvedAttributes.recycle();
        return width;
    }

    private boolean isLaunchFromHearingDevicePage() {
        final Intent intent = getIntent();
        if (intent == null) {
            return false;
        }

        return intent.getIntExtra(MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY,
                SettingsEnums.PAGE_UNKNOWN) == SettingsEnums.ACCESSIBILITY_HEARING_AID_SETTINGS;
    }

    @VisibleForTesting
    void setTitleForInputDevice() {
        if (StylusDevicesController.isDeviceStylus(mInputDevice, mCachedDevice)) {
            // This will override the default R.string.device_details_title "Device Details"
            // that will show on non-stylus bluetooth devices.
            // That title is set via the manifest and also from BluetoothDeviceUpdater.
            getActivity().setTitle(getContext().getString(R.string.stylus_device_details_title));
        }
    }
}
