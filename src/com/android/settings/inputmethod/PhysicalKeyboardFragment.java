/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.inputmethod;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.hardware.input.InputDeviceIdentifier;
import android.hardware.input.InputManager;
import android.hardware.input.InputSettings;
import android.hardware.input.KeyboardLayout;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.FeatureFlagUtils;
import android.view.InputDevice;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.internal.util.Preconditions;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.utils.ThreadUtils;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

// TODO(b/327638540): Update implementation of preference here and reuse key preferences and
//  controllers between here and A11y Setting page.
@SearchIndexable
public final class PhysicalKeyboardFragment extends SettingsPreferenceFragment
        implements InputManager.InputDeviceListener,
        KeyboardLayoutDialogFragment.OnSetupKeyboardLayoutsListener {

    private static final String KEYBOARD_OPTIONS_CATEGORY = "keyboard_options_category";
    private static final String KEYBOARD_A11Y_CATEGORY = "keyboard_a11y_category";
    private static final String SHOW_VIRTUAL_KEYBOARD_SWITCH = "show_virtual_keyboard_switch";
    private static final String ACCESSIBILITY_BOUNCE_KEYS = "accessibility_bounce_keys";
    private static final String ACCESSIBILITY_SLOW_KEYS = "accessibility_slow_keys";
    private static final String ACCESSIBILITY_STICKY_KEYS = "accessibility_sticky_keys";
    private static final String KEYBOARD_SHORTCUTS_HELPER = "keyboard_shortcuts_helper";
    private static final String MODIFIER_KEYS_SETTINGS = "modifier_keys_settings";
    private static final String EXTRA_AUTO_SELECTION = "auto_selection";
    private static final Uri sVirtualKeyboardSettingsUri = Secure.getUriFor(
            Secure.SHOW_IME_WITH_HARD_KEYBOARD);
    private static final Uri sAccessibilityBounceKeysUri = Secure.getUriFor(
            Secure.ACCESSIBILITY_BOUNCE_KEYS);
    private static final Uri sAccessibilitySlowKeysUri = Secure.getUriFor(
            Secure.ACCESSIBILITY_SLOW_KEYS);
    private static final Uri sAccessibilityStickyKeysUri = Secure.getUriFor(
            Secure.ACCESSIBILITY_STICKY_KEYS);
    public static final int BOUNCE_KEYS_THRESHOLD = 500;
    public static final int SLOW_KEYS_THRESHOLD = 500;

    @NonNull
    private final ArrayList<HardKeyboardDeviceInfo> mLastHardKeyboards = new ArrayList<>();

    private InputManager mIm;
    private InputMethodManager mImm;
    private InputDeviceIdentifier mAutoInputDeviceIdentifier;
    private KeyboardSettingsFeatureProvider mFeatureProvider;
    @NonNull
    private PreferenceCategory mKeyboardAssistanceCategory;
    @Nullable
    private PreferenceCategory mKeyboardA11yCategory = null;
    @Nullable
    private TwoStatePreference mShowVirtualKeyboardSwitch = null;
    @Nullable
    private TwoStatePreference mAccessibilityBounceKeys = null;
    @Nullable
    private TwoStatePreference mAccessibilitySlowKeys = null;
    @Nullable
    private TwoStatePreference mAccessibilityStickyKeys = null;


    private Intent mIntentWaitingForResult;
    private boolean mIsNewKeyboardSettings;
    private boolean mSupportsFirmwareUpdate;

    static final String EXTRA_BT_ADDRESS = "extra_bt_address";
    private String mBluetoothAddress;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(EXTRA_AUTO_SELECTION, mAutoInputDeviceIdentifier);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        Activity activity = Preconditions.checkNotNull(getActivity());
        addPreferencesFromResource(R.xml.physical_keyboard_settings);
        mIm = Preconditions.checkNotNull(activity.getSystemService(InputManager.class));
        mImm = Preconditions.checkNotNull(activity.getSystemService(InputMethodManager.class));
        mKeyboardAssistanceCategory = Preconditions.checkNotNull(
                findPreference(KEYBOARD_OPTIONS_CATEGORY));
        mShowVirtualKeyboardSwitch = Objects.requireNonNull(
                mKeyboardAssistanceCategory.findPreference(SHOW_VIRTUAL_KEYBOARD_SWITCH));

        mKeyboardA11yCategory = Objects.requireNonNull(findPreference(KEYBOARD_A11Y_CATEGORY));
        mAccessibilityBounceKeys = Objects.requireNonNull(
                mKeyboardA11yCategory.findPreference(ACCESSIBILITY_BOUNCE_KEYS));
        mAccessibilityBounceKeys.setSummary(
                getContext().getString(R.string.bounce_keys_summary, BOUNCE_KEYS_THRESHOLD));
        mAccessibilitySlowKeys = Objects.requireNonNull(
                mKeyboardA11yCategory.findPreference(ACCESSIBILITY_SLOW_KEYS));
        mAccessibilitySlowKeys.setSummary(
                getContext().getString(R.string.slow_keys_summary, SLOW_KEYS_THRESHOLD));
        mAccessibilityStickyKeys = Objects.requireNonNull(
                mKeyboardA11yCategory.findPreference(ACCESSIBILITY_STICKY_KEYS));

        FeatureFactory featureFactory = FeatureFactory.getFeatureFactory();
        mMetricsFeatureProvider = featureFactory.getMetricsFeatureProvider();
        mFeatureProvider = featureFactory.getKeyboardSettingsFeatureProvider();
        mSupportsFirmwareUpdate = mFeatureProvider.supportsFirmwareUpdate();
        if (mSupportsFirmwareUpdate) {
            mFeatureProvider.addFirmwareUpdateCategory(getContext(), getPreferenceScreen());
        }
        mIsNewKeyboardSettings = FeatureFlagUtils.isEnabled(
                getContext(), FeatureFlagUtils.SETTINGS_NEW_KEYBOARD_UI);
        boolean isModifierKeySettingsEnabled = FeatureFlagUtils
                .isEnabled(getContext(), FeatureFlagUtils.SETTINGS_NEW_KEYBOARD_MODIFIER_KEY);
        if (!isModifierKeySettingsEnabled) {
            mKeyboardAssistanceCategory.removePreference(findPreference(MODIFIER_KEYS_SETTINGS));
        }
        if (!InputSettings.isAccessibilityBounceKeysFeatureEnabled()) {
            mKeyboardA11yCategory.removePreference(mAccessibilityBounceKeys);
        }
        if (!InputSettings.isAccessibilitySlowKeysFeatureFlagEnabled()) {
            mKeyboardA11yCategory.removePreference(mAccessibilitySlowKeys);
        }
        if (!InputSettings.isAccessibilityStickyKeysFeatureEnabled()) {
            mKeyboardA11yCategory.removePreference(mAccessibilityStickyKeys);
        }
        InputDeviceIdentifier inputDeviceIdentifier = activity.getIntent().getParcelableExtra(
                KeyboardLayoutPickerFragment.EXTRA_INPUT_DEVICE_IDENTIFIER,
                InputDeviceIdentifier.class);
        int intentFromWhere =
                activity.getIntent().getIntExtra(android.provider.Settings.EXTRA_ENTRYPOINT, -1);
        if (intentFromWhere != -1) {
            mMetricsFeatureProvider.action(
                    getContext(), SettingsEnums.ACTION_OPEN_PK_SETTINGS_FROM, intentFromWhere);
        }
        if (inputDeviceIdentifier != null) {
            mAutoInputDeviceIdentifier = inputDeviceIdentifier;
        }
        // Don't repeat the autoselection.
        if (isAutoSelection(bundle, inputDeviceIdentifier)) {
            showEnabledLocalesKeyboardLayoutList(inputDeviceIdentifier);
        }
    }

    private static boolean isAutoSelection(Bundle bundle, InputDeviceIdentifier identifier) {
        if (bundle != null && bundle.getParcelable(EXTRA_AUTO_SELECTION,
                InputDeviceIdentifier.class) != null) {
            return false;
        }
        return identifier != null;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (KEYBOARD_SHORTCUTS_HELPER.equals(preference.getKey())) {
            writePreferenceClickMetric(preference);
            toggleKeyboardShortcutsMenu();
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onResume() {
        super.onResume();
        mLastHardKeyboards.clear();
        scheduleUpdateHardKeyboards();
        mIm.registerInputDeviceListener(this, null);
        Objects.requireNonNull(mShowVirtualKeyboardSwitch).setOnPreferenceChangeListener(
                mShowVirtualKeyboardSwitchPreferenceChangeListener);
        Objects.requireNonNull(mAccessibilityBounceKeys).setOnPreferenceChangeListener(
                mAccessibilityBounceKeysSwitchPreferenceChangeListener);
        Objects.requireNonNull(mAccessibilitySlowKeys).setOnPreferenceChangeListener(
                mAccessibilitySlowKeysSwitchPreferenceChangeListener);
        Objects.requireNonNull(mAccessibilityStickyKeys).setOnPreferenceChangeListener(
                mAccessibilityStickyKeysSwitchPreferenceChangeListener);
        registerSettingsObserver();
    }

    @Override
    public void onPause() {
        super.onPause();
        mLastHardKeyboards.clear();
        mIm.unregisterInputDeviceListener(this);
        Objects.requireNonNull(mShowVirtualKeyboardSwitch).setOnPreferenceChangeListener(null);
        Objects.requireNonNull(mAccessibilityBounceKeys).setOnPreferenceChangeListener(null);
        Objects.requireNonNull(mAccessibilitySlowKeys).setOnPreferenceChangeListener(null);
        Objects.requireNonNull(mAccessibilityStickyKeys).setOnPreferenceChangeListener(null);
        unregisterSettingsObserver();
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        scheduleUpdateHardKeyboards();
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        scheduleUpdateHardKeyboards();
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        scheduleUpdateHardKeyboards();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PHYSICAL_KEYBOARDS;
    }

    private void scheduleUpdateHardKeyboards() {
        final Context context = getContext();
        ThreadUtils.postOnBackgroundThread(() -> {
            final List<HardKeyboardDeviceInfo> newHardKeyboards = getHardKeyboards(context);
            if (newHardKeyboards.isEmpty()) {
                getActivity().finish();
                return;
            }
            ThreadUtils.postOnMainThread(() -> updateHardKeyboards(newHardKeyboards));
        });
    }

    private void updateHardKeyboards(@NonNull List<HardKeyboardDeviceInfo> newHardKeyboards) {
        if (Objects.equals(mLastHardKeyboards, newHardKeyboards)) {
            // Nothing has changed.  Ignore.
            return;
        }

        // TODO(yukawa): Maybe we should follow the style used in ConnectedDeviceDashboardFragment.

        mLastHardKeyboards.clear();
        mLastHardKeyboards.addAll(newHardKeyboards);

        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.removeAll();
        final PreferenceCategory category = new PreferenceCategory(getPrefContext());
        category.setTitle(R.string.builtin_keyboard_settings_title);
        category.setOrder(0);
        preferenceScreen.addPreference(category);

        for (HardKeyboardDeviceInfo hardKeyboardDeviceInfo : newHardKeyboards) {
            // TODO(yukawa): Consider using com.android.settings.widget.GearPreference
            final Preference pref = new Preference(getPrefContext());
            pref.setTitle(hardKeyboardDeviceInfo.mDeviceName);
            if (mIsNewKeyboardSettings) {
                String currentLayout =
                        NewKeyboardSettingsUtils.getSelectedKeyboardLayoutLabelForUser(getContext(),
                                UserHandle.myUserId(), hardKeyboardDeviceInfo.mDeviceIdentifier);
                if (currentLayout != null) {
                    pref.setSummary(currentLayout);
                }
                pref.setOnPreferenceClickListener(
                        preference -> {
                            showEnabledLocalesKeyboardLayoutList(
                                    hardKeyboardDeviceInfo.mDeviceIdentifier);
                            return true;
                        });
            } else {
                pref.setSummary(hardKeyboardDeviceInfo.mLayoutLabel);
                pref.setOnPreferenceClickListener(
                        preference -> {
                            showKeyboardLayoutDialog(hardKeyboardDeviceInfo.mDeviceIdentifier);
                            return true;
                        });
            }
            category.addPreference(pref);
            StringBuilder vendorAndProductId = new StringBuilder();
            String vendorId = String.valueOf(hardKeyboardDeviceInfo.mVendorId);
            String productId = String.valueOf(hardKeyboardDeviceInfo.mProductId);
            vendorAndProductId.append(vendorId);
            vendorAndProductId.append("-");
            vendorAndProductId.append(productId);
            mMetricsFeatureProvider.action(
                    getContext(),
                    SettingsEnums.ACTION_USE_SPECIFIC_KEYBOARD,
                    vendorAndProductId.toString());
        }
        mKeyboardAssistanceCategory.setOrder(1);
        preferenceScreen.addPreference(mKeyboardAssistanceCategory);
        if (mSupportsFirmwareUpdate) {
            mFeatureProvider.addFirmwareUpdateCategory(getPrefContext(), preferenceScreen);
        }
        updateShowVirtualKeyboardSwitch();

        if (InputSettings.isAccessibilityBounceKeysFeatureEnabled()
                || InputSettings.isAccessibilityStickyKeysFeatureEnabled()
                || InputSettings.isAccessibilitySlowKeysFeatureFlagEnabled()) {
            Objects.requireNonNull(mKeyboardA11yCategory).setOrder(2);
            preferenceScreen.addPreference(mKeyboardA11yCategory);
            updateAccessibilityBounceKeysSwitch();
            updateAccessibilitySlowKeysSwitch();
            updateAccessibilityStickyKeysSwitch();
        }
    }

    private void showKeyboardLayoutDialog(InputDeviceIdentifier inputDeviceIdentifier) {
        KeyboardLayoutDialogFragment fragment = new KeyboardLayoutDialogFragment(
                inputDeviceIdentifier);
        fragment.setTargetFragment(this, 0);
        fragment.show(getActivity().getSupportFragmentManager(), "keyboardLayout");
    }

    private void showEnabledLocalesKeyboardLayoutList(InputDeviceIdentifier inputDeviceIdentifier) {
        Bundle arguments = new Bundle();
        arguments.putParcelable(NewKeyboardSettingsUtils.EXTRA_INPUT_DEVICE_IDENTIFIER,
                inputDeviceIdentifier);
        new SubSettingLauncher(getContext())
                .setSourceMetricsCategory(getMetricsCategory())
                .setDestination(NewKeyboardLayoutEnabledLocalesFragment.class.getName())
                .setArguments(arguments)
                .launch();
    }

    private void registerSettingsObserver() {
        unregisterSettingsObserver();
        ContentResolver contentResolver = getActivity().getContentResolver();
        contentResolver.registerContentObserver(
                sVirtualKeyboardSettingsUri,
                false,
                mContentObserver,
                UserHandle.myUserId());
        if (InputSettings.isAccessibilityBounceKeysFeatureEnabled()) {
            contentResolver.registerContentObserver(
                    sAccessibilityBounceKeysUri,
                    false,
                    mContentObserver,
                    UserHandle.myUserId());
        }
        if (InputSettings.isAccessibilitySlowKeysFeatureFlagEnabled()) {
            contentResolver.registerContentObserver(
                    sAccessibilitySlowKeysUri,
                    false,
                    mContentObserver,
                    UserHandle.myUserId());
        }
        if (InputSettings.isAccessibilityStickyKeysFeatureEnabled()) {
            contentResolver.registerContentObserver(
                    sAccessibilityStickyKeysUri,
                    false,
                    mContentObserver,
                    UserHandle.myUserId());
        }
        updateShowVirtualKeyboardSwitch();
        updateAccessibilityBounceKeysSwitch();
        updateAccessibilitySlowKeysSwitch();
        updateAccessibilityStickyKeysSwitch();
    }

    private void unregisterSettingsObserver() {
        getActivity().getContentResolver().unregisterContentObserver(mContentObserver);
    }

    private void updateShowVirtualKeyboardSwitch() {
        Objects.requireNonNull(mShowVirtualKeyboardSwitch).setChecked(
                Secure.getInt(getContentResolver(), Secure.SHOW_IME_WITH_HARD_KEYBOARD, 0) != 0);
    }

    private void updateAccessibilityBounceKeysSwitch() {
        if (!InputSettings.isAccessibilityBounceKeysFeatureEnabled()) {
            return;
        }
        Objects.requireNonNull(mAccessibilityBounceKeys).setChecked(
                InputSettings.isAccessibilityBounceKeysEnabled(getContext()));
    }

    private void updateAccessibilitySlowKeysSwitch() {
        if (!InputSettings.isAccessibilitySlowKeysFeatureFlagEnabled()) {
            return;
        }
        Objects.requireNonNull(mAccessibilitySlowKeys).setChecked(
                InputSettings.isAccessibilitySlowKeysEnabled(getContext()));
    }

    private void updateAccessibilityStickyKeysSwitch() {
        if (!InputSettings.isAccessibilityStickyKeysFeatureEnabled()) {
            return;
        }
        Objects.requireNonNull(mAccessibilityStickyKeys).setChecked(
                InputSettings.isAccessibilityStickyKeysEnabled(getContext()));
    }

    private void toggleKeyboardShortcutsMenu() {
        getActivity().requestShowKeyboardShortcuts();
    }

    private final OnPreferenceChangeListener mShowVirtualKeyboardSwitchPreferenceChangeListener =
            (preference, newValue) -> {
                final ContentResolver cr = getContentResolver();
                Secure.putInt(cr, Secure.SHOW_IME_WITH_HARD_KEYBOARD, ((Boolean) newValue) ? 1 : 0);
                cr.notifyChange(Secure.getUriFor(Secure.SHOW_IME_WITH_HARD_KEYBOARD),
                        null /* observer */, ContentResolver.NOTIFY_NO_DELAY);
                return true;
            };

    private final OnPreferenceChangeListener
            mAccessibilityBounceKeysSwitchPreferenceChangeListener = (preference, newValue) -> {
                InputSettings.setAccessibilityBounceKeysThreshold(getContext(),
                        ((Boolean) newValue) ? 500 : 0);
                return true;
            };

    private final OnPreferenceChangeListener
            mAccessibilitySlowKeysSwitchPreferenceChangeListener = (preference, newValue) -> {
                InputSettings.setAccessibilitySlowKeysThreshold(getContext(),
                        ((Boolean) newValue) ? 500 : 0);
                return true;
            };

    private final OnPreferenceChangeListener
            mAccessibilityStickyKeysSwitchPreferenceChangeListener = (preference, newValue) -> {
                InputSettings.setAccessibilityStickyKeysEnabled(getContext(), (Boolean) newValue);
                return true;
            };

    private final ContentObserver mContentObserver = new ContentObserver(new Handler(true)) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (sVirtualKeyboardSettingsUri.equals(uri)) {
                updateShowVirtualKeyboardSwitch();
            } else if (sAccessibilityBounceKeysUri.equals(uri)) {
                updateAccessibilityBounceKeysSwitch();
            } else if (sAccessibilitySlowKeysUri.equals(uri)) {
                updateAccessibilitySlowKeysSwitch();
            } else if (sAccessibilityStickyKeysUri.equals(uri)) {
                updateAccessibilityStickyKeysSwitch();
            }
        }
    };

    @Override
    public void onSetupKeyboardLayouts(InputDeviceIdentifier inputDeviceIdentifier) {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClass(getActivity(), Settings.KeyboardLayoutPickerActivity.class);
        intent.putExtra(KeyboardLayoutPickerFragment.EXTRA_INPUT_DEVICE_IDENTIFIER,
                inputDeviceIdentifier);
        mIntentWaitingForResult = intent;
        startActivityForResult(intent, 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (mIntentWaitingForResult != null) {
            InputDeviceIdentifier inputDeviceIdentifier = mIntentWaitingForResult
                    .getParcelableExtra(KeyboardLayoutPickerFragment.EXTRA_INPUT_DEVICE_IDENTIFIER,
                            InputDeviceIdentifier.class);
            mIntentWaitingForResult = null;
            showKeyboardLayoutDialog(inputDeviceIdentifier);
        }
    }

    private static String getLayoutLabel(@NonNull InputDevice device,
            @NonNull Context context, @NonNull InputManager im) {
        final String currentLayoutDesc =
                im.getCurrentKeyboardLayoutForInputDevice(device.getIdentifier());
        if (currentLayoutDesc == null) {
            return context.getString(R.string.keyboard_layout_default_label);
        }
        final KeyboardLayout currentLayout = im.getKeyboardLayout(currentLayoutDesc);
        if (currentLayout == null) {
            return context.getString(R.string.keyboard_layout_default_label);
        }
        // If current layout is specified but the layout is null, just return an empty string
        // instead of falling back to R.string.keyboard_layout_default_label.
        return TextUtils.emptyIfNull(currentLayout.getLabel());
    }

    @NonNull
    static List<HardKeyboardDeviceInfo> getHardKeyboards(@NonNull Context context) {
        final List<HardKeyboardDeviceInfo> keyboards = new ArrayList<>();
        final InputManager im = context.getSystemService(InputManager.class);
        if (im == null) {
            return new ArrayList<>();
        }
        for (int deviceId : InputDevice.getDeviceIds()) {
            final InputDevice device = InputDevice.getDevice(deviceId);
            if (device == null || device.isVirtual() || !device.isFullKeyboard()) {
                continue;
            }
            keyboards.add(new HardKeyboardDeviceInfo(
                    device.getName(),
                    device.getIdentifier(),
                    getLayoutLabel(device, context, im),
                    device.getBluetoothAddress(),
                    device.getVendorId(),
                    device.getProductId()));
        }

        // We intentionally don't reuse Comparator because Collator may not be thread-safe.
        final Collator collator = Collator.getInstance();
        keyboards.sort((a, b) -> {
            int result = collator.compare(a.mDeviceName, b.mDeviceName);
            if (result != 0) {
                return result;
            }
            result = a.mDeviceIdentifier.getDescriptor().compareTo(
                    b.mDeviceIdentifier.getDescriptor());
            if (result != 0) {
                return result;
            }
            return collator.compare(a.mLayoutLabel, b.mLayoutLabel);
        });
        return keyboards;
    }

    public static final class HardKeyboardDeviceInfo {
        @NonNull
        public final String mDeviceName;
        @NonNull
        public final InputDeviceIdentifier mDeviceIdentifier;
        @NonNull
        public final String mLayoutLabel;
        @Nullable
        public final String mBluetoothAddress;
        @NonNull
        public final int mVendorId;
        @NonNull
        public final int mProductId;

        public HardKeyboardDeviceInfo(
                @Nullable String deviceName,
                @NonNull InputDeviceIdentifier deviceIdentifier,
                @NonNull String layoutLabel,
                @Nullable String bluetoothAddress,
                @NonNull int vendorId,
                @NonNull int productId) {
            mDeviceName = TextUtils.emptyIfNull(deviceName);
            mDeviceIdentifier = deviceIdentifier;
            mLayoutLabel = layoutLabel;
            mBluetoothAddress = bluetoothAddress;
            mVendorId = vendorId;
            mProductId = productId;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (o == null) return false;

            if (!(o instanceof HardKeyboardDeviceInfo)) return false;

            final HardKeyboardDeviceInfo that = (HardKeyboardDeviceInfo) o;
            if (!TextUtils.equals(mDeviceName, that.mDeviceName)) {
                return false;
            }
            if (!Objects.equals(mDeviceIdentifier, that.mDeviceIdentifier)) {
                return false;
            }
            if (!TextUtils.equals(mLayoutLabel, that.mLayoutLabel)) {
                return false;
            }
            if (!TextUtils.equals(mBluetoothAddress, that.mBluetoothAddress)) {
                return false;
            }

            return true;
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.physical_keyboard_settings;
                    return Arrays.asList(sir);
                }

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return !getHardKeyboards(context).isEmpty();
                }
            };
}
