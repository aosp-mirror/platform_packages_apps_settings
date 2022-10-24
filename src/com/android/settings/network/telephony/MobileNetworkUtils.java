/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.network.telephony;

import static android.provider.Telephony.Carriers.ENFORCE_MANAGED_URI;

import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.CDMA;
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.EVDO;
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.GSM;
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.LTE;
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.NR;
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.RAF_TD_SCDMA;
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.RAF_UNKNOWN;
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.WCDMA;
import static com.android.settings.network.telephony.TelephonyConstants.TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO;
import static com.android.settings.network.telephony.TelephonyConstants.TelephonyManagerConstants.NETWORK_MODE_LTE_GSM_WCDMA;
import static com.android.settings.network.telephony.TelephonyConstants.TelephonyManagerConstants.NETWORK_MODE_NR_LTE_CDMA_EVDO;
import static com.android.settings.network.telephony.TelephonyConstants.TelephonyManagerConstants.NETWORK_MODE_NR_LTE_GSM_WCDMA;

import android.annotation.Nullable;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.euicc.EuiccManager;
import android.telephony.ims.ImsManager;
import android.telephony.ims.ImsRcsManager;
import android.telephony.ims.ProvisioningManager;
import android.telephony.ims.RcsUceAdapter;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;

import androidx.annotation.VisibleForTesting;

import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.network.CarrierConfigCache;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.ims.WifiCallingQueryImsState;
import com.android.settings.network.telephony.TelephonyConstants.TelephonyManagerConstants;
import com.android.settingslib.core.instrumentation.Instrumentable;
import com.android.settingslib.development.DevelopmentSettingsEnabler;
import com.android.settingslib.graph.SignalDrawable;
import com.android.settingslib.utils.ThreadUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MobileNetworkUtils {

    private static final String TAG = "MobileNetworkUtils";

    // CID of the device.
    private static final String KEY_CID = "ro.boot.cid";
    // CIDs of devices which should not show anything related to eSIM.
    private static final String KEY_ESIM_CID_IGNORE = "ro.setupwizard.esim_cid_ignore";
    // System Property which is used to decide whether the default eSIM UI will be shown,
    // the default value is false.
    private static final String KEY_ENABLE_ESIM_UI_BY_DEFAULT =
            "esim.enable_esim_system_ui_by_default";
    private static final String LEGACY_ACTION_CONFIGURE_PHONE_ACCOUNT =
            "android.telecom.action.CONNECTION_SERVICE_CONFIGURE";
    private static final String RTL_MARK = "\u200F";

    // The following constants are used to draw signal icon.
    public static final int NO_CELL_DATA_TYPE_ICON = 0;
    public static final Drawable EMPTY_DRAWABLE = new ColorDrawable(Color.TRANSPARENT);

    /**
     * Returns if DPC APNs are enforced.
     */
    public static boolean isDpcApnEnforced(Context context) {
        try (Cursor enforceCursor = context.getContentResolver().query(ENFORCE_MANAGED_URI,
                null, null, null, null)) {
            if (enforceCursor == null || enforceCursor.getCount() != 1) {
                return false;
            }
            enforceCursor.moveToFirst();
            return enforceCursor.getInt(0) > 0;
        }
    }

    /**
     * Returns true if Wifi calling is provisioned for the specific subscription with id
     * {@code subId}.
     */
    @VisibleForTesting
    public static boolean isWfcProvisionedOnDevice(int subId) {
        final ProvisioningManager provisioningMgr =
                ProvisioningManager.createForSubscriptionId(subId);
        if (provisioningMgr == null) {
            return true;
        }
        return provisioningMgr.getProvisioningStatusForCapability(
                MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE,
                ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN);
    }

    /**
     * @return The current user setting for whether or not contact discovery is enabled for the
     * subscription id specified.
     * @see RcsUceAdapter#isUceSettingEnabled()
     */
    public static boolean isContactDiscoveryEnabled(Context context, int subId) {
        ImsManager imsManager =
                context.getSystemService(ImsManager.class);
        return isContactDiscoveryEnabled(imsManager, subId);
    }

    /**
     * @return The current user setting for whether or not contact discovery is enabled for the
     * subscription id specified.
     * @see RcsUceAdapter#isUceSettingEnabled()
     */
    public static boolean isContactDiscoveryEnabled(ImsManager imsManager,
            int subId) {
        ImsRcsManager manager = getImsRcsManager(imsManager, subId);
        if (manager == null) return false;
        RcsUceAdapter adapter = manager.getUceAdapter();
        try {
            return adapter.isUceSettingEnabled();
        } catch (android.telephony.ims.ImsException e) {
            Log.w(TAG, "UCE service is not available: " + e.getMessage());
        }
        return false;
    }

    /**
     * Set the new user setting to enable or disable contact discovery through RCS UCE.
     * @see RcsUceAdapter#setUceSettingEnabled(boolean)
     */
    public static void setContactDiscoveryEnabled(ImsManager imsManager,
            int subId, boolean isEnabled) {
        ImsRcsManager manager = getImsRcsManager(imsManager, subId);
        if (manager == null) return;
        RcsUceAdapter adapter = manager.getUceAdapter();
        try {
            adapter.setUceSettingEnabled(isEnabled);
        } catch (android.telephony.ims.ImsException e) {
            Log.w(TAG, "UCE service is not available: " + e.getMessage());
        }
    }

    /**
     * @return The ImsRcsManager associated with the subscription specified.
     */
    private static ImsRcsManager getImsRcsManager(ImsManager imsManager,
            int subId) {
        if (imsManager == null) return null;
        try {
            return imsManager.getImsRcsManager(subId);
        } catch (Exception e) {
            Log.w(TAG, "Could not resolve ImsRcsManager: " + e.getMessage());
        }
        return null;
    }

    /**
     * @return true if contact discovery is available for the subscription specified and the option
     * should be shown to the user, false if the option should be hidden.
     */
    public static boolean isContactDiscoveryVisible(Context context, int subId) {
        CarrierConfigCache carrierConfigCache = CarrierConfigCache.getInstance(context);
        if (!carrierConfigCache.hasCarrierConfigManager()) {
            Log.w(TAG, "isContactDiscoveryVisible: Could not resolve carrier config");
            return false;
        }
        PersistableBundle bundle = carrierConfigCache.getConfigForSubId(subId);
        return bundle.getBoolean(
                CarrierConfigManager.KEY_USE_RCS_PRESENCE_BOOL, false /*default*/)
                || bundle.getBoolean(CarrierConfigManager.Ims.KEY_RCS_BULK_CAPABILITY_EXCHANGE_BOOL,
                false /*default*/);
    }

    public static Intent buildPhoneAccountConfigureIntent(
            Context context, PhoneAccountHandle accountHandle) {
        Intent intent = buildConfigureIntent(
                context, accountHandle, TelecomManager.ACTION_CONFIGURE_PHONE_ACCOUNT);

        if (intent == null) {
            // If the new configuration didn't work, try the old configuration intent.
            intent = buildConfigureIntent(context, accountHandle,
                    LEGACY_ACTION_CONFIGURE_PHONE_ACCOUNT);
        }
        return intent;
    }

    private static Intent buildConfigureIntent(
            Context context, PhoneAccountHandle accountHandle, String actionStr) {
        if (accountHandle == null || accountHandle.getComponentName() == null
                || TextUtils.isEmpty(accountHandle.getComponentName().getPackageName())) {
            return null;
        }

        // Build the settings intent.
        Intent intent = new Intent(actionStr);
        intent.setPackage(accountHandle.getComponentName().getPackageName());
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accountHandle);

        // Check to see that the phone account package can handle the setting intent.
        final PackageManager pm = context.getPackageManager();
        final List<ResolveInfo> resolutions = pm.queryIntentActivities(intent, 0);
        if (resolutions.size() == 0) {
            intent = null;  // set no intent if the package cannot handle it.
        }

        return intent;
    }

    /**
     * Whether to show the entry point to eUICC settings.
     *
     * <p>We show the entry point on any device which supports eUICC as long as either the eUICC
     * was ever provisioned (that is, at least one profile was ever downloaded onto it), or if
     * the user has enabled development mode.
     */
    public static boolean showEuiccSettings(Context context) {
        if (!SubscriptionUtil.isSimHardwareVisible(context)) {
            return false;
        }
        long timeForAccess = SystemClock.elapsedRealtime();
        try {
            Boolean isShow = ((Future<Boolean>) ThreadUtils.postOnBackgroundThread(() -> {
                        try {
                            return showEuiccSettingsDetecting(context);
                        } catch (Exception threadException) {
                            Log.w(TAG, "Accessing Euicc failure", threadException);
                        }
                        return Boolean.FALSE;
                    })).get(3, TimeUnit.SECONDS);
            return ((isShow != null) && isShow.booleanValue());
        } catch (ExecutionException | InterruptedException | TimeoutException exception) {
            timeForAccess = SystemClock.elapsedRealtime() - timeForAccess;
            Log.w(TAG, "Accessing Euicc takes too long: +" + timeForAccess + "ms");
        }
        return false;
    }

    // The same as #showEuiccSettings(Context context)
    public static Boolean showEuiccSettingsDetecting(Context context) {
        final EuiccManager euiccManager =
                (EuiccManager) context.getSystemService(EuiccManager.class);
        if (!euiccManager.isEnabled()) {
            Log.w(TAG, "EuiccManager is not enabled.");
            return false;
        }

        final ContentResolver cr = context.getContentResolver();
        final boolean esimIgnoredDevice =
                Arrays.asList(TextUtils.split(SystemProperties.get(KEY_ESIM_CID_IGNORE, ""), ","))
                        .contains(SystemProperties.get(KEY_CID));
        final boolean enabledEsimUiByDefault =
                SystemProperties.getBoolean(KEY_ENABLE_ESIM_UI_BY_DEFAULT, true);
        final boolean euiccProvisioned =
                Settings.Global.getInt(cr, Settings.Global.EUICC_PROVISIONED, 0) != 0;
        final boolean inDeveloperMode =
                DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(context);
        Log.i(TAG,
                String.format("showEuiccSettings: esimIgnoredDevice: %b, enabledEsimUiByDefault: "
                        + "%b, euiccProvisioned: %b, inDeveloperMode: %b.",
                esimIgnoredDevice, enabledEsimUiByDefault, euiccProvisioned, inDeveloperMode));
        return (euiccProvisioned
                || (!esimIgnoredDevice && inDeveloperMode)
                || (!esimIgnoredDevice && enabledEsimUiByDefault
                        && isCurrentCountrySupported(context)));
    }

    /**
     * Return {@code true} if mobile data is enabled
     */
    public static boolean isMobileDataEnabled(Context context) {
        final TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class);
        if (!telephonyManager.isDataEnabled()) {
            // Check if the data is enabled on the second SIM in the case of dual SIM.
            final TelephonyManager tmDefaultData = telephonyManager.createForSubscriptionId(
                    SubscriptionManager.getDefaultDataSubscriptionId());
            if (tmDefaultData == null || !tmDefaultData.isDataEnabled()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Set whether to enable data for {@code subId}, also whether to disable data for other
     * subscription
     */
    public static void setMobileDataEnabled(Context context, int subId, boolean enabled,
            boolean disableOtherSubscriptions) {
        final TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(subId);
        final SubscriptionManager subscriptionManager = context.getSystemService(
                SubscriptionManager.class);
        telephonyManager.setDataEnabled(enabled);

        if (disableOtherSubscriptions) {
            final List<SubscriptionInfo> subInfoList =
                    subscriptionManager.getActiveSubscriptionInfoList();
            if (subInfoList != null) {
                for (SubscriptionInfo subInfo : subInfoList) {
                    // We never disable mobile data for opportunistic subscriptions.
                    if (subInfo.getSubscriptionId() != subId && !subInfo.isOpportunistic()) {
                        context.getSystemService(TelephonyManager.class).createForSubscriptionId(
                                subInfo.getSubscriptionId()).setDataEnabled(false);
                    }
                }
            }
        }
    }

    /**
     * Return {@code true} if show CDMA category
     */
    public static boolean isCdmaOptions(Context context, int subId) {
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return false;
        }
        final PersistableBundle carrierConfig =
                CarrierConfigCache.getInstance(context).getConfigForSubId(subId);
        if (carrierConfig != null
                && !carrierConfig.getBoolean(
                CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL)
                && carrierConfig.getBoolean(CarrierConfigManager.KEY_WORLD_PHONE_BOOL)) {
            return true;
        }

        final TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(subId);
        if (telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
            return true;
        }

        if (isWorldMode(context, subId)) {
            final int settingsNetworkMode = getNetworkTypeFromRaf(
                    (int) telephonyManager.getAllowedNetworkTypesForReason(
                            TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER));

            if (settingsNetworkMode == NETWORK_MODE_LTE_GSM_WCDMA
                    || settingsNetworkMode == NETWORK_MODE_LTE_CDMA_EVDO
                    || settingsNetworkMode == NETWORK_MODE_NR_LTE_GSM_WCDMA
                    || settingsNetworkMode == NETWORK_MODE_NR_LTE_CDMA_EVDO) {
                return true;
            }

            if (shouldSpeciallyUpdateGsmCdma(context, subId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * return {@code true} if we need show Gsm related settings
     */
    public static boolean isGsmOptions(Context context, int subId) {
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return false;
        }
        if (isGsmBasicOptions(context, subId)) {
            return true;
        }
        final TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(subId);
        final int networkMode = getNetworkTypeFromRaf(
                (int) telephonyManager.getAllowedNetworkTypesForReason(
                        TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER));
        if (isWorldMode(context, subId)) {
            if (networkMode == NETWORK_MODE_LTE_CDMA_EVDO
                    || networkMode == NETWORK_MODE_LTE_GSM_WCDMA
                    || networkMode == NETWORK_MODE_NR_LTE_CDMA_EVDO
                    || networkMode == NETWORK_MODE_NR_LTE_GSM_WCDMA) {
                return true;
            } else if (shouldSpeciallyUpdateGsmCdma(context, subId)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isGsmBasicOptions(Context context, int subId) {
        final PersistableBundle carrierConfig =
                CarrierConfigCache.getInstance(context).getConfigForSubId(subId);
        if (carrierConfig != null
                && !carrierConfig.getBoolean(
                CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL)
                && carrierConfig.getBoolean(CarrierConfigManager.KEY_WORLD_PHONE_BOOL)) {
            return true;
        }

        final TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(subId);
        if (telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
            return true;
        }

        return false;
    }

    /**
     * Return {@code true} if it is world mode, and we may show advanced options in telephony
     * settings
     */
    public static boolean isWorldMode(Context context, int subId) {
        final PersistableBundle carrierConfig =
                CarrierConfigCache.getInstance(context).getConfigForSubId(subId);
        return carrierConfig == null
                ? false
                : carrierConfig.getBoolean(CarrierConfigManager.KEY_WORLD_MODE_ENABLED_BOOL);
    }

    /**
     * Return {@code true} if we need show settings for network selection(i.e. Verizon)
     */
    public static boolean shouldDisplayNetworkSelectOptions(Context context, int subId) {
        final TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(subId);
        final PersistableBundle carrierConfig =
                CarrierConfigCache.getInstance(context).getConfigForSubId(subId);
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID
                || carrierConfig == null
                || !carrierConfig.getBoolean(
                CarrierConfigManager.KEY_OPERATOR_SELECTION_EXPAND_BOOL)
                || carrierConfig.getBoolean(
                CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL)
                || (carrierConfig.getBoolean(CarrierConfigManager.KEY_CSP_ENABLED_BOOL)
                && !telephonyManager.isManualNetworkSelectionAllowed())) {
            return false;
        }

        if (isWorldMode(context, subId)) {
            final int networkMode = getNetworkTypeFromRaf(
                    (int) telephonyManager.getAllowedNetworkTypesForReason(
                            TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER));
            if (networkMode == TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO) {
                return false;
            }
            if (shouldSpeciallyUpdateGsmCdma(context, subId)) {
                return false;
            }

            if (networkMode == TelephonyManagerConstants.NETWORK_MODE_LTE_GSM_WCDMA) {
                return true;
            }
        }

        return isGsmBasicOptions(context, subId);
    }

    /**
     * Return {@code true} if Tdscdma is supported in current subscription
     */
    public static boolean isTdscdmaSupported(Context context, int subId) {
        return isTdscdmaSupported(context,
                context.getSystemService(TelephonyManager.class).createForSubscriptionId(subId));
    }

    //TODO(b/117651939): move it to telephony
    private static boolean isTdscdmaSupported(Context context, TelephonyManager telephonyManager) {
        final PersistableBundle carrierConfig = CarrierConfigCache.getInstance(context).getConfig();

        if (carrierConfig == null) {
            return false;
        }

        if (carrierConfig.getBoolean(CarrierConfigManager.KEY_SUPPORT_TDSCDMA_BOOL)) {
            return true;
        }
        final String[] numericArray = carrierConfig.getStringArray(
                CarrierConfigManager.KEY_SUPPORT_TDSCDMA_ROAMING_NETWORKS_STRING_ARRAY);
        if (numericArray == null) {
            return false;
        }
        final ServiceState serviceState = telephonyManager.getServiceState();
        final String operatorNumeric =
                (serviceState != null) ? serviceState.getOperatorNumeric() : null;
        if (operatorNumeric == null) {
            return false;
        }
        for (String numeric : numericArray) {
            if (operatorNumeric.equals(numeric)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return subId that supported by search. If there are more than one, return first one,
     * otherwise return {@link SubscriptionManager#INVALID_SUBSCRIPTION_ID}
     */
    public static int getSearchableSubscriptionId(Context context) {
        final int[] subIds = getActiveSubscriptionIdList(context);

        return subIds.length >= 1 ? subIds[0] : SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }

    /**
     * Return availability for a default subscription id. If subId already been set, use it to
     * check, otherwise traverse all active subIds on device to check.
     * @param context context
     * @param defSubId Default subId get from telephony preference controller
     * @param callback Callback to check availability for a specific subId
     * @return Availability
     *
     * @see BasePreferenceController#getAvailabilityStatus()
     */
    public static int getAvailability(Context context, int defSubId,
            TelephonyAvailabilityCallback callback) {
        if (defSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            // If subId has been set, return the corresponding status
            return callback.getAvailabilityStatus(defSubId);
        } else {
            // Otherwise, search whether there is one subId in device that support this preference
            final int[] subIds = getActiveSubscriptionIdList(context);
            if (ArrayUtils.isEmpty(subIds)) {
                return callback.getAvailabilityStatus(
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            } else {
                for (final int subId : subIds) {
                    final int status = callback.getAvailabilityStatus(subId);
                    if (status == BasePreferenceController.AVAILABLE) {
                        return status;
                    }
                }
                return callback.getAvailabilityStatus(subIds[0]);
            }
        }
    }

    /**
     * This method is migrated from {@link com.android.phone.MobileNetworkSettings} and we should
     * use it carefully. This code snippet doesn't have very clear meaning however we should
     * update GSM or CDMA differently based on what it returns.
     *
     * 1. For all CDMA settings, make them visible if it return {@code true}
     * 2. For GSM settings, make them visible if it return {@code true} unless 3
     * 3. For network select settings, make it invisible if it return {@code true}
     */
    @VisibleForTesting
    static boolean shouldSpeciallyUpdateGsmCdma(Context context, int subId) {
        if (!isWorldMode(context, subId)) {
            return false;
        }
        final TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(subId);
        final int networkMode = getNetworkTypeFromRaf(
                (int) telephonyManager.getAllowedNetworkTypesForReason(
                        TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER));
        if (networkMode == TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_GSM
                || networkMode == TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA
                || networkMode == TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA
                || networkMode == TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_WCDMA
                || networkMode
                == TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA
                || networkMode == TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA) {
            if (!isTdscdmaSupported(context, subId)) {
                return true;
            }
        }

        return false;
    }

    public static Drawable getSignalStrengthIcon(Context context, int level, int numLevels,
            int iconType, boolean cutOut) {
        final SignalDrawable signalDrawable = new SignalDrawable(context);
        signalDrawable.setLevel(
                SignalDrawable.getState(level, numLevels, cutOut));

        // Make the network type drawable
        final Drawable networkDrawable =
                iconType == NO_CELL_DATA_TYPE_ICON
                        ? EMPTY_DRAWABLE
                        : context.getResources().getDrawable(iconType, context.getTheme());

        // Overlay the two drawables
        final Drawable[] layers = {networkDrawable, signalDrawable};
        final int iconSize =
                context.getResources().getDimensionPixelSize(R.dimen.signal_strength_icon_size);

        final LayerDrawable icons = new LayerDrawable(layers);
        // Set the network type icon at the top left
        icons.setLayerGravity(0 /* index of networkDrawable */, Gravity.TOP | Gravity.LEFT);
        // Set the signal strength icon at the bottom right
        icons.setLayerGravity(1 /* index of SignalDrawable */, Gravity.BOTTOM | Gravity.RIGHT);
        icons.setLayerSize(1 /* index of SignalDrawable */, iconSize, iconSize);
        icons.setTintList(Utils.getColorAttr(context, android.R.attr.colorControlNormal));
        return icons;
    }

    /**
     * This method is migrated from
     * {@link android.telephony.TelephonyManager.getNetworkOperatorName}. Which provides
     *
     * 1. Better support under multi-SIM environment.
     * 2. Similar design which aligned with operator name displayed in status bar
     */
    public static CharSequence getCurrentCarrierNameForDisplay(Context context, int subId) {
        final SubscriptionManager sm = context.getSystemService(SubscriptionManager.class);
        if (sm != null) {
            final SubscriptionInfo subInfo = getSubscriptionInfo(sm, subId);
            if (subInfo != null) {
                return subInfo.getCarrierName();
            }
        }
        return getOperatorNameFromTelephonyManager(context);
    }

    public static CharSequence getCurrentCarrierNameForDisplay(Context context) {
        final SubscriptionManager sm = context.getSystemService(SubscriptionManager.class);
        if (sm != null) {
            final int subId = sm.getDefaultSubscriptionId();
            final SubscriptionInfo subInfo = getSubscriptionInfo(sm, subId);
            if (subInfo != null) {
                return subInfo.getCarrierName();
            }
        }
        return getOperatorNameFromTelephonyManager(context);
    }

    private static SubscriptionInfo getSubscriptionInfo(SubscriptionManager subManager, int subId) {
        List<SubscriptionInfo> subInfos = subManager.getActiveSubscriptionInfoList();
        if (subInfos == null) {
            return null;
        }
        for (SubscriptionInfo subInfo : subInfos) {
            if (subInfo.getSubscriptionId() == subId) {
                return subInfo;
            }
        }
        return null;
    }

    private static String getOperatorNameFromTelephonyManager(Context context) {
        final TelephonyManager tm =
                (TelephonyManager) context.getSystemService(TelephonyManager.class);
        if (tm == null) {
            return null;
        }
        return tm.getNetworkOperatorName();
    }

    private static int[] getActiveSubscriptionIdList(Context context) {
        final SubscriptionManager subscriptionManager = context.getSystemService(
                SubscriptionManager.class);
        final List<SubscriptionInfo> subInfoList =
                subscriptionManager.getActiveSubscriptionInfoList();
        if (subInfoList == null) {
            return new int[0];
        }
        int[] activeSubIds = new int[subInfoList.size()];
        int i = 0;
        for (SubscriptionInfo subInfo : subInfoList) {
            activeSubIds[i] = subInfo.getSubscriptionId();
            i++;
        }
        return activeSubIds;
    }

    /**
     * Loop through all the device logical slots to check whether the user's current country
     * supports eSIM.
     */
    private static boolean isCurrentCountrySupported(Context context) {
        final EuiccManager em = (EuiccManager) context.getSystemService(EuiccManager.class);
        final TelephonyManager tm =
                (TelephonyManager) context.getSystemService(TelephonyManager.class);

        Set<String> countrySet = new HashSet<>();
        for (int i = 0; i < tm.getPhoneCount(); i++) {
            String countryCode = tm.getNetworkCountryIso(i);
            if (!TextUtils.isEmpty(countryCode)) {
                countrySet.add(countryCode);
            }
        }
        boolean isSupported = countrySet.stream().anyMatch(em::isSupportedCountry);
        Log.i(TAG, "isCurrentCountrySupported countryCodes: " + countrySet
                + " eSIMSupported: " + isSupported);
        return isSupported;
    }

    /**
     *  Imported from {@link android.telephony.RadioAccessFamily}
     */
    public static long getRafFromNetworkType(int type) {
        switch (type) {
            case TelephonyManagerConstants.NETWORK_MODE_WCDMA_PREF:
                return GSM | WCDMA;
            case TelephonyManagerConstants.NETWORK_MODE_GSM_ONLY:
                return GSM;
            case TelephonyManagerConstants.NETWORK_MODE_WCDMA_ONLY:
                return WCDMA;
            case TelephonyManagerConstants.NETWORK_MODE_GSM_UMTS:
                return GSM | WCDMA;
            case TelephonyManagerConstants.NETWORK_MODE_CDMA_EVDO:
                return CDMA | EVDO;
            case TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO:
                return LTE | CDMA | EVDO;
            case TelephonyManagerConstants.NETWORK_MODE_LTE_GSM_WCDMA:
                return LTE | GSM | WCDMA;
            case TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                return LTE | CDMA | EVDO | GSM | WCDMA;
            case TelephonyManagerConstants.NETWORK_MODE_LTE_ONLY:
                return LTE;
            case TelephonyManagerConstants.NETWORK_MODE_LTE_WCDMA:
                return LTE | WCDMA;
            case TelephonyManagerConstants.NETWORK_MODE_CDMA_NO_EVDO:
                return CDMA;
            case TelephonyManagerConstants.NETWORK_MODE_EVDO_NO_CDMA:
                return EVDO;
            case TelephonyManagerConstants.NETWORK_MODE_GLOBAL:
                return GSM | WCDMA | CDMA | EVDO;
            case TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_ONLY:
                return RAF_TD_SCDMA;
            case TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_WCDMA:
                return RAF_TD_SCDMA | WCDMA;
            case TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA:
                return LTE | RAF_TD_SCDMA;
            case TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_GSM:
                return RAF_TD_SCDMA | GSM;
            case TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_GSM:
                return LTE | RAF_TD_SCDMA | GSM;
            case TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_GSM_WCDMA:
                return RAF_TD_SCDMA | GSM | WCDMA;
            case TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_WCDMA:
                return LTE | RAF_TD_SCDMA | WCDMA;
            case TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA:
                return LTE | RAF_TD_SCDMA | GSM | WCDMA;
            case TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                return RAF_TD_SCDMA | CDMA | EVDO | GSM | WCDMA;
            case TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                return LTE | RAF_TD_SCDMA | CDMA | EVDO | GSM | WCDMA;
            case (TelephonyManagerConstants.NETWORK_MODE_NR_ONLY):
                return NR;
            case (TelephonyManagerConstants.NETWORK_MODE_NR_LTE):
                return NR | LTE;
            case (TelephonyManagerConstants.NETWORK_MODE_NR_LTE_CDMA_EVDO):
                return NR | LTE | CDMA | EVDO;
            case (TelephonyManagerConstants.NETWORK_MODE_NR_LTE_GSM_WCDMA):
                return NR | LTE | GSM | WCDMA;
            case (TelephonyManagerConstants.NETWORK_MODE_NR_LTE_CDMA_EVDO_GSM_WCDMA):
                return NR | LTE | CDMA | EVDO | GSM | WCDMA;
            case (TelephonyManagerConstants.NETWORK_MODE_NR_LTE_WCDMA):
                return NR | LTE | WCDMA;
            case (TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA):
                return NR | LTE | RAF_TD_SCDMA;
            case (TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_GSM):
                return NR | LTE | RAF_TD_SCDMA | GSM;
            case (TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_WCDMA):
                return NR | LTE | RAF_TD_SCDMA | WCDMA;
            case (TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_GSM_WCDMA):
                return NR | LTE | RAF_TD_SCDMA | GSM | WCDMA;
            case (TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA):
                return NR | LTE | RAF_TD_SCDMA | CDMA | EVDO | GSM | WCDMA;
            default:
                return RAF_UNKNOWN;
        }
    }

    /**
     *  Imported from {@link android.telephony.RadioAccessFamily}
     */
    public static int getNetworkTypeFromRaf(int raf) {
        raf = getAdjustedRaf(raf);

        switch (raf) {
            case (GSM | WCDMA):
                return TelephonyManagerConstants.NETWORK_MODE_WCDMA_PREF;
            case GSM:
                return TelephonyManagerConstants.NETWORK_MODE_GSM_ONLY;
            case WCDMA:
                return TelephonyManagerConstants.NETWORK_MODE_WCDMA_ONLY;
            case (CDMA | EVDO):
                return TelephonyManagerConstants.NETWORK_MODE_CDMA_EVDO;
            case (LTE | CDMA | EVDO):
                return TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO;
            case (LTE | GSM | WCDMA):
                return TelephonyManagerConstants.NETWORK_MODE_LTE_GSM_WCDMA;
            case (LTE | CDMA | EVDO | GSM | WCDMA):
                return TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA;
            case LTE:
                return TelephonyManagerConstants.NETWORK_MODE_LTE_ONLY;
            case (LTE | WCDMA):
                return TelephonyManagerConstants.NETWORK_MODE_LTE_WCDMA;
            case CDMA:
                return TelephonyManagerConstants.NETWORK_MODE_CDMA_NO_EVDO;
            case EVDO:
                return TelephonyManagerConstants.NETWORK_MODE_EVDO_NO_CDMA;
            case (GSM | WCDMA | CDMA | EVDO):
                return TelephonyManagerConstants.NETWORK_MODE_GLOBAL;
            case RAF_TD_SCDMA:
                return TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_ONLY;
            case (RAF_TD_SCDMA | WCDMA):
                return TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_WCDMA;
            case (LTE | RAF_TD_SCDMA):
                return TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA;
            case (RAF_TD_SCDMA | GSM):
                return TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_GSM;
            case (LTE | RAF_TD_SCDMA | GSM):
                return TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_GSM;
            case (RAF_TD_SCDMA | GSM | WCDMA):
                return TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_GSM_WCDMA;
            case (LTE | RAF_TD_SCDMA | WCDMA):
                return TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_WCDMA;
            case (LTE | RAF_TD_SCDMA | GSM | WCDMA):
                return TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA;
            case (RAF_TD_SCDMA | CDMA | EVDO | GSM | WCDMA):
                return TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA;
            case (LTE | RAF_TD_SCDMA | CDMA | EVDO | GSM | WCDMA):
                return TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA;
            case (NR):
                return TelephonyManagerConstants.NETWORK_MODE_NR_ONLY;
            case (NR | LTE):
                return TelephonyManagerConstants.NETWORK_MODE_NR_LTE;
            case (NR | LTE | CDMA | EVDO):
                return TelephonyManagerConstants.NETWORK_MODE_NR_LTE_CDMA_EVDO;
            case (NR | LTE | GSM | WCDMA):
                return TelephonyManagerConstants.NETWORK_MODE_NR_LTE_GSM_WCDMA;
            case (NR | LTE | CDMA | EVDO | GSM | WCDMA):
                return TelephonyManagerConstants.NETWORK_MODE_NR_LTE_CDMA_EVDO_GSM_WCDMA;
            case (NR | LTE | WCDMA):
                return TelephonyManagerConstants.NETWORK_MODE_NR_LTE_WCDMA;
            case (NR | LTE | RAF_TD_SCDMA):
                return TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA;
            case (NR | LTE | RAF_TD_SCDMA | GSM):
                return TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_GSM;
            case (NR | LTE | RAF_TD_SCDMA | WCDMA):
                return TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_WCDMA;
            case (NR | LTE | RAF_TD_SCDMA | GSM | WCDMA):
                return TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_GSM_WCDMA;
            case (NR | LTE | RAF_TD_SCDMA | CDMA | EVDO | GSM | WCDMA):
                return TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA;
            default:
                return TelephonyManagerConstants.NETWORK_MODE_UNKNOWN;
        }
    }

    /**
     *  Imported from {@link android.telephony.RadioAccessFamily}
     */
    private static int getAdjustedRaf(int raf) {
        raf = ((GSM & raf) > 0) ? (GSM | raf) : raf;
        raf = ((WCDMA & raf) > 0) ? (WCDMA | raf) : raf;
        raf = ((CDMA & raf) > 0) ? (CDMA | raf) : raf;
        raf = ((EVDO & raf) > 0) ? (EVDO | raf) : raf;
        raf = ((LTE & raf) > 0) ? (LTE | raf) : raf;
        raf = ((NR & raf) > 0) ? (NR | raf) : raf;
        return raf;
    }

    /**
     * Copied from SubscriptionsPreferenceController#activeNetworkIsCellular()
     */
    public static boolean activeNetworkIsCellular(Context context) {
        final ConnectivityManager connectivityManager =
                context.getSystemService(ConnectivityManager.class);
        final Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            return false;
        }
        final NetworkCapabilities networkCapabilities =
                connectivityManager.getNetworkCapabilities(activeNetwork);
        if (networkCapabilities == null) {
            return false;
        }
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
    }

    /**
     * Copied from WifiCallingPreferenceController#isWifiCallingEnabled()
     */
    public static boolean isWifiCallingEnabled(Context context, int subId,
            @Nullable WifiCallingQueryImsState queryImsState,
            @Nullable PhoneAccountHandle phoneAccountHandle) {
        if (phoneAccountHandle == null){
            phoneAccountHandle = context.getSystemService(TelecomManager.class)
                    .getSimCallManagerForSubscription(subId);
        }
        boolean isWifiCallingEnabled;
        if (phoneAccountHandle != null) {
            final Intent intent = buildPhoneAccountConfigureIntent(context, phoneAccountHandle);
            isWifiCallingEnabled = intent != null;
        } else {
            if (queryImsState == null) {
                queryImsState = new WifiCallingQueryImsState(context, subId);
            }
            isWifiCallingEnabled = queryImsState.isReadyToWifiCalling();
        }
        return isWifiCallingEnabled;
    }


    /**
     * Returns preferred status of Calls & SMS separately when Provider Model is enabled.
     */
    public static CharSequence getPreferredStatus(boolean isRtlMode, Context context,
            SubscriptionManager subscriptionManager, boolean isPreferredCallStatus) {
        final List<SubscriptionInfo> subs = SubscriptionUtil.getActiveSubscriptions(
                subscriptionManager);
        if (!subs.isEmpty()) {
            final StringBuilder summary = new StringBuilder();
            for (SubscriptionInfo subInfo : subs) {
                int subsSize = subs.size();
                final CharSequence displayName = SubscriptionUtil.getUniqueSubscriptionDisplayName(
                        subInfo, context);

                // Set displayName as summary if there is only one valid SIM.
                if (subsSize == 1
                        && SubscriptionManager.isValidSubscriptionId(subInfo.getSubscriptionId())) {
                    return displayName;
                }

                CharSequence status = isPreferredCallStatus
                        ? getPreferredCallStatus(context, subInfo)
                        : getPreferredSmsStatus(context, subInfo);
                if (status.toString().isEmpty()) {
                    // If there are 2 or more SIMs and one of these has no preferred status,
                    // set only its displayName as summary.
                    summary.append(displayName);
                } else {
                    summary.append(displayName)
                            .append(" (")
                            .append(status)
                            .append(")");
                }
                // Do not add ", " for the last subscription.
                if (subInfo != subs.get(subs.size() - 1)) {
                    summary.append(", ");
                }

                if (isRtlMode) {
                    summary.insert(0, RTL_MARK).insert(summary.length(), RTL_MARK);
                }
            }
            return summary;
        } else {
            return "";
        }
    }

    private static CharSequence getPreferredCallStatus(Context context, SubscriptionInfo subInfo) {
        final int subId = subInfo.getSubscriptionId();
        String status = "";
        boolean isDataPreferred = subId == SubscriptionManager.getDefaultVoiceSubscriptionId();

        if (isDataPreferred) {
            status = setSummaryResId(context, R.string.calls_sms_preferred);
        }

        return status;
    }

    private static CharSequence getPreferredSmsStatus(Context context, SubscriptionInfo subInfo) {
        final int subId = subInfo.getSubscriptionId();
        String status = "";
        boolean isSmsPreferred = subId == SubscriptionManager.getDefaultSmsSubscriptionId();

        if (isSmsPreferred) {
            status = setSummaryResId(context, R.string.calls_sms_preferred);
        }

        return status;
    }

    private static String setSummaryResId(Context context, int resId) {
        return context.getResources().getString(resId);
    }

    public static void launchMobileNetworkSettings(Context context, SubscriptionInfo info) {
        final int subId = info.getSubscriptionId();
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            Log.d(TAG, "launchMobileNetworkSettings fail, subId is invalid.");
            return;
        }

        Log.d(TAG, "launchMobileNetworkSettings for subId: " + subId);
        final Bundle extra = new Bundle();
        extra.putInt(Settings.EXTRA_SUB_ID, subId);
        new SubSettingLauncher(context)
                .setTitleText(SubscriptionUtil.getUniqueSubscriptionDisplayName(info, context))
                .setDestination(MobileNetworkSettings.class.getCanonicalName())
                .setSourceMetricsCategory(Instrumentable.METRICS_CATEGORY_UNKNOWN)
                .setArguments(extra)
                .launch();
    }

}
