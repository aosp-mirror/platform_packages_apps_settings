/**
 * Copyright (C) 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.android.settings;

import static android.content.Intent.EXTRA_USER;
import static android.content.Intent.EXTRA_USER_ID;
import static android.media.MediaRoute2Info.TYPE_GROUP;
import static android.media.MediaRoute2Info.TYPE_REMOTE_SPEAKER;
import static android.media.MediaRoute2Info.TYPE_REMOTE_TV;
import static android.media.MediaRoute2Info.TYPE_UNKNOWN;
import static android.text.format.DateUtils.FORMAT_ABBREV_MONTH;
import static android.text.format.DateUtils.FORMAT_SHOW_DATE;

import android.annotation.Nullable;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.IntentFilterVerificationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;
import android.media.MediaRoute2Info;
import android.media.MediaRouter2Manager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.preference.PreferenceFrameLayout;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Profile;
import android.provider.ContactsContract.RawContacts;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.TtsSpan;
import android.util.ArraySet;
import android.util.IconDrawableFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TabWidget;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.android.internal.app.UnlaunchableAppActivity;
import com.android.internal.util.ArrayUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.dashboard.profileselector.ProfileFragmentBridge;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settingslib.widget.ActionBarShadowController;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public final class Utils extends com.android.settingslib.Utils {

    private static final String TAG = "Settings";

    public static final String FILE_PROVIDER_AUTHORITY = "com.android.settings.files";

    /**
     * Set the preference's title to the matching activity's label.
     */
    public static final int UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY = 1;

    public static final String SETTINGS_PACKAGE_NAME = "com.android.settings";

    public static final String OS_PKG = "os";

    /**
     * Whether to disable the new device identifier access restrictions.
     */
    public static final String PROPERTY_DEVICE_IDENTIFIER_ACCESS_RESTRICTIONS_DISABLED =
            "device_identifier_access_restrictions_disabled";

    /**
     * Whether to show the Permissions Hub.
     */
    public static final String PROPERTY_PERMISSIONS_HUB_ENABLED = "permissions_hub_enabled";

    /**
     * Finds a matching activity for a preference's intent. If a matching
     * activity is not found, it will remove the preference.
     *
     * @param context The context.
     * @param parentPreferenceGroup The preference group that contains the
     *            preference whose intent is being resolved.
     * @param preferenceKey The key of the preference whose intent is being
     *            resolved.
     * @param flags 0 or one or more of
     *            {@link #UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY}
     *            .
     * @return Whether an activity was found. If false, the preference was
     *         removed.
     */
    public static boolean updatePreferenceToSpecificActivityOrRemove(Context context,
            PreferenceGroup parentPreferenceGroup, String preferenceKey, int flags) {

        final Preference preference = parentPreferenceGroup.findPreference(preferenceKey);
        if (preference == null) {
            return false;
        }

        final Intent intent = preference.getIntent();
        if (intent != null) {
            // Find the activity that is in the system image
            final PackageManager pm = context.getPackageManager();
            final List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
            final int listSize = list.size();
            for (int i = 0; i < listSize; i++) {
                final ResolveInfo resolveInfo = list.get(i);
                if ((resolveInfo.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)
                        != 0) {

                    // Replace the intent with this specific activity
                    preference.setIntent(new Intent().setClassName(
                            resolveInfo.activityInfo.packageName,
                            resolveInfo.activityInfo.name));

                    if ((flags & UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY) != 0) {
                        // Set the preference title to the activity's label
                        preference.setTitle(resolveInfo.loadLabel(pm));
                    }

                    return true;
                }
            }
        }

        // Did not find a matching activity, so remove the preference
        parentPreferenceGroup.removePreference(preference);

        return false;
    }

    /**
     * Returns true if Monkey is running.
     */
    public static boolean isMonkeyRunning() {
        return ActivityManager.isUserAMonkey();
    }

    /**
     * Returns whether the device is voice-capable (meaning, it is also a phone).
     */
    public static boolean isVoiceCapable(Context context) {
        final TelephonyManager telephony =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephony != null && telephony.isVoiceCapable();
    }

    /**
     * Returns the WIFI IP Addresses, if any, taking into account IPv4 and IPv6 style addresses.
     * @param context the application context
     * @return the formatted and newline-separated IP addresses, or null if none.
     */
    public static String getWifiIpAddresses(Context context) {
        final WifiManager wifiManager = context.getSystemService(WifiManager.class);
        final Network currentNetwork = wifiManager.getCurrentNetwork();
        if (currentNetwork != null) {
            final ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final LinkProperties prop = cm.getLinkProperties(currentNetwork);
            return formatIpAddresses(prop);
        }
        return null;
    }

    private static String formatIpAddresses(LinkProperties prop) {
        if (prop == null) return null;
        final Iterator<InetAddress> iter = prop.getAllAddresses().iterator();
        // If there are no entries, return null
        if (!iter.hasNext()) return null;
        // Concatenate all available addresses, comma separated
        String addresses = "";
        while (iter.hasNext()) {
            addresses += iter.next().getHostAddress();
            if (iter.hasNext()) addresses += "\n";
        }
        return addresses;
    }

    public static Locale createLocaleFromString(String localeStr) {
        // TODO: is there a better way to actually construct a locale that will match?
        // The main problem is, on top of Java specs, locale.toString() and
        // new Locale(locale.toString()).toString() do not return equal() strings in
        // many cases, because the constructor takes the only string as the language
        // code. So : new Locale("en", "US").toString() => "en_US"
        // And : new Locale("en_US").toString() => "en_us"
        if (null == localeStr)
            return Locale.getDefault();
        final String[] brokenDownLocale = localeStr.split("_", 3);
        // split may not return a 0-length array.
        if (1 == brokenDownLocale.length) {
            return new Locale(brokenDownLocale[0]);
        } else if (2 == brokenDownLocale.length) {
            return new Locale(brokenDownLocale[0], brokenDownLocale[1]);
        } else {
            return new Locale(brokenDownLocale[0], brokenDownLocale[1], brokenDownLocale[2]);
        }
    }

    public static boolean isBatteryPresent(Intent batteryChangedIntent) {
        return batteryChangedIntent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true);
    }

    /**
     * Return true if battery is present.
     */
    public static boolean isBatteryPresent(Context context) {
        Intent batteryBroadcast = context.registerReceiver(null /* receiver */,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return isBatteryPresent(batteryBroadcast);
    }

    public static String getBatteryPercentage(Intent batteryChangedIntent) {
        return formatPercentage(getBatteryLevel(batteryChangedIntent));
    }

    /**
     * Prepare a custom preferences layout, moving padding to {@link ListView}
     * when outside scrollbars are requested. Usually used to display
     * {@link ListView} and {@link TabWidget} with correct padding.
     */
    public static void prepareCustomPreferencesList(
            ViewGroup parent, View child, View list, boolean ignoreSidePadding) {
        final boolean movePadding = list.getScrollBarStyle() == View.SCROLLBARS_OUTSIDE_OVERLAY;
        if (movePadding) {
            final Resources res = list.getResources();
            final int paddingBottom = res.getDimensionPixelSize(
                    com.android.internal.R.dimen.preference_fragment_padding_bottom);

            if (parent instanceof PreferenceFrameLayout) {
                ((PreferenceFrameLayout.LayoutParams) child.getLayoutParams()).removeBorders = true;
            }
            list.setPaddingRelative(0 /* start */, 0 /* top */, 0 /* end */, paddingBottom);
        }
    }

    public static void forceCustomPadding(View view, boolean additive) {
        final Resources res = view.getResources();

        final int paddingStart = additive ? view.getPaddingStart() : 0;
        final int paddingEnd = additive ? view.getPaddingEnd() : 0;
        final int paddingBottom = res.getDimensionPixelSize(
                com.android.internal.R.dimen.preference_fragment_padding_bottom);

        view.setPaddingRelative(paddingStart, 0, paddingEnd, paddingBottom);
    }

    public static String getMeProfileName(Context context, boolean full) {
        if (full) {
            return getProfileDisplayName(context);
        } else {
            return getShorterNameIfPossible(context);
        }
    }

    private static String getShorterNameIfPossible(Context context) {
        final String given = getLocalProfileGivenName(context);
        return !TextUtils.isEmpty(given) ? given : getProfileDisplayName(context);
    }

    private static String getLocalProfileGivenName(Context context) {
        final ContentResolver cr = context.getContentResolver();

        // Find the raw contact ID for the local ME profile raw contact.
        final long localRowProfileId;
        final Cursor localRawProfile = cr.query(
                Profile.CONTENT_RAW_CONTACTS_URI,
                new String[] {RawContacts._ID},
                RawContacts.ACCOUNT_TYPE + " IS NULL AND " +
                        RawContacts.ACCOUNT_NAME + " IS NULL",
                null, null);
        if (localRawProfile == null) return null;

        try {
            if (!localRawProfile.moveToFirst()) {
                return null;
            }
            localRowProfileId = localRawProfile.getLong(0);
        } finally {
            localRawProfile.close();
        }

        // Find the structured name for the raw contact.
        final Cursor structuredName = cr.query(
                Profile.CONTENT_URI.buildUpon().appendPath(Contacts.Data.CONTENT_DIRECTORY).build(),
                new String[] {CommonDataKinds.StructuredName.GIVEN_NAME,
                    CommonDataKinds.StructuredName.FAMILY_NAME},
                Data.RAW_CONTACT_ID + "=" + localRowProfileId,
                null, null);
        if (structuredName == null) return null;

        try {
            if (!structuredName.moveToFirst()) {
                return null;
            }
            String partialName = structuredName.getString(0);
            if (TextUtils.isEmpty(partialName)) {
                partialName = structuredName.getString(1);
            }
            return partialName;
        } finally {
            structuredName.close();
        }
    }

    private static final String getProfileDisplayName(Context context) {
        final ContentResolver cr = context.getContentResolver();
        final Cursor profile = cr.query(Profile.CONTENT_URI,
                new String[] {Profile.DISPLAY_NAME}, null, null, null);
        if (profile == null) return null;

        try {
            if (!profile.moveToFirst()) {
                return null;
            }
            return profile.getString(0);
        } finally {
            profile.close();
        }
    }

    public static boolean hasMultipleUsers(Context context) {
        return context.getSystemService(UserManager.class)
                .getUsers().size() > 1;
    }

    /**
     * Returns the managed profile of the current user or {@code null} if none is found or a profile
     * exists but it is disabled.
     */
    public static UserHandle getManagedProfile(UserManager userManager) {
        final List<UserHandle> userProfiles = userManager.getUserProfiles();
        for (UserHandle profile : userProfiles) {
            if (profile.getIdentifier() == userManager.getUserHandle()) {
                continue;
            }
            final UserInfo userInfo = userManager.getUserInfo(profile.getIdentifier());
            if (userInfo.isManagedProfile()) {
                return profile;
            }
        }
        return null;
    }

    /**
     * Returns the managed profile of the current user or {@code null} if none is found. Unlike
     * {@link #getManagedProfile} this method returns enabled and disabled managed profiles.
     */
    public static UserHandle getManagedProfileWithDisabled(UserManager userManager) {
        // TODO: Call getManagedProfileId from here once Robolectric supports
        // API level 24 and UserManager.getProfileIdsWithDisabled can be Mocked (to avoid having
        // yet another implementation that loops over user profiles in this method). In the meantime
        // we need to use UserManager.getProfiles that is available on API 23 (the one currently
        // used for Settings Robolectric tests).
        final int myUserId = UserHandle.myUserId();
        final List<UserInfo> profiles = userManager.getProfiles(myUserId);
        final int count = profiles.size();
        for (int i = 0; i < count; i++) {
            final UserInfo profile = profiles.get(i);
            if (profile.isManagedProfile()
                    && profile.getUserHandle().getIdentifier() != myUserId) {
                return profile.getUserHandle();
            }
        }
        return null;
    }

    /**
     * Retrieves the id for the given user's managed profile.
     *
     * @return the managed profile id or UserHandle.USER_NULL if there is none.
     */
    public static int getManagedProfileId(UserManager um, int parentUserId) {
        final int[] profileIds = um.getProfileIdsWithDisabled(parentUserId);
        for (int profileId : profileIds) {
            if (profileId != parentUserId) {
                return profileId;
            }
        }
        return UserHandle.USER_NULL;
    }

    /**
     * Returns the target user for a Settings activity.
     * <p>
     * User would be retrieved in this order:
     * <ul>
     * <li> If this activity is launched from other user, return that user id.
     * <li> If this is launched from the Settings app in same user, return the user contained as an
     *      extra in the arguments or intent extras.
     * <li> Otherwise, return UserHandle.myUserId().
     * </ul>
     * <p>
     * Note: This is secure in the sense that it only returns a target user different to the current
     * one if the app launching this activity is the Settings app itself, running in the same user
     * or in one that is in the same profile group, or if the user id is provided by the system.
     */
    public static UserHandle getSecureTargetUser(IBinder activityToken,
            UserManager um, @Nullable Bundle arguments, @Nullable Bundle intentExtras) {
        final UserHandle currentUser = new UserHandle(UserHandle.myUserId());
        final IActivityManager am = ActivityManager.getService();
        try {
            final String launchedFromPackage = am.getLaunchedFromPackage(activityToken);
            final boolean launchedFromSettingsApp =
                    SETTINGS_PACKAGE_NAME.equals(launchedFromPackage);

            final UserHandle launchedFromUser = new UserHandle(UserHandle.getUserId(
                    am.getLaunchedFromUid(activityToken)));
            if (launchedFromUser != null && !launchedFromUser.equals(currentUser)) {
                // Check it's secure
                if (isProfileOf(um, launchedFromUser)) {
                    return launchedFromUser;
                }
            }
            final UserHandle extrasUser = getUserHandleFromBundle(intentExtras);
            if (extrasUser != null && !extrasUser.equals(currentUser)) {
                // Check it's secure
                if (launchedFromSettingsApp && isProfileOf(um, extrasUser)) {
                    return extrasUser;
                }
            }
            final UserHandle argumentsUser = getUserHandleFromBundle(arguments);
            if (argumentsUser != null && !argumentsUser.equals(currentUser)) {
                // Check it's secure
                if (launchedFromSettingsApp && isProfileOf(um, argumentsUser)) {
                    return argumentsUser;
                }
            }
        } catch (RemoteException e) {
            // Should not happen
            Log.v(TAG, "Could not talk to activity manager.", e);
        }
        return currentUser;
    }

    /**
     * Lookup both {@link Intent#EXTRA_USER} and {@link Intent#EXTRA_USER_ID} in the bundle
     * and return the {@link UserHandle} object. Return {@code null} if nothing is found.
     */
    private static @Nullable UserHandle getUserHandleFromBundle(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        final UserHandle user = bundle.getParcelable(EXTRA_USER);
        if (user != null) {
            return user;
        }
        final int userId = bundle.getInt(EXTRA_USER_ID, -1);
        if (userId != -1) {
            return UserHandle.of(userId);
        }
        return null;
    }

   /**
    * Returns true if the user provided is in the same profiles group as the current user.
    */
   private static boolean isProfileOf(UserManager um, UserHandle otherUser) {
       if (um == null || otherUser == null) return false;
       return (UserHandle.myUserId() == otherUser.getIdentifier())
               || um.getUserProfiles().contains(otherUser);
   }

    /**
     * Queries for the UserInfo of a user. Returns null if the user doesn't exist (was removed).
     * @param userManager Instance of UserManager
     * @param checkUser The user to check the existence of.
     * @return UserInfo of the user or null for non-existent user.
     */
    public static UserInfo getExistingUser(UserManager userManager, UserHandle checkUser) {
        final List<UserInfo> users = userManager.getUsers(true /* excludeDying */);
        final int checkUserId = checkUser.getIdentifier();
        for (UserInfo user : users) {
            if (user.id == checkUserId) {
                return user;
            }
        }
        return null;
    }

    public static View inflateCategoryHeader(LayoutInflater inflater, ViewGroup parent) {
        final TypedArray a = inflater.getContext().obtainStyledAttributes(null,
                com.android.internal.R.styleable.Preference,
                com.android.internal.R.attr.preferenceCategoryStyle, 0);
        final int resId = a.getResourceId(com.android.internal.R.styleable.Preference_layout,
                0);
        a.recycle();
        return inflater.inflate(resId, parent, false);
    }

    public static ArraySet<String> getHandledDomains(PackageManager pm, String packageName) {
        final List<IntentFilterVerificationInfo> iviList =
                pm.getIntentFilterVerifications(packageName);
        final List<IntentFilter> filters = pm.getAllIntentFilters(packageName);

        final ArraySet<String> result = new ArraySet<>();
        if (iviList != null && iviList.size() > 0) {
            for (IntentFilterVerificationInfo ivi : iviList) {
                for (String host : ivi.getDomains()) {
                    result.add(host);
                }
            }
        }
        if (filters != null && filters.size() > 0) {
            for (IntentFilter filter : filters) {
                if (filter.hasCategory(Intent.CATEGORY_BROWSABLE)
                        && (filter.hasDataScheme(IntentFilter.SCHEME_HTTP) ||
                                filter.hasDataScheme(IntentFilter.SCHEME_HTTPS))) {
                    result.addAll(filter.getHostsList());
                }
            }
        }
        return result;
    }

    /**
     * Returns the application info of the currently installed MDM package.
     */
    public static ApplicationInfo getAdminApplicationInfo(Context context, int profileId) {
        final DevicePolicyManager dpm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        final ComponentName mdmPackage = dpm.getProfileOwnerAsUser(profileId);
        if (mdmPackage == null) {
            return null;
        }
        final String mdmPackageName = mdmPackage.getPackageName();
        try {
            final IPackageManager ipm = AppGlobals.getPackageManager();
            final ApplicationInfo mdmApplicationInfo =
                    ipm.getApplicationInfo(mdmPackageName, 0, profileId);
            return mdmApplicationInfo;
        } catch (RemoteException e) {
            Log.e(TAG, "Error while retrieving application info for package " + mdmPackageName
                    + ", userId " + profileId, e);
            return null;
        }
    }

    public static boolean isBandwidthControlEnabled() {
        final INetworkManagementService netManager = INetworkManagementService.Stub
                .asInterface(ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE));
        try {
            return netManager.isBandwidthControlEnabled();
        } catch (RemoteException e) {
            return false;
        }
    }

    /**
     * Returns an accessible SpannableString.
     * @param displayText the text to display
     * @param accessibileText the text text-to-speech engines should read
     */
    public static SpannableString createAccessibleSequence(CharSequence displayText,
            String accessibileText) {
        final SpannableString str = new SpannableString(displayText);
        str.setSpan(new TtsSpan.TextBuilder(accessibileText).build(), 0,
                displayText.length(),
                Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        return str;
    }

    /**
     * Returns the user id present in the bundle with
     * {@link Intent#EXTRA_USER_ID} if it belongs to the current user.
     *
     * @throws SecurityException if the given userId does not belong to the
     *             current user group.
     */
    public static int getUserIdFromBundle(Context context, Bundle bundle) {
        return getUserIdFromBundle(context, bundle, false);
    }

    /**
     * Returns the user id present in the bundle with
     * {@link Intent#EXTRA_USER_ID} if it belongs to the current user.
     *
     * @param isInternal indicating if the caller is "internal" to the system,
     *            meaning we're willing to trust extras like
     *            {@link ChooseLockSettingsHelper#EXTRA_ALLOW_ANY_USER}.
     * @throws SecurityException if the given userId does not belong to the
     *             current user group.
     */
    public static int getUserIdFromBundle(Context context, Bundle bundle, boolean isInternal) {
        if (bundle == null) {
            return getCredentialOwnerUserId(context);
        }
        final boolean allowAnyUser = isInternal
                && bundle.getBoolean(ChooseLockSettingsHelper.EXTRA_ALLOW_ANY_USER, false);
        final int userId = bundle.getInt(Intent.EXTRA_USER_ID, UserHandle.myUserId());
        if (userId == LockPatternUtils.USER_FRP) {
            return allowAnyUser ? userId : enforceSystemUser(context, userId);
        } else {
            return allowAnyUser ? userId : enforceSameOwner(context, userId);
        }
    }

    /**
     * Returns the given user id if the current user is the system user.
     *
     * @throws SecurityException if the current user is not the system user.
     */
    public static int enforceSystemUser(Context context, int userId) {
        if (UserHandle.myUserId() == UserHandle.USER_SYSTEM) {
            return userId;
        }
        throw new SecurityException("Given user id " + userId + " must only be used from "
                + "USER_SYSTEM, but current user is " + UserHandle.myUserId());
    }

    /**
     * Returns the given user id if it belongs to the current user.
     *
     * @throws SecurityException if the given userId does not belong to the current user group.
     */
    public static int enforceSameOwner(Context context, int userId) {
        final UserManager um = context.getSystemService(UserManager.class);
        final int[] profileIds = um.getProfileIdsWithDisabled(UserHandle.myUserId());
        if (ArrayUtils.contains(profileIds, userId)) {
            return userId;
        }
        throw new SecurityException("Given user id " + userId + " does not belong to user "
                + UserHandle.myUserId());
    }

    /**
     * Returns the effective credential owner of the calling user.
     */
    public static int getCredentialOwnerUserId(Context context) {
        return getCredentialOwnerUserId(context, UserHandle.myUserId());
    }

    /**
     * Returns the user id of the credential owner of the given user id.
     */
    public static int getCredentialOwnerUserId(Context context, int userId) {
        final UserManager um = context.getSystemService(UserManager.class);
        return um.getCredentialOwnerProfile(userId);
    }

    /**
     * Returns the credential type of the given user id.
     */
    public static @LockPatternUtils.CredentialType int getCredentialType(Context context,
            int userId) {
        final LockPatternUtils lpu = new LockPatternUtils(context);
        return lpu.getCredentialTypeForUser(userId);
    }

    private static final StringBuilder sBuilder = new StringBuilder(50);
    private static final java.util.Formatter sFormatter = new java.util.Formatter(
            sBuilder, Locale.getDefault());

    public static String formatDateRange(Context context, long start, long end) {
        final int flags = FORMAT_SHOW_DATE | FORMAT_ABBREV_MONTH;

        synchronized (sBuilder) {
            sBuilder.setLength(0);
            return DateUtils.formatDateRange(context, sFormatter, start, end, flags, null)
                    .toString();
        }
    }

    public static boolean startQuietModeDialogIfNecessary(Context context, UserManager um,
            int userId) {
        if (um.isQuietModeEnabled(UserHandle.of(userId))) {
            final Intent intent = UnlaunchableAppActivity.createInQuietModeDialogIntent(userId);
            context.startActivity(intent);
            return true;
        }
        return false;
    }

    public static boolean unlockWorkProfileIfNecessary(Context context, int userId) {
        try {
            if (!ActivityManager.getService().isUserRunning(userId,
                    ActivityManager.FLAG_AND_LOCKED)) {
                return false;
            }
        } catch (RemoteException e) {
            return false;
        }
        if (!(new LockPatternUtils(context)).isSecure(userId)) {
            return false;
        }
        return confirmWorkProfileCredentials(context, userId);
    }

    private static boolean confirmWorkProfileCredentials(Context context, int userId) {
        final KeyguardManager km = (KeyguardManager) context.getSystemService(
                Context.KEYGUARD_SERVICE);
        final Intent unlockIntent = km.createConfirmDeviceCredentialIntent(null, null, userId);
        if (unlockIntent != null) {
            context.startActivity(unlockIntent);
            return true;
        } else {
            return false;
        }
    }

    public static CharSequence getApplicationLabel(Context context, String packageName) {
        try {
            final ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    packageName,
                    PackageManager.MATCH_DISABLED_COMPONENTS
                    | PackageManager.MATCH_ANY_USER);
            return appInfo.loadLabel(context.getPackageManager());
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Unable to find info for package: " + packageName);
        }
        return null;
    }

    public static boolean isPackageDirectBootAware(Context context, String packageName) {
        try {
            final ApplicationInfo ai = context.getPackageManager().getApplicationInfo(
                    packageName, 0);
            return ai.isDirectBootAware() || ai.isPartiallyDirectBootAware();
        } catch (NameNotFoundException ignored) {
        }
        return false;
    }

    /**
     * Returns a context created from the given context for the given user, or null if it fails
     */
    public static Context createPackageContextAsUser(Context context, int userId) {
        try {
            return context.createPackageContextAsUser(
                    context.getPackageName(), 0 /* flags */, UserHandle.of(userId));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to create user context", e);
        }
        return null;
    }

    public static FingerprintManager getFingerprintManagerOrNull(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            return (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
        } else {
            return null;
        }
    }

    public static boolean hasFingerprintHardware(Context context) {
        final FingerprintManager fingerprintManager = getFingerprintManagerOrNull(context);
        return fingerprintManager != null && fingerprintManager.isHardwareDetected();
    }

    public static FaceManager getFaceManagerOrNull(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FACE)) {
            return (FaceManager) context.getSystemService(Context.FACE_SERVICE);
        } else {
            return null;
        }
    }

    public static boolean hasFaceHardware(Context context) {
        final FaceManager faceManager = getFaceManagerOrNull(context);
        return faceManager != null && faceManager.isHardwareDetected();
    }

    /**
     * Launches an intent which may optionally have a user id defined.
     * @param fragment Fragment to use to launch the activity.
     * @param intent Intent to launch.
     */
    public static void launchIntent(Fragment fragment, Intent intent) {
        try {
            final int userId = intent.getIntExtra(Intent.EXTRA_USER_ID, -1);

            if (userId == -1) {
                fragment.startActivity(intent);
            } else {
                fragment.getActivity().startActivityAsUser(intent, new UserHandle(userId));
            }
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "No activity found for " + intent);
        }
    }

    public static boolean isDemoUser(Context context) {
        return UserManager.isDeviceInDemoMode(context)
                && context.getSystemService(UserManager.class).isDemoUser();
    }

    public static ComponentName getDeviceOwnerComponent(Context context) {
        final DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        return dpm.getDeviceOwnerComponentOnAnyUser();
    }

    /**
     * Returns if a given user is a profile of another user.
     * @param user The user whose profiles wibe checked.
     * @param profile The (potential) profile.
     * @return if the profile is actually a profile
     */
    public static boolean isProfileOf(UserInfo user, UserInfo profile) {
        return user.id == profile.id ||
                (user.profileGroupId != UserInfo.NO_PROFILE_GROUP_ID
                        && user.profileGroupId == profile.profileGroupId);
    }

    /**
     * Tries to initalize a volume with the given bundle. If it is a valid, private, and readable
     * {@link VolumeInfo}, it is returned. If it is not valid, null is returned.
     */
    @Nullable
    public static VolumeInfo maybeInitializeVolume(StorageManager sm, Bundle bundle) {
        final String volumeId = bundle.getString(VolumeInfo.EXTRA_VOLUME_ID,
                VolumeInfo.ID_PRIVATE_INTERNAL);
        final VolumeInfo volume = sm.findVolumeById(volumeId);
        return isVolumeValid(volume) ? volume : null;
    }

    /**
     * Return {@code true} if the supplied package is device owner or profile owner of at
     * least one user.
     * @param userManager used to get profile owner app for each user
     * @param devicePolicyManager used to check whether it is device owner app
     * @param packageName package to check about
     */
    public static boolean isProfileOrDeviceOwner(UserManager userManager,
            DevicePolicyManager devicePolicyManager, String packageName) {
        final List<UserInfo> userInfos = userManager.getUsers();
        if (devicePolicyManager.isDeviceOwnerAppOnAnyUser(packageName)) {
            return true;
        }
        for (int i = 0, size = userInfos.size(); i < size; i++) {
            final ComponentName cn = devicePolicyManager
                    .getProfileOwnerAsUser(userInfos.get(i).id);
            if (cn != null && cn.getPackageName().equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return {@code true} if the supplied package is the device owner or profile owner of a
     * given user.
     *
     * @param devicePolicyManager used to check whether it is device owner and profile owner app
     * @param packageName         package to check about
     * @param userId              the if of the relevant user
     */
    public static boolean isProfileOrDeviceOwner(DevicePolicyManager devicePolicyManager,
            String packageName, int userId) {
        if ((devicePolicyManager.getDeviceOwnerUserId() == userId)
                && devicePolicyManager.isDeviceOwnerApp(packageName)) {
            return true;
        }
        final ComponentName cn = devicePolicyManager.getProfileOwnerAsUser(userId);
        if (cn != null && cn.getPackageName().equals(packageName)) {
            return true;
        }
        return false;
    }

    /**
     * Return the resource id to represent the install status for an app
     */
    @StringRes
    public static int getInstallationStatus(ApplicationInfo info) {
        if ((info.flags & ApplicationInfo.FLAG_INSTALLED) == 0) {
            return R.string.not_installed;
        }
        return info.enabled ? R.string.installed : R.string.disabled;
    }

    private static boolean isVolumeValid(VolumeInfo volume) {
        return (volume != null) && (volume.getType() == VolumeInfo.TYPE_PRIVATE)
                && volume.isMountedReadable();
    }

    public static void setEditTextCursorPosition(EditText editText) {
        editText.setSelection(editText.getText().length());
    }

    /**
     * Sets the preference icon with a drawable that is scaled down to to avoid crashing Settings if
     * it's too big.
     */
    public static void setSafeIcon(Preference pref, Drawable icon) {
        Drawable safeIcon = icon;
        if ((icon != null) && !(icon instanceof VectorDrawable)) {
            safeIcon = getSafeDrawable(icon, 500, 500);
        }
        pref.setIcon(safeIcon);
    }

    /**
     * Gets a drawable with a limited size to avoid crashing Settings if it's too big.
     *
     * @param original original drawable, typically an app icon.
     * @param maxWidth maximum width, in pixels.
     * @param maxHeight maximum height, in pixels.
     */
    public static Drawable getSafeDrawable(Drawable original, int maxWidth, int maxHeight) {
        final int actualWidth = original.getMinimumWidth();
        final int actualHeight = original.getMinimumHeight();

        if (actualWidth <= maxWidth && actualHeight <= maxHeight) {
            return original;
        }

        final float scaleWidth = ((float) maxWidth) / actualWidth;
        final float scaleHeight = ((float) maxHeight) / actualHeight;
        final float scale = Math.min(scaleWidth, scaleHeight);
        final int width = (int) (actualWidth * scale);
        final int height = (int) (actualHeight * scale);

        final Bitmap bitmap;
        if (original instanceof BitmapDrawable) {
            bitmap = Bitmap.createScaledBitmap(((BitmapDrawable) original).getBitmap(), width,
                    height, false);
        } else {
            bitmap = createBitmap(original, width, height);
        }
        return new BitmapDrawable(null, bitmap);
    }

    /**
     * Create an Icon pointing to a drawable.
     */
    public static IconCompat createIconWithDrawable(Drawable drawable) {
        Bitmap bitmap;
        if (drawable instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable)drawable).getBitmap();
        } else {
            final int width = drawable.getIntrinsicWidth();
            final int height = drawable.getIntrinsicHeight();
            bitmap = createBitmap(drawable,
                    width > 0 ? width : 1,
                    height > 0 ? height : 1);
        }
        return IconCompat.createWithBitmap(bitmap);
    }

    /**
     * Creates a drawable with specified width and height.
     */
    public static Bitmap createBitmap(Drawable drawable, int width, int height) {
        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * Get the {@link Drawable} that represents the app icon
     */
    public static Drawable getBadgedIcon(IconDrawableFactory iconDrawableFactory,
            PackageManager packageManager, String packageName, int userId) {
        try {
            final ApplicationInfo appInfo = packageManager.getApplicationInfoAsUser(
                    packageName, PackageManager.GET_META_DATA, userId);
            return iconDrawableFactory.getBadgedIcon(appInfo, userId);
        } catch (PackageManager.NameNotFoundException e) {
            return packageManager.getDefaultActivityIcon();
        }
    }

    /** Returns true if the current package is installed & enabled. */
    public static boolean isPackageEnabled(Context context, String packageName) {
        try {
            return context.getPackageManager().getApplicationInfo(packageName, 0).enabled;
        } catch (Exception e) {
            Log.e(TAG, "Error while retrieving application info for package " + packageName, e);
        }
        return false;
    }

    /** Get {@link Resources} by subscription id if subscription id is valid. */
    public static Resources getResourcesForSubId(Context context, int subId) {
        if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return SubscriptionManager.getResourcesForSubId(context, subId);
        } else {
            return context.getResources();
        }
    }

    /**
     * Returns true if SYSTEM_ALERT_WINDOW permission is available.
     * Starting from Q, SYSTEM_ALERT_WINDOW is disabled on low ram phones.
     */
    public static boolean isSystemAlertWindowEnabled(Context context) {
        // SYSTEM_ALERT_WINDOW is disabled on on low ram devices starting from Q
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return !(am.isLowRamDevice() && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q));
    }

    /**
     * Adds a shadow appear/disappear animation to action bar scroll.
     *
     * <p/>
     * This method must be called after {@link Fragment#onCreate(Bundle)}.
     */
    public static void setActionBarShadowAnimation(Activity activity, Lifecycle lifecycle,
            View scrollView) {
        if (activity == null) {
            Log.w(TAG, "No activity, cannot style actionbar.");
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (actionBar == null) {
            Log.w(TAG, "No actionbar, cannot style actionbar.");
            return;
        }
        actionBar.setElevation(0);

        if (lifecycle != null && scrollView != null) {
            ActionBarShadowController.attachToView(activity, lifecycle, scrollView);
        }
    }

    /**
     * Return correct target fragment based on argument
     *
     * @param activity     the activity target fragment will be launched.
     * @param fragmentName initial target fragment name.
     * @param args         fragment launch arguments.
     */
    public static Fragment getTargetFragment(Activity activity, String fragmentName, Bundle args) {
        Fragment f = null;
        final boolean isPersonal = args != null ? args.getInt(ProfileSelectFragment.EXTRA_PROFILE)
                == ProfileSelectFragment.ProfileType.PERSONAL : false;
        final boolean isWork = args != null ? args.getInt(ProfileSelectFragment.EXTRA_PROFILE)
                == ProfileSelectFragment.ProfileType.WORK : false;
        if (activity.getSystemService(UserManager.class).getUserProfiles().size() > 1
                && ProfileFragmentBridge.FRAGMENT_MAP.get(fragmentName) != null
                && !isWork && !isPersonal) {
            f = Fragment.instantiate(activity, ProfileFragmentBridge.FRAGMENT_MAP.get(fragmentName),
                    args);
        } else {
            f = Fragment.instantiate(activity, fragmentName, args);
        }
        return f;
    }

    /**
     * Returns true if current binder uid is Settings Intelligence.
     */
    public static boolean isSettingsIntelligence(Context context) {
        final int callingUid = Binder.getCallingUid();
        final String callingPackage = context.getPackageManager().getPackagesForUid(callingUid)[0];
        final boolean isSettingsIntelligence = TextUtils.equals(callingPackage,
                context.getString(R.string.config_settingsintelligence_package_name));
        return isSettingsIntelligence;
    }

    /**
     * Returns true if the night mode is enabled.
     */
    public static boolean isNightMode(Context context) {
        final int currentNightMode =
                context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Returns a bitmap with rounded corner.
     *
     * @param context application context.
     * @param source bitmap to apply round corner.
     * @param cornerRadius corner radius value.
     */
    public static Bitmap convertCornerRadiusBitmap(@NonNull Context context,
            @NonNull Bitmap source, @NonNull float cornerRadius) {
        final Bitmap roundedBitmap = Bitmap.createBitmap(source.getWidth(), source.getHeight(),
                Bitmap.Config.ARGB_8888);
        final RoundedBitmapDrawable drawable =
                RoundedBitmapDrawableFactory.create(context.getResources(), source);
        drawable.setAntiAlias(true);
        drawable.setCornerRadius(cornerRadius);
        final Canvas canvas = new Canvas(roundedBitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return roundedBitmap;
    }

    /**
     * Returns {@code true} if needed to disable media output, otherwise returns {@code false}.
     */
    public static boolean isMediaOutputDisabled(
            MediaRouter2Manager router2Manager, String packageName) {
        boolean isMediaOutputDisabled = false;
        if (!TextUtils.isEmpty(packageName)) {
            final List<MediaRoute2Info> infos = router2Manager.getAvailableRoutes(packageName);
            if (infos.size() == 1) {
                final MediaRoute2Info info = infos.get(0);
                final int deviceType = info.getType();
                switch (deviceType) {
                    case TYPE_UNKNOWN:
                    case TYPE_REMOTE_TV:
                    case TYPE_REMOTE_SPEAKER:
                    case TYPE_GROUP:
                        isMediaOutputDisabled = true;
                        break;
                    default:
                        isMediaOutputDisabled = false;
                        break;
                }
            }
        }
        return isMediaOutputDisabled;
    }
}
