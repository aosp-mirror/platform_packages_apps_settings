package com.android.settings.survey;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.overlay.SurveyFeatureProvider;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SurveyMixinTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private static final String FAKE_KEY = "fake_key";
    private SurveyFeatureProvider mProvider;

    @Before
    public void setUp() {
        // set up the fakefeature factory to mock out the survey provider
        mProvider = FakeFeatureFactory.setupForTest().getSurveyFeatureProvider(
                ApplicationProvider.getApplicationContext());
    }

    @Test
    public void onResume_noActionIfActivityDoesNotExist() {
        // Initialize a fragment without associating with an activity
        Fragment fragment = new Fragment();
        SurveyMixin mixin = new SurveyMixin(fragment, FAKE_KEY);
        mixin.onResume();

        verify(mProvider, times(0)).sendActivityIfAvailable(FAKE_KEY);
    }

    @Test
    public void onResume_sendActivityWhenSurveyFeatureExists() {
        try (var fragmentScenario = FragmentScenario.launch(Fragment.class)) {
            fragmentScenario.onFragment(fragment -> {
                SurveyMixin mixin = new SurveyMixin(fragment, FAKE_KEY);
                mixin.onResume();
            });
        }
        // Verify one send activity action is attempted
        verify(mProvider).sendActivityIfAvailable(FAKE_KEY);
    }
}
