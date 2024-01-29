/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.notification.app;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.NotificationManager.IMPORTANCE_LOW;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.util.Pair;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ConversationPriorityPreferenceTest {

    private Context mContext;

    @Before
    public void setUp() {
        Context context = spy(RuntimeEnvironment.application.getApplicationContext());
        mContext = new ContextThemeWrapper(context, R.style.Theme_Settings);
    }

    @Test
    public void createNewPreference_shouldSetLayout() {
        final ConversationPriorityPreference preference =
                new ConversationPriorityPreference(mContext);
        assertThat(preference.getLayoutResource()).isEqualTo(
                R.layout.notif_priority_conversation_preference);
    }

    @Test
    public void onBindViewHolder_nonConfigurable() {
        final ConversationPriorityPreference preference =
                new ConversationPriorityPreference(mContext);
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(preference.getLayoutResource(), null));

        preference.setConfigurable(false);
        preference.setImportance(IMPORTANCE_DEFAULT);
        preference.setPriorityConversation(true);
        preference.onBindViewHolder(holder);

        assertThat(holder.itemView.findViewById(R.id.silence).isEnabled()).isFalse();
        assertThat(holder.itemView.findViewById(R.id.priority_group).isEnabled()).isFalse();
        assertThat(holder.itemView.findViewById(R.id.alert).isEnabled()).isFalse();
    }

    @Test
    public void onBindViewHolder_selectButtonAndText() {
        final ConversationPriorityPreference preference =
                new ConversationPriorityPreference(mContext);
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(preference.getLayoutResource(), null));

        preference.setConfigurable(true);
        preference.setImportance(IMPORTANCE_LOW);
        preference.setPriorityConversation(true);

        preference.onBindViewHolder(holder);

        assertThat(holder.itemView.findViewById(R.id.silence)
                .findViewById(R.id.summary).getVisibility())
                .isEqualTo(View.VISIBLE);
    }

    @Test
    public void onClick_changesUICallsListener() {
        final ConversationPriorityPreference preference =
                spy(new ConversationPriorityPreference(mContext));
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(preference.getLayoutResource(), null));

        preference.setConfigurable(true);
        preference.setImportance(IMPORTANCE_DEFAULT);
        preference.setPriorityConversation(true);
        preference.setOriginalImportance(IMPORTANCE_DEFAULT);
        preference.onBindViewHolder(holder);

        View silenceButton = holder.itemView.findViewById(R.id.silence);

        silenceButton.callOnClick();

        verify(preference, times(1)).callChangeListener(new Pair(IMPORTANCE_LOW, false));
    }
}
