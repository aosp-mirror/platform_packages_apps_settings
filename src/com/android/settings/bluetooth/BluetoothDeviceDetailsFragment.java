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

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.res.TypedArray;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.DeviceConfig;
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

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.connecteddevice.stylus.StylusDevicesController;
import com.android.settings.core.SettingsUIDeviceConfig;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settings.inputmethod.KeyboardSettingsPreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.slices.SlicePreferenceController;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;

public class BluetoothDeviceDetailsFragment extends RestrictedDashboardFragment {
    public static final String KEY_DEVICE_ADDRESS = "device_address";
    private static final String TAG = "BTDeviceDetailsFrg";

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

    @Nullable
    InputDevice mInputDevice;

    private UserManager mUserManager;

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
    CachedBluetoothDevice getCachedDevice(String deviceAddress) {
        if (sTestDataFactory != null) {
            return sTestDataFactory.getDevice(deviceAddress);
        }
        BluetoothDevice remoteDevice =
                mManager.getBluetoothAdapter().getRemoteDevice(deviceAddress);
        return mManager.getCachedDeviceManager().findDevice(remoteDevice);
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
        use(AdvancedBluetoothDetailsHeaderController.class).init(mCachedDevice);
        use(LeAudioBluetoothDetailsHeaderController.class).init(mCachedDevice, mManager);
        use(KeyboardSettingsPreferenceController.class).init(mCachedDevice);

        final BluetoothFeatureProvider featureProvider =
                FeatureFactory.getFeatureFactory().getBluetoothFeatureProvider();
        final boolean sliceEnabled = DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_SETTINGS_UI,
                SettingsUIDeviceConfig.BT_SLICE_SETTINGS_ENABLED, true);

        use(BlockingPrefWithSliceController.class).setSliceUri(sliceEnabled
                ? featureProvider.getBluetoothDeviceSettingsUri(mCachedDevice.getDevice())
                : null);
    }

    private void updateExtraControlUri(int viewWidth) {
        BluetoothFeatureProvider featureProvider =
                FeatureFactory.getFeatureFactory().getBluetoothFeatureProvider();
        boolean sliceEnabled = DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_SETTINGS_UI,
                SettingsUIDeviceConfig.BT_SLICE_SETTINGS_ENABLED, true);
        Uri controlUri = null;
        String uri = featureProvider.getBluetoothDeviceControlUri(mCachedDevice.getDevice());
        if (!TextUtils.isEmpty(uri)) {
            try {
                controlUri = Uri.parse(uri + viewWidth);
            } catch (NullPointerException exception) {
                Log.d(TAG, "unable to parse uri");
                controlUri = null;
            }
        }
        final SlicePreferenceController slicePreferenceController = use(
                SlicePreferenceController.class);
        slicePreferenceController.setSliceUri(sliceEnabled ? controlUri : null);
        slicePreferenceController.onStart();
        slicePreferenceController.displayPreference(getPreferenceScreen());

        // Temporarily fix the issue that the page will be automatically scrolled to a wrong
        // position when entering the page. This will make sure the bluetooth header is shown on top
        // of the page.
        use(LeAudioBluetoothDetailsHeaderController.class).displayPreference(
                getPreferenceScreen());
        use(AdvancedBluetoothDetailsHeaderController.class).displayPreference(
                getPreferenceScreen());
        use(BluetoothDetailsHeaderController.class).displayPreference(
                getPreferenceScreen());
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
                    updateExtraControlUri(view.getWidth() - getPaddingSize());
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
        if (!mUserManager.isGuestUser()) {
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
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        ArrayList<AbstractPreferenceController> controllers = new ArrayList<>();

        if (mCachedDevice != null) {
            Lifecycle lifecycle = getSettingsLifecycle();
            controllers.add(new BluetoothDetailsHeaderController(context, this, mCachedDevice,
                    lifecycle));
            controllers.add(new BluetoothDetailsButtonsController(context, this, mCachedDevice,
                    lifecycle));
            controllers.add(new BluetoothDetailsCompanionAppsController(context, this,
                    mCachedDevice, lifecycle));
            controllers.add(new BluetoothDetailsAudioDeviceTypeController(context, this, mManager,
                    mCachedDevice, lifecycle));
            controllers.add(new BluetoothDetailsSpatialAudioController(context, this, mCachedDevice,
                    lifecycle));
            controllers.add(new BluetoothDetailsProfilesController(context, this, mManager,
                    mCachedDevice, lifecycle));
            controllers.add(new BluetoothDetailsMacAddressController(context, this, mCachedDevice,
                    lifecycle));
            controllers.add(new StylusDevicesController(context, mInputDevice, mCachedDevice,
                    lifecycle));
            controllers.add(new BluetoothDetailsRelatedToolsController(context, this, mCachedDevice,
                    lifecycle));
            controllers.add(new BluetoothDetailsPairOtherController(context, this, mCachedDevice,
                    lifecycle));
            controllers.add(new BluetoothDetailsHearingDeviceControlsController(context, this,
                    mCachedDevice, lifecycle));
            controllers.add(new BluetoothDetailsDataSyncController(context, this,
                    mCachedDevice, lifecycle));
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
