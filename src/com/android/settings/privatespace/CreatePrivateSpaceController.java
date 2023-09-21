/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.privatespace;

import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

// TODO(b/293569406): Remove this when we have the setup flow in place to create PS
/**
 * Temp Controller to create the private space from the PS Settings page. This is to allow PM, UX,
 * and other folks to play around with PS before the PS setup flow is ready.
 */
public final class CreatePrivateSpaceController extends BasePreferenceController {

    public CreatePrivateSpaceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }

        if (PrivateSpaceMaintainer.getInstance(mContext).doesPrivateSpaceExist()) {
            showPrivateSpaceAlreadyExistsToast();
            return super.handlePreferenceTreeClick(preference);
        }

        if (PrivateSpaceMaintainer.getInstance(mContext).createPrivateSpace()) {
            showPrivateSpaceCreatedToast();
        } else {
            showPrivateSpaceCreationFailedToast();
        }
        return super.handlePreferenceTreeClick(preference);
    }

    private void showPrivateSpaceCreatedToast() {
        Toast.makeText(mContext, R.string.private_space_created, Toast.LENGTH_SHORT).show();
    }

    private void showPrivateSpaceCreationFailedToast() {
        Toast.makeText(mContext, R.string.private_space_create_failed, Toast.LENGTH_SHORT).show();
    }

    private void showPrivateSpaceAlreadyExistsToast() {
        Toast.makeText(mContext, R.string.private_space_already_exists, Toast.LENGTH_SHORT).show();
    }
}
