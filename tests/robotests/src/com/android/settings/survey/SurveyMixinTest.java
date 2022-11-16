package com.android.settings.survey;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.overlay.SurveyFeatureProvider;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SurveyMixinTest {

    private static final String FAKE_KEY = "fake_key";

    private Context mContext;
    private SurveyFeatureProvider mProvider;
    @Mock
    private InstrumentedPreferenceFragment mFragment;

    @Before
    public void setUp() {
        // set up the fakefeature factory to mock out the survey provider
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mProvider = FakeFeatureFactory.setupForTest().getSurveyFeatureProvider(mContext);
    }

    @Test
    public void onResume_noActionIfActivityDoesNotExist() {
        // Pretend we are an activity that is starting up
        FragmentActivity temp = Robolectric.setupActivity(FragmentActivity.class);
        when(mFragment.getActivity()).thenReturn(null);
        SurveyMixin mixin = new SurveyMixin(mFragment, FAKE_KEY);
        mixin.onResume();

        verify(mProvider, times(0)).sendActivityIfAvailable(FAKE_KEY);
    }
}
