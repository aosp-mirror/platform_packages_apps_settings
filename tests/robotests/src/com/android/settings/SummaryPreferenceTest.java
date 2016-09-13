package com.android.settings;

import android.content.Context;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SummaryPreferenceTest {

    private Context mContext;
    private PreferenceViewHolder mHolder;
    private SummaryPreference mPreference;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPreference = new SummaryPreference(mContext, null);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(mPreference.getLayoutResource(),
                new LinearLayout(mContext), false);

        mHolder = new PreferenceViewHolder(view);
    }

    @Test
    public void disableChart_shouldNotRender() {
        mPreference.setChartEnabled(false);
        mPreference.onBindViewHolder(mHolder);

        assertTrue(
                TextUtils.isEmpty(((TextView) mHolder.findViewById(android.R.id.text1)).getText()));
        assertTrue(
                TextUtils.isEmpty(((TextView) mHolder.findViewById(android.R.id.text2)).getText()));
    }

    @Test
    public void enableChart_shouldRender() {
        final String testLabel1 = "label1";
        final String testLabel2 = "label2";
        mPreference.setChartEnabled(true);
        mPreference.setLabels(testLabel1, testLabel2);
        mPreference.onBindViewHolder(mHolder);

        assertEquals(testLabel1,
                ((TextView) mHolder.findViewById(android.R.id.text1)).getText());
        assertEquals(testLabel2,
                ((TextView) mHolder.findViewById(android.R.id.text2)).getText());
    }

}
