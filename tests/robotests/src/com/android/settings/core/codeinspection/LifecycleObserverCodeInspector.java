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

package com.android.settings.core.codeinspection;

import static org.junit.Assert.fail;

import android.util.ArraySet;

import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnAttach;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnCreateOptionsMenu;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.core.lifecycle.events.OnOptionsItemSelected;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnPrepareOptionsMenu;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.core.lifecycle.events.SetPreferenceScreen;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class LifecycleObserverCodeInspector extends CodeInspector {

    private static final List<Class> LIFECYCLE_EVENTS = Arrays.asList(
            OnAttach.class,
            OnCreate.class,
            OnCreateOptionsMenu.class,
            OnDestroy.class,
            OnOptionsItemSelected.class,
            OnPause.class,
            OnPrepareOptionsMenu.class,
            OnResume.class,
            OnSaveInstanceState.class,
            OnStart.class,
            OnStop.class,
            SetPreferenceScreen.class
    );

    public LifecycleObserverCodeInspector(List<Class<?>> classes) {
        super(classes);
    }

    @Override
    public void run() {
        final Set<String> notImplementingLifecycleObserver = new ArraySet<>();
        for (Class clazz : mClasses) {
            if (!isConcreteSettingsClass(clazz)) {
                continue;
            }
            boolean classObservesLifecycleEvent = false;
            for (Class event : LIFECYCLE_EVENTS) {
                if (event.isAssignableFrom(clazz)) {
                    classObservesLifecycleEvent = true;
                    break;
                }
            }
            if (classObservesLifecycleEvent && !LifecycleObserver.class.isAssignableFrom(clazz)) {
                // Observes LifecycleEvent but not implementing LifecycleObserver. Something is
                // wrong.
                notImplementingLifecycleObserver.add(clazz.getName());
            }
        }
        if (!notImplementingLifecycleObserver.isEmpty()) {
            final String errorTemplate =
                    "The following class(es) implements lifecycle.events.*, but don't "
                            + "implement LifecycleObserver. Something is wrong:\n";
            final StringBuilder error = new StringBuilder(errorTemplate);
            for (String name : notImplementingLifecycleObserver) {
                error.append(name).append('\n');
            }
            fail(error.toString());
        }
    }
}
