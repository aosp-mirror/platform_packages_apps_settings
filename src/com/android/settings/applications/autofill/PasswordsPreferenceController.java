/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.applications.autofill;

import static android.app.admin.DevicePolicyResources.Strings.Settings.AUTO_SYNC_PERSONAL_DATA;
import static android.app.admin.DevicePolicyResources.Strings.Settings.AUTO_SYNC_PRIVATE_DATA;
import static android.app.admin.DevicePolicyResources.Strings.Settings.AUTO_SYNC_WORK_DATA;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.service.autofill.AutofillService.EXTRA_RESULT;

import static androidx.lifecycle.Lifecycle.Event.ON_CREATE;
import static androidx.lifecycle.Lifecycle.Event.ON_DESTROY;

import android.annotation.UserIdInt;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.service.autofill.AutofillService;
import android.service.autofill.AutofillServiceInfo;
import android.service.autofill.IAutoFillService;
import android.text.TextUtils;
import android.util.IconDrawableFactory;
import android.util.Log;

import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.IResultReceiver;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.utils.StringUtil;
import com.android.settingslib.widget.AppPreference;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Queries available autofill services and adds preferences for those that declare passwords
 * settings.
 * <p>
 * The controller binds to each service to fetch the number of saved passwords in each.
 */
public class PasswordsPreferenceController extends BasePreferenceController
        implements LifecycleObserver {
    private static final String TAG = "AutofillSettings";
    private static final boolean DEBUG = false;

    private final PackageManager mPm;
    private final IconDrawableFactory mIconFactory;
    private final List<AutofillServiceInfo> mServices;

    private LifecycleOwner mLifecycleOwner;

    public PasswordsPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mPm = context.getPackageManager();
        mIconFactory = IconDrawableFactory.newInstance(mContext);
        mServices = new ArrayList<>();
    }

    @OnLifecycleEvent(ON_CREATE)
    void onCreate(LifecycleOwner lifecycleOwner) {
        init(lifecycleOwner, AutofillServiceInfo.getAvailableServices(mContext, getUser()));
    }

    @VisibleForTesting
    void init(LifecycleOwner lifecycleOwner, List<AutofillServiceInfo> availableServices) {
        mLifecycleOwner = lifecycleOwner;

        for (int i = availableServices.size() - 1; i >= 0; i--) {
            final String passwordsActivity = availableServices.get(i).getPasswordsActivity();
            if (TextUtils.isEmpty(passwordsActivity)) {
                availableServices.remove(i);
            }
        }
        // TODO: Reverse the loop above and add to mServices directly.
        mServices.clear();
        mServices.addAll(availableServices);
    }

    @Override
    public int getAvailabilityStatus() {
        return mServices.isEmpty() ? CONDITIONALLY_UNAVAILABLE : AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final PreferenceGroup group = screen.findPreference(getPreferenceKey());
        addPasswordPreferences(screen.getContext(), getUser(), group);

        replaceEnterpriseStringTitle(screen, "auto_sync_personal_account_data",
                AUTO_SYNC_PERSONAL_DATA, R.string.account_settings_menu_auto_sync_personal);
        replaceEnterpriseStringTitle(screen, "auto_sync_work_account_data",
                AUTO_SYNC_WORK_DATA, R.string.account_settings_menu_auto_sync_work);
        replaceEnterpriseStringTitle(screen, "auto_sync_private_account_data",
                AUTO_SYNC_PRIVATE_DATA, R.string.account_settings_menu_auto_sync_private);
    }

    private void addPasswordPreferences(
            Context prefContext, @UserIdInt int user, PreferenceGroup group) {
        for (int i = 0; i < mServices.size(); i++) {
            final AutofillServiceInfo service = mServices.get(i);
            final AppPreference pref = new AppPreference(prefContext);
            final ServiceInfo serviceInfo = service.getServiceInfo();
            pref.setTitle(serviceInfo.loadLabel(mPm));
            final Drawable icon =
                    mIconFactory.getBadgedIcon(
                            serviceInfo,
                            serviceInfo.applicationInfo,
                            user);
            pref.setIcon(Utils.getSafeIcon(icon));
            pref.setOnPreferenceClickListener(p -> {
                final Intent intent =
                        new Intent(Intent.ACTION_MAIN)
                                .setClassName(
                                        serviceInfo.packageName,
                                        service.getPasswordsActivity())
                                .setFlags(FLAG_ACTIVITY_NEW_TASK);
                prefContext.startActivityAsUser(intent, UserHandle.of(user));
                return true;
            });
            // Set a placeholder summary to avoid a UI flicker when the value loads.
            pref.setSummary(R.string.autofill_passwords_count_placeholder);

            final MutableLiveData<Integer> passwordCount = new MutableLiveData<>();
            passwordCount.observe(
                    mLifecycleOwner, count -> {
                        // TODO(b/169455298): Validate the result.
                        final CharSequence summary = StringUtil.getIcuPluralsString(mContext, count,
                                R.string.autofill_passwords_count);
                        pref.setSummary(summary);
                    });
            // TODO(b/169455298): Limit the number of concurrent queries.
            // TODO(b/169455298): Cache the results for some time.
            requestSavedPasswordCount(service, user, passwordCount);

            group.addPreference(pref);
        }
    }

    private void requestSavedPasswordCount(
            AutofillServiceInfo service, @UserIdInt int user, MutableLiveData<Integer> data) {
        final Intent intent =
                new Intent(AutofillService.SERVICE_INTERFACE)
                        .setComponent(service.getServiceInfo().getComponentName());
        final AutofillServiceConnection connection = new AutofillServiceConnection(mContext, data);
        if (mContext.bindServiceAsUser(
                intent, connection, Context.BIND_AUTO_CREATE, UserHandle.of(user))) {
            connection.mBound.set(true);
            mLifecycleOwner.getLifecycle().addObserver(connection);
        }
    }

    private static class AutofillServiceConnection implements ServiceConnection, LifecycleObserver {
        final WeakReference<Context> mContext;
        final MutableLiveData<Integer> mData;
        final AtomicBoolean mBound = new AtomicBoolean();

        AutofillServiceConnection(Context context, MutableLiveData<Integer> data) {
            mContext = new WeakReference<>(context);
            mData = data;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            final IAutoFillService autofillService = IAutoFillService.Stub.asInterface(service);
            if (DEBUG) {
                Log.d(TAG, "Fetching password count from " + name);
            }
            try {
                autofillService.onSavedPasswordCountRequest(
                        new IResultReceiver.Stub() {
                            @Override
                            public void send(int resultCode, Bundle resultData) {
                                if (DEBUG) {
                                    Log.d(TAG, "Received password count result " + resultCode
                                            + " from " + name);
                                }
                                if (resultCode == 0 && resultData != null) {
                                    mData.postValue(resultData.getInt(EXTRA_RESULT));
                                }
                                unbind();
                            }
                        });
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to fetch password count: " + e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @OnLifecycleEvent(ON_DESTROY)
        void unbind() {
            if (!mBound.getAndSet(false)) {
                return;
            }
            final Context context = mContext.get();
            if (context != null) {
                context.unbindService(this);
            }
        }
    }

    private int getUser() {
        UserHandle workUser = getWorkProfileUser();
        return workUser != null ? workUser.getIdentifier() : UserHandle.myUserId();
    }
}
