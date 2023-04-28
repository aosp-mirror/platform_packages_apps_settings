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

package com.android.settings.bluetooth;

import static android.app.slice.Slice.HINT_PERMISSION_REQUEST;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The blocking preference with slice controller will make whole page invisible for a certain time
 * until {@link Slice} is fully loaded.
 */
public class BlockingPrefWithSliceController extends BasePreferenceController implements
        LifecycleObserver, OnStart, OnStop, Observer<Slice>, BasePreferenceController.UiBlocker{
    private static final String TAG = "BlockingPrefWithSliceController";

    private static final String PREFIX_KEY = "slice_preference_item_";

    @VisibleForTesting
    LiveData<Slice> mLiveData;
    private Uri mUri;
    @VisibleForTesting
    PreferenceCategory mPreferenceCategory;
    private List<Preference> mCurrentPreferencesList = new ArrayList<>();
    @VisibleForTesting
    String mSliceIntentAction = "";
    @VisibleForTesting
    String mSlicePendingIntentAction = "";
    @VisibleForTesting
    String mExtraIntent = "";
    @VisibleForTesting
    String mExtraPendingIntent = "";

    public BlockingPrefWithSliceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceCategory = screen.findPreference(getPreferenceKey());
        mSliceIntentAction = mContext.getResources().getString(
                R.string.config_bt_slice_intent_action);
        mSlicePendingIntentAction = mContext.getResources().getString(
                R.string.config_bt_slice_pending_intent_action);
        mExtraIntent = mContext.getResources().getString(R.string.config_bt_slice_extra_intent);
        mExtraPendingIntent = mContext.getResources().getString(
                R.string.config_bt_slice_extra_pending_intent);
    }

    @Override
    public int getAvailabilityStatus() {
        return mUri != null ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    public void setSliceUri(Uri uri) {
        mUri = uri;
        mLiveData = SliceLiveData.fromUri(mContext, mUri, (int type, Throwable source) -> {
            Log.w(TAG, "Slice may be null. uri = " + uri + ", error = " + type);
        });

        //TODO(b/120803703): figure out why we need to remove observer first
        mLiveData.removeObserver(this);
    }

    @Override
    public void onStart() {
        if (mLiveData != null) {
            mLiveData.observeForever(this);
        }
    }

    @Override
    public void onStop() {
        if (mLiveData != null) {
            mLiveData.removeObserver(this);
        }
    }

    @Override
    public void onChanged(Slice slice) {
        updatePreferenceFromSlice(slice);
        if (mUiBlockListener != null) {
            mUiBlockListener.onBlockerWorkFinished(this);
        }
    }

    @VisibleForTesting
    void updatePreferenceFromSlice(Slice slice) {
        if (TextUtils.isEmpty(mSliceIntentAction)
                || TextUtils.isEmpty(mExtraIntent)
                || TextUtils.isEmpty(mSlicePendingIntentAction)
                || TextUtils.isEmpty(mExtraPendingIntent)) {
            Log.d(TAG, "No configs");
            return;
        }
        if (slice == null || slice.hasHint(HINT_PERMISSION_REQUEST)) {
            Log.d(TAG, "Current slice: " + slice);
            removePreferenceListFromPreferenceCategory();
            return;
        }
        updatePreferenceListAndPreferenceCategory(parseSliceToPreferenceList(slice));
    }

    private List<Preference> parseSliceToPreferenceList(Slice slice) {
        List<Preference> preferenceItemsList = new ArrayList<>();
        List<SliceItem> items = slice.getItems();
        int orderLevel = 0;
        for (SliceItem sliceItem : items) {
            // Parse the slice
            if (sliceItem.getFormat().equals(FORMAT_SLICE)) {
                Optional<CharSequence> title = extractTitleFromSlice(sliceItem.getSlice());
                Optional<CharSequence> subtitle = extractSubtitleFromSlice(sliceItem.getSlice());
                Optional<SliceAction> action = extractActionFromSlice(sliceItem.getSlice());
                // Create preference
                Optional<Preference> preferenceItem = createPreferenceItem(title, subtitle, action,
                        orderLevel);
                if (preferenceItem.isPresent()) {
                    orderLevel++;
                    preferenceItemsList.add(preferenceItem.get());
                }
            }
        }
        return preferenceItemsList;
    }

    private Optional<Preference> createPreferenceItem(Optional<CharSequence> title,
            Optional<CharSequence> subtitle, Optional<SliceAction> sliceAction, int orderLevel) {
        Log.d(TAG, "Title: " + title.orElse("no title")
                + ", Subtitle: " + subtitle.orElse("no Subtitle")
                + ", Action: " + sliceAction.orElse(null));
        if (!title.isPresent()) {
            return Optional.empty();
        }
        String key = PREFIX_KEY + title.get();
        Preference preference = mPreferenceCategory.findPreference(key);
        if (preference == null) {
            preference = new Preference(mContext);
            preference.setKey(key);
            mPreferenceCategory.addPreference(preference);
        }
        preference.setTitle(title.get());
        preference.setOrder(orderLevel);
        if (subtitle.isPresent()) {
            preference.setSummary(subtitle.get());
        }
        if (sliceAction.isPresent()) {
            // To support the settings' 2 panel feature, here can't use the slice's
            // PendingIntent.send(). Since the PendingIntent.send() always take NEW_TASK flag.
            // Therefore, transfer the slice's PendingIntent to Intent and start it
            // without NEW_TASK.
            preference.setIcon(sliceAction.get().getIcon().loadDrawable(mContext));
            Intent intentFromSliceAction = sliceAction.get().getAction().getIntent();
            Intent expectedActivityIntent = null;
            Log.d(TAG, "SliceAction: intent's Action:" + intentFromSliceAction.getAction());
            if (intentFromSliceAction.getAction().equals(mSliceIntentAction)) {
                expectedActivityIntent = intentFromSliceAction
                        .getParcelableExtra(mExtraIntent, Intent.class);
            } else if (intentFromSliceAction.getAction().equals(
                    mSlicePendingIntentAction)) {
                PendingIntent pendingIntent = intentFromSliceAction
                        .getParcelableExtra(mExtraPendingIntent, PendingIntent.class);
                expectedActivityIntent =
                        pendingIntent != null ? pendingIntent.getIntent() : null;
            } else {
                expectedActivityIntent = intentFromSliceAction;
            }
            if (expectedActivityIntent != null) {
                Log.d(TAG, "setIntent: ActivityIntent" + expectedActivityIntent);
                // Since UI needs to support the Settings' 2 panel feature, the intent can't use the
                // FLAG_ACTIVITY_NEW_TASK. The above intent may have the FLAG_ACTIVITY_NEW_TASK
                // flag, so removes it before startActivity(preference.setIntent).
                expectedActivityIntent.removeFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                preference.setIntent(expectedActivityIntent);
            } else {
                Log.d(TAG, "setIntent: Intent is null");
            }
        }

        return Optional.of(preference);
    }

    private void removePreferenceListFromPreferenceCategory() {
        mCurrentPreferencesList.stream()
                .forEach(p -> mPreferenceCategory.removePreference(p));
        mCurrentPreferencesList.clear();
    }

    private void updatePreferenceListAndPreferenceCategory(List<Preference> newPreferenceList) {
        List<Preference> removedItemList = new ArrayList<>(mCurrentPreferencesList);
        for (Preference item : mCurrentPreferencesList) {
            if (newPreferenceList.stream().anyMatch(p -> item.compareTo(p) == 0)) {
                removedItemList.remove(item);
            }
        }
        removedItemList.stream()
                .forEach(p -> mPreferenceCategory.removePreference(p));
        mCurrentPreferencesList = newPreferenceList;
    }

    private Optional<CharSequence> extractTitleFromSlice(Slice slice) {
        return extractTextFromSlice(slice, HINT_TITLE);
    }

    private Optional<CharSequence> extractSubtitleFromSlice(Slice slice) {
        // For subtitle items, there isn't a hint available.
        return extractTextFromSlice(slice, /* hint= */ null);
    }

    private Optional<CharSequence> extractTextFromSlice(Slice slice, @Nullable String hint) {
        for (SliceItem item : slice.getItems()) {
            if (item.getFormat().equals(FORMAT_TEXT)
                    && ((TextUtils.isEmpty(hint) && item.getHints().isEmpty())
                    || (!TextUtils.isEmpty(hint) && item.hasHint(hint)))) {
                return Optional.ofNullable(item.getText());
            }
        }
        return Optional.empty();
    }

    private Optional<SliceAction> extractActionFromSlice(Slice slice) {
        for (SliceItem item : slice.getItems()) {
            if (item.getFormat().equals(FORMAT_SLICE)) {
                if (item.hasHint(HINT_TITLE)) {
                    Optional<SliceAction> result = extractActionFromSlice(item.getSlice());
                    if (result.isPresent()) {
                        return result;
                    }
                }
                continue;
            }

            if (item.getFormat().equals(FORMAT_ACTION)) {
                Optional<IconCompat> icon = extractIconFromSlice(item.getSlice());
                Optional<CharSequence> title = extractTitleFromSlice(item.getSlice());
                if (icon.isPresent()) {
                    return Optional.of(
                            SliceAction.create(
                                    item.getAction(),
                                    icon.get(),
                                    ListBuilder.ICON_IMAGE,
                                    title.orElse(/* other= */ "")));
                }
            }
        }
        return Optional.empty();
    }

    private Optional<IconCompat> extractIconFromSlice(Slice slice) {
        for (SliceItem item : slice.getItems()) {
            if (item.getFormat().equals(FORMAT_IMAGE)) {
                return Optional.of(item.getIcon());
            }
        }
        return Optional.empty();
    }
}
