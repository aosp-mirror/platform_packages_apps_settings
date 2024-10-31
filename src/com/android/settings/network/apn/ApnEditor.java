/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.settings.network.apn;

import static com.android.settings.network.apn.ApnEditPageProviderKt.EDIT_URL;
import static com.android.settings.network.apn.ApnEditPageProviderKt.INSERT_URL;

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserManager;
import android.provider.Telephony;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.internal.util.ArrayUtils;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.spa.SpaActivity;

import java.util.Arrays;
import java.util.List;

/** Use to edit apn settings. */
public class ApnEditor extends SettingsPreferenceFragment {

    private static final String TAG = ApnEditor.class.getSimpleName();

    /**
     * APN types for data connections.  These are usage categories for an APN
     * entry.  One APN entry may support multiple APN types, eg, a single APN
     * may service regular internet traffic ("default") as well as MMS-specific
     * connections.<br/>
     * APN_TYPE_ALL is a special type to indicate that this APN entry can
     * service all data connections.
     */
    public static final String APN_TYPE_ALL = "*";
    /** APN type for default data traffic */
    public static final String APN_TYPE_DEFAULT = "default";
    /** APN type for MMS traffic */
    public static final String APN_TYPE_MMS = "mms";
    /** APN type for SUPL assisted GPS */
    public static final String APN_TYPE_SUPL = "supl";
    /** APN type for DUN traffic */
    public static final String APN_TYPE_DUN = "dun";
    /** APN type for HiPri traffic */
    public static final String APN_TYPE_HIPRI = "hipri";
    /** APN type for FOTA */
    public static final String APN_TYPE_FOTA = "fota";
    /** APN type for IMS */
    public static final String APN_TYPE_IMS = "ims";
    /** APN type for CBS */
    public static final String APN_TYPE_CBS = "cbs";
    /** APN type for IA Initial Attach APN */
    public static final String APN_TYPE_IA = "ia";
    /** APN type for Emergency PDN. This is not an IA apn, but is used
     * for access to carrier services in an emergency call situation. */
    public static final String APN_TYPE_EMERGENCY = "emergency";
    /** APN type for Mission Critical Services */
    public static final String APN_TYPE_MCX = "mcx";
    /** APN type for XCAP */
    public static final String APN_TYPE_XCAP = "xcap";
    /** APN type for OEM_PAID networks (Automotive PANS) */
    public static final String APN_TYPE_OEM_PAID = "oem_paid";
    /** APN type for OEM_PRIVATE networks (Automotive PANS) */
    public static final String APN_TYPE_OEM_PRIVATE = "oem_private";
    /** Array of all APN types */
    public static final String[] APN_TYPES = {APN_TYPE_DEFAULT,
            APN_TYPE_MMS,
            APN_TYPE_SUPL,
            APN_TYPE_DUN,
            APN_TYPE_HIPRI,
            APN_TYPE_FOTA,
            APN_TYPE_IMS,
            APN_TYPE_CBS,
            APN_TYPE_IA,
            APN_TYPE_EMERGENCY,
            APN_TYPE_MCX,
            APN_TYPE_XCAP,
            APN_TYPE_OEM_PAID,
            APN_TYPE_OEM_PRIVATE,
    };

    /** Array of APN types that are never user-editable */
    private static final String[] ALWAYS_READ_ONLY_APN_TYPES = new String[] {
        APN_TYPE_OEM_PAID,
        APN_TYPE_OEM_PRIVATE,
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        maybeRedirectToNewPage();
        finish();
    }

    private void maybeRedirectToNewPage() {
        if (isUserRestricted()) {
            Log.e(TAG, "This setting isn't available due to user restriction.");
            return;
        }

        final Intent intent = getIntent();
        final String action = intent.getAction();

        int subId =
                intent.getIntExtra(ApnSettings.SUB_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        Uri uri = intent.getData();
        if (Intent.ACTION_EDIT.equals(action)) {
            if (!uri.isPathPrefixMatch(Telephony.Carriers.CONTENT_URI)) {
                Log.e(TAG, "Edit request not for carrier table. Uri: " + uri);
            } else {
                String route = ApnEditPageProvider.INSTANCE.getRoute(EDIT_URL, uri, subId);
                SpaActivity.startSpaActivity(requireContext(), route);
            }
        } else if (Intent.ACTION_INSERT.equals(action)) {
            if (!uri.isPathPrefixMatch(Telephony.Carriers.CONTENT_URI)) {
                Log.e(TAG, "Insert request not for carrier table. Uri: " + uri);
            } else {
                String route = ApnEditPageProvider.INSTANCE.getRoute(
                        INSERT_URL, Telephony.Carriers.CONTENT_URI, subId);
                SpaActivity.startSpaActivity(getContext(), route);
            }
        }
    }

    /**
     * Fetch complete list of read only APN types.
     *
     * The list primarily comes from carrier config, but is also supplied by APN types which are
     * always read only.
     */
    static String[] getReadOnlyApnTypes(PersistableBundle b) {
        String[] carrierReadOnlyApnTypes = b.getStringArray(
                CarrierConfigManager.KEY_READ_ONLY_APN_TYPES_STRING_ARRAY);
        return ArrayUtils.concat(String.class, carrierReadOnlyApnTypes, ALWAYS_READ_ONLY_APN_TYPES);
    }

    /**
     * Check if passed in array of APN types indicates all APN types
     * @param apnTypes array of APN types. "*" indicates all types.
     * @return true if all apn types are included in the array, false otherwise
     */
    static boolean hasAllApns(String[] apnTypes) {
        if (ArrayUtils.isEmpty(apnTypes)) {
            return false;
        }

        final List apnList = Arrays.asList(apnTypes);
        if (apnList.contains(APN_TYPE_ALL)) {
            Log.d(TAG, "hasAllApns: true because apnList.contains(APN_TYPE_ALL)");
            return true;
        }
        for (String apn : APN_TYPES) {
            if (!apnList.contains(apn)) {
                return false;
            }
        }

        Log.d(TAG, "hasAllApns: true");
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.APN_EDITOR;
    }

    @VisibleForTesting
    boolean isUserRestricted() {
        UserManager userManager = getContext().getSystemService(UserManager.class);
        if (userManager == null) {
            return false;
        }
        if (!userManager.isAdminUser()) {
            Log.e(TAG, "User is not an admin");
            return true;
        }
        if (userManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)) {
            Log.e(TAG, "User is not allowed to configure mobile network");
            return true;
        }
        return false;
    }
}
