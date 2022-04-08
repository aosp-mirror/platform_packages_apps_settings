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

package com.android.settings.language;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.input.InputManager;
import android.os.UserManager;
import android.view.autofill.AutofillManager;
import android.view.inputmethod.InputMethodManager;
import android.view.textservice.TextServicesManager;

import androidx.lifecycle.LifecycleObserver;

import com.android.settings.R;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class LanguageAndInputSettingsTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Activity mActivity;
    @Mock
    private InputManager mIm;
    @Mock
    private InputMethodManager mImm;
    @Mock
    private DevicePolicyManager mDpm;
    @Mock
    private AutofillManager mAutofillManager;
    private TestFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mActivity.getSystemService(Context.USER_SERVICE)).thenReturn(mock(UserManager.class));
        when(mActivity.getSystemService(Context.INPUT_SERVICE))
                .thenReturn(mock(InputManager.class));
        when(mActivity.getSystemService(Context.INPUT_SERVICE)).thenReturn(mIm);
        when(mActivity.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE))
                .thenReturn(mock(TextServicesManager.class));
        when(mActivity.getSystemService(Context.DEVICE_POLICY_SERVICE)).thenReturn(mDpm);
        when(mActivity.getSystemService(Context.INPUT_METHOD_SERVICE)).thenReturn(mImm);
        when((Object) mActivity.getSystemService(AutofillManager.class))
                .thenReturn(mAutofillManager);
        mFragment = new TestFragment(mActivity);
    }

    @Test
    public void testGetPreferenceScreenResId() {
        assertThat(mFragment.getPreferenceScreenResId()).isEqualTo(R.xml.language_and_input);
    }

    @Test
    public void testGetPreferenceControllers_shouldRegisterLifecycleObservers() {
        final List<AbstractPreferenceController> controllers =
                mFragment.createPreferenceControllers(mActivity);
        int lifecycleObserverCount = 0;
        for (AbstractPreferenceController controller : controllers) {
            if (controller instanceof LifecycleObserver) {
                lifecycleObserverCount++;
            }
        }
        verify(mFragment.getSettingsLifecycle(), times(lifecycleObserverCount))
                .addObserver(any(LifecycleObserver.class));
    }

    @Test
    public void testGetPreferenceControllers_shouldAllBeCreated() {
        final List<AbstractPreferenceController> controllers =
                mFragment.createPreferenceControllers(mActivity);

        assertThat(controllers.isEmpty()).isFalse();
    }

    @Test
    public void testNonIndexableKeys_existInXmlLayout() {
        final Context context = spy(RuntimeEnvironment.application);
        final Resources res = spy(RuntimeEnvironment.application.getResources());
        final InputManager inputManager = mock(InputManager.class);
        final TextServicesManager textServicesManager = mock(TextServicesManager.class);
        when(inputManager.getInputDeviceIds()).thenReturn(new int[0]);
        doReturn(inputManager).when(context).getSystemService(Context.INPUT_SERVICE);
        doReturn(textServicesManager).when(context)
            .getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE);
        doReturn(res).when(context).getResources();
        doReturn(false).when(res)
                .getBoolean(com.android.internal.R.bool.config_supportSystemNavigationKeys);
        final List<String> niks =
            LanguageAndInputSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(context);
        LanguageAndInputSettings settings = new LanguageAndInputSettings();
        final int xmlId = settings.getPreferenceScreenResId();

        final List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(context, xmlId);

        assertThat(keys).containsAllIn(niks);
    }

    @Test
    public void testPreferenceControllers_getPreferenceKeys_existInPreferenceScreen() {
        final Context context = spy(RuntimeEnvironment.application);
        final TextServicesManager textServicesManager = mock(TextServicesManager.class);
        doReturn(textServicesManager).when(context)
            .getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE);
        final LanguageAndInputSettings fragment = new LanguageAndInputSettings();
        final List<String> preferenceScreenKeys =
            XmlTestUtils.getKeysFromPreferenceXml(context, fragment.getPreferenceScreenResId());
        final List<String> preferenceKeys = new ArrayList<>();

        for (AbstractPreferenceController controller : fragment.createPreferenceControllers(context)) {
            preferenceKeys.add(controller.getPreferenceKey());
        }

        assertThat(preferenceScreenKeys).containsAllIn(preferenceKeys);
    }

    /**
     * Test fragment to expose lifecycle and context so we can verify behavior for observables.
     */
    public static class TestFragment extends LanguageAndInputSettings {

        private Lifecycle mLifecycle;
        private Context mContext;

        public TestFragment(Context context) {
            mContext = context;
            mLifecycle = mock(Lifecycle.class);
        }

        @Override
        public Context getContext() {
            return mContext;
        }

        @Override
        public Lifecycle getSettingsLifecycle() {
            if (mLifecycle == null) {
                return super.getSettingsLifecycle();
            }
            return mLifecycle;
        }
    }
}
