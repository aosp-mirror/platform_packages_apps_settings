/*
 * Copyright (C) 2022 The Android Open Source Project
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
import android.app.AlertDialog;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastMetadata;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


/**
 * This fragment allowed users to find the nearby broadcast sources.
 */
public class BluetoothFindBroadcastsFragment extends RestrictedDashboardFragment {

    private static final String TAG = "BtFindBroadcastsFrg";

    public static final String KEY_DEVICE_ADDRESS = "device_address";
    public static final String PREF_KEY_BROADCAST_SOURCE_LIST = "broadcast_source_list";
    public static final int REQUEST_SCAN_BT_BROADCAST_QR_CODE = 0;

    @VisibleForTesting
    String mDeviceAddress;
    @VisibleForTesting
    LocalBluetoothManager mManager;
    @VisibleForTesting
    CachedBluetoothDevice mCachedDevice;
    @VisibleForTesting
    PreferenceCategory mBroadcastSourceListCategory;
    @VisibleForTesting
    BluetoothBroadcastSourcePreference mSelectedPreference;
    BluetoothFindBroadcastsHeaderController mBluetoothFindBroadcastsHeaderController;

    private LocalBluetoothLeBroadcastAssistant mLeBroadcastAssistant;
    private LocalBluetoothLeBroadcastMetadata mLocalBroadcastMetadata;
    private Executor mExecutor;
    private int mSourceId;

    private BluetoothLeBroadcastAssistant.Callback mBroadcastAssistantCallback =
            new BluetoothLeBroadcastAssistant.Callback() {
                @Override
                public void onSearchStarted(int reason) {
                    Log.d(TAG, "onSearchStarted: " + reason);
                    getActivity().runOnUiThread(() -> handleSearchStarted());
                }

                @Override
                public void onSearchStartFailed(int reason) {
                    Log.d(TAG, "onSearchStartFailed: " + reason);
                }

                @Override
                public void onSearchStopped(int reason) {
                    Log.d(TAG, "onSearchStopped: " + reason);
                }

                @Override
                public void onSearchStopFailed(int reason) {
                    Log.d(TAG, "onSearchStopFailed: " + reason);
                }

                @Override
                public void onSourceFound(@NonNull BluetoothLeBroadcastMetadata source) {
                    Log.d(TAG, "onSourceFound:");
                    getActivity().runOnUiThread(
                            () -> updateListCategoryFromBroadcastMetadata(source, false));
                }

                @Override
                public void onSourceAdded(@NonNull BluetoothDevice sink, int sourceId, int reason) {
                    setSourceId(sourceId);
                    if (mSelectedPreference == null) {
                        Log.w(TAG, "onSourceAdded: mSelectedPreference == null!");
                        return;
                    }
                    if (mLeBroadcastAssistant != null
                            && mLeBroadcastAssistant.isSearchInProgress()) {
                        mLeBroadcastAssistant.stopSearchingForSources();
                    }
                    getActivity().runOnUiThread(() -> updateListCategoryFromBroadcastMetadata(
                            mSelectedPreference.getBluetoothLeBroadcastMetadata(), true));
                }

                @Override
                public void onSourceAddFailed(@NonNull BluetoothDevice sink,
                        @NonNull BluetoothLeBroadcastMetadata source, int reason) {
                    mSelectedPreference = null;
                    Log.d(TAG, "onSourceAddFailed: clear the mSelectedPreference.");
                }

                @Override
                public void onSourceModified(@NonNull BluetoothDevice sink, int sourceId,
                        int reason) {
                }

                @Override
                public void onSourceModifyFailed(@NonNull BluetoothDevice sink, int sourceId,
                        int reason) {
                }

                @Override
                public void onSourceRemoved(@NonNull BluetoothDevice sink, int sourceId,
                        int reason) {
                    Log.d(TAG, "onSourceRemoved:");
                    getActivity().runOnUiThread(() -> handleSourceRemoved());
                }

                @Override
                public void onSourceRemoveFailed(@NonNull BluetoothDevice sink, int sourceId,
                        int reason) {
                    Log.d(TAG, "onSourceRemoveFailed:");
                }

                @Override
                public void onReceiveStateChanged(@NonNull BluetoothDevice sink, int sourceId,
                        @NonNull BluetoothLeBroadcastReceiveState state) {
                    Log.d(TAG, "onReceiveStateChanged:");
                }
            };

    public BluetoothFindBroadcastsFragment() {
        super(DISALLOW_CONFIG_BLUETOOTH);
    }

    @VisibleForTesting
    LocalBluetoothManager getLocalBluetoothManager(Context context) {
        return Utils.getLocalBtManager(context);
    }

    @VisibleForTesting
    CachedBluetoothDevice getCachedDevice(String deviceAddress) {
        BluetoothDevice remoteDevice =
                mManager.getBluetoothAdapter().getRemoteDevice(deviceAddress);
        return mManager.getCachedDeviceManager().findDevice(remoteDevice);
    }

    @Override
    public void onAttach(Context context) {
        mDeviceAddress = getArguments().getString(KEY_DEVICE_ADDRESS);
        mManager = getLocalBluetoothManager(context);
        mCachedDevice = getCachedDevice(mDeviceAddress);
        mLeBroadcastAssistant = getLeBroadcastAssistant();
        mExecutor = Executors.newSingleThreadExecutor();
        mLocalBroadcastMetadata = new LocalBluetoothLeBroadcastMetadata();

        super.onAttach(context);
        if (mCachedDevice == null || mLeBroadcastAssistant == null) {
            //Close this page if device is null with invalid device mac address
            //or if the device does not have LeBroadcastAssistant profile
            Log.w(TAG, "onAttach() CachedDevice or LeBroadcastAssistant is null!");
            finish();
            return;
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mBroadcastSourceListCategory = findPreference(PREF_KEY_BROADCAST_SOURCE_LIST);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mLeBroadcastAssistant != null) {
            mLeBroadcastAssistant.registerServiceCallBack(mExecutor, mBroadcastAssistantCallback);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        finishFragmentIfNecessary();
        //check assistant status. Start searching...
        if (mLeBroadcastAssistant != null && !mLeBroadcastAssistant.isSearchInProgress()) {
            mLeBroadcastAssistant.startSearchingForSources(getScanFilter());
        } else {
            addConnectedSourcePreference();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mLeBroadcastAssistant != null) {
            if (mLeBroadcastAssistant.isSearchInProgress()) {
                mLeBroadcastAssistant.stopSearchingForSources();
            }
            mLeBroadcastAssistant.unregisterServiceCallBack(mBroadcastAssistantCallback);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: " + requestCode + ", resultCode: " + resultCode);
        if (requestCode == REQUEST_SCAN_BT_BROADCAST_QR_CODE) {
            if (resultCode == Activity.RESULT_OK) {

                //Get BroadcastMetadata
                String broadcastMetadata = data.getStringExtra(
                        QrCodeScanModeFragment.KEY_BROADCAST_METADATA);
                BluetoothLeBroadcastMetadata source = convertToBroadcastMetadata(broadcastMetadata);

                if (source != null) {
                    Log.d(TAG, "onActivityResult source Id = " + source.getBroadcastId());
                    //Create preference for the broadcast source
                    updateListCategoryFromBroadcastMetadata(source, false);
                    //Add Source
                    addSource(mBroadcastSourceListCategory.findPreference(
                            Integer.toString(source.getBroadcastId())));
                } else {
                    Toast.makeText(getContext(),
                        R.string.find_broadcast_join_broadcast_error, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
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
        return SettingsEnums.LE_AUDIO_BROADCAST_FIND_BROADCAST;
    }

    /**
     * Starts to scan broadcast source by the BluetoothLeBroadcastAssistant.
     */
    public void scanBroadcastSource() {
        if (mLeBroadcastAssistant == null) {
            Log.w(TAG, "scanBroadcastSource: LeBroadcastAssistant is null!");
            return;
        }
        mLeBroadcastAssistant.startSearchingForSources(getScanFilter());
    }

    /**
     * Leaves the broadcast source by the BluetoothLeBroadcastAssistant.
     */
    public void leaveBroadcastSession() {
        if (mLeBroadcastAssistant == null || mCachedDevice == null) {
            Log.w(TAG, "leaveBroadcastSession: LeBroadcastAssistant or CachedDevice is null!");
            return;
        }
        mLeBroadcastAssistant.removeSource(mCachedDevice.getDevice(), getSourceId());
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.bluetooth_find_broadcasts_fragment;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        ArrayList<AbstractPreferenceController> controllers = new ArrayList<>();

        if (mCachedDevice != null) {
            Lifecycle lifecycle = getSettingsLifecycle();
            mBluetoothFindBroadcastsHeaderController = new BluetoothFindBroadcastsHeaderController(
                    context, this, mCachedDevice, lifecycle, mManager);
            controllers.add(mBluetoothFindBroadcastsHeaderController);
        }
        return controllers;
    }

    /**
     * Gets the LocalBluetoothLeBroadcastAssistant
     * @return the LocalBluetoothLeBroadcastAssistant
     */
    public LocalBluetoothLeBroadcastAssistant getLeBroadcastAssistant() {
        if (mManager == null) {
            Log.w(TAG, "getLeBroadcastAssistant: LocalBluetoothManager is null!");
            return null;
        }

        LocalBluetoothProfileManager profileManager = mManager.getProfileManager();
        if (profileManager == null) {
            Log.w(TAG, "getLeBroadcastAssistant: LocalBluetoothProfileManager is null!");
            return null;
        }

        return profileManager.getLeAudioBroadcastAssistantProfile();
    }

    private List<ScanFilter> getScanFilter() {
        // Currently there is no function for setting the ScanFilter. It may have this function
        // in the further.
        return Collections.emptyList();
    }

    private void updateListCategoryFromBroadcastMetadata(BluetoothLeBroadcastMetadata source,
            boolean isConnected) {
        BluetoothBroadcastSourcePreference item = mBroadcastSourceListCategory.findPreference(
                Integer.toString(source.getBroadcastId()));
        if (item == null) {
            item = createBluetoothBroadcastSourcePreference();
            item.setKey(Integer.toString(source.getBroadcastId()));
            mBroadcastSourceListCategory.addPreference(item);
        }
        item.updateMetadataAndRefreshUi(source, isConnected);
        item.setOrder(isConnected ? 0 : 1);

        //refresh the header
        if (mBluetoothFindBroadcastsHeaderController != null) {
            mBluetoothFindBroadcastsHeaderController.refreshUi();
        }
    }

    private void updateListCategoryFromBroadcastReceiveState(
            BluetoothLeBroadcastReceiveState receiveState) {
        BluetoothBroadcastSourcePreference item = mBroadcastSourceListCategory.findPreference(
                Integer.toString(receiveState.getBroadcastId()));
        if (item == null) {
            item = createBluetoothBroadcastSourcePreference();
            item.setKey(Integer.toString(receiveState.getBroadcastId()));
            mBroadcastSourceListCategory.addPreference(item);
        }
        item.updateReceiveStateAndRefreshUi(receiveState);
        item.setOrder(0);

        setSourceId(receiveState.getSourceId());
        mSelectedPreference = item;

        //refresh the header
        if (mBluetoothFindBroadcastsHeaderController != null) {
            mBluetoothFindBroadcastsHeaderController.refreshUi();
        }
    }

    private BluetoothBroadcastSourcePreference createBluetoothBroadcastSourcePreference() {
        BluetoothBroadcastSourcePreference pref = new BluetoothBroadcastSourcePreference(
                getContext());
        pref.setOnPreferenceClickListener(preference -> {
            if (pref.getBluetoothLeBroadcastMetadata() == null) {
                Log.d(TAG, "BluetoothLeBroadcastMetadata is null, do nothing.");
                return false;
            }
            if (pref.isEncrypted()) {
                launchBroadcastCodeDialog(pref);
            } else {
                addSource(pref);
            }
            return true;
        });
        return pref;
    }

    @VisibleForTesting
    void addSource(BluetoothBroadcastSourcePreference pref) {
        if (mLeBroadcastAssistant == null || mCachedDevice == null) {
            Log.w(TAG, "addSource: LeBroadcastAssistant or CachedDevice is null!");
            return;
        }
        if (mSelectedPreference != null) {
            if (mSelectedPreference.isCreatedByReceiveState()) {
                Log.d(TAG, "addSource: Remove preference that created by getAllSources()");
                getActivity().runOnUiThread(() ->
                        mBroadcastSourceListCategory.removePreference(mSelectedPreference));
                if (mLeBroadcastAssistant != null && !mLeBroadcastAssistant.isSearchInProgress()) {
                    Log.d(TAG, "addSource: Start Searching For Broadcast Sources");
                    mLeBroadcastAssistant.startSearchingForSources(getScanFilter());
                }
            } else {
                Log.d(TAG, "addSource: Update preference that created by onSourceFound()");
                // The previous preference status set false after user selects the new Preference.
                getActivity().runOnUiThread(
                    () -> {
                        mSelectedPreference.updateMetadataAndRefreshUi(
                                mSelectedPreference.getBluetoothLeBroadcastMetadata(), false);
                        mSelectedPreference.setOrder(1);
                    });
            }
        }
        mSelectedPreference = pref;
        mLeBroadcastAssistant.addSource(mCachedDevice.getDevice(),
                pref.getBluetoothLeBroadcastMetadata(), true);
    }

    private void addBroadcastCodeIntoPreference(BluetoothBroadcastSourcePreference pref,
            String broadcastCode) {
        BluetoothLeBroadcastMetadata metadata =
                new BluetoothLeBroadcastMetadata.Builder(pref.getBluetoothLeBroadcastMetadata())
                        .setBroadcastCode(broadcastCode.getBytes(StandardCharsets.UTF_8))
                        .build();
        pref.updateMetadataAndRefreshUi(metadata, false);
    }

    private void launchBroadcastCodeDialog(BluetoothBroadcastSourcePreference pref) {
        final View layout = LayoutInflater.from(getContext()).inflate(
                R.layout.bluetooth_find_broadcast_password_dialog, null);
        final TextView broadcastName = layout.requireViewById(R.id.broadcast_name_text);
        final EditText editText = layout.requireViewById(R.id.broadcast_edit_text);
        broadcastName.setText(pref.getTitle());
        AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.find_broadcast_password_dialog_title)
                .setView(layout)
                .setNeutralButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.bluetooth_connect_access_dialog_positive,
                        (d, w) -> {
                            Log.d(TAG, "setPositiveButton: clicked");
                            if (pref.getBluetoothLeBroadcastMetadata() == null) {
                                Log.d(TAG, "BluetoothLeBroadcastMetadata is null, do nothing.");
                                return;
                            }
                            addBroadcastCodeIntoPreference(pref, editText.getText().toString());
                            addSource(pref);
                        })
                .create();

        alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        addTextWatcher(alertDialog, editText);
        alertDialog.show();
        updateBtnState(alertDialog, false);
    }

    private void addTextWatcher(AlertDialog alertDialog, EditText editText) {
        if (alertDialog == null || editText == null) {
            return;
        }
        final InputFilter[] filter = new InputFilter[] {mInputFilter};
        editText.setFilters(filter);
        editText.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        TextWatcher bCodeTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Do nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
                boolean breakBroadcastCodeRuleTextLengthLessThanMin =
                        s.length() > 0 && s.toString().getBytes().length < 4;
                boolean breakBroadcastCodeRuleTextLengthMoreThanMax =
                        s.toString().getBytes().length > 16;
                boolean breakRule = breakBroadcastCodeRuleTextLengthLessThanMin
                        || breakBroadcastCodeRuleTextLengthMoreThanMax;
                updateBtnState(alertDialog, !breakRule);
            }
        };
        editText.addTextChangedListener(bCodeTextWatcher);
    }

    private void updateBtnState(AlertDialog alertDialog, boolean isEnable) {
        Button positiveBtn = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positiveBtn != null) {
            positiveBtn.setEnabled(isEnable ? true : false);
        }
    }

    private InputFilter mInputFilter = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end,
                Spanned dest, int dstart, int dend) {
            byte[] bytes = source.toString().getBytes(StandardCharsets.UTF_8);
            if (bytes.length == source.length()) {
                return source;
            } else {
                return "";
            }
        }
    };

    private void handleSearchStarted() {
        cacheRemoveAllPrefs(mBroadcastSourceListCategory);
        addConnectedSourcePreference();
    }

    private void handleSourceRemoved() {
        if (mSelectedPreference != null) {
            if (mSelectedPreference.getBluetoothLeBroadcastMetadata() == null) {
                mBroadcastSourceListCategory.removePreference(mSelectedPreference);
            } else {
                mSelectedPreference.clearReceiveState();
            }
        }
        mSelectedPreference = null;
    }

    private void addConnectedSourcePreference() {
        List<BluetoothLeBroadcastReceiveState> receiveStateList =
                mLeBroadcastAssistant.getAllSources(mCachedDevice.getDevice());
        if (!receiveStateList.isEmpty()) {
            updateListCategoryFromBroadcastReceiveState(receiveStateList.get(0));
        }
    }

    public int getSourceId() {
        return mSourceId;
    }

    public void setSourceId(int sourceId) {
        mSourceId = sourceId;
    }

    private BluetoothLeBroadcastMetadata convertToBroadcastMetadata(String qrCodeString) {
        return mLocalBroadcastMetadata.convertToBroadcastMetadata(qrCodeString);
    }
}
