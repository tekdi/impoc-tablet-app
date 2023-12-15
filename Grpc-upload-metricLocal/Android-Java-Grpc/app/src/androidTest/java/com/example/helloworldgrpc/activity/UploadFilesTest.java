package com.example.helloworldgrpc.activity;

import static org.junit.Assert.*;

import android.widget.ListView;
import android.widget.TextView;

import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.example.helloworldgrpc.R;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class UploadFilesTest {
    @Rule
    public ActivityScenarioRule<UploadFiles> mActivityRule = new ActivityScenarioRule<>(
            UploadFiles.class);

    @Test
    public void checkForListingview() {
        mActivityRule.getScenario().onActivity(activity -> {
            ListView list = activity.findViewById(R.id.rcv_files);
            Assert.assertNotNull(list);
        });
    }

    @Test
    public void checkForEmptyview() {
        mActivityRule.getScenario().onActivity(activity -> {
            TextView frameEmpty = activity.findViewById(R.id.frame_empty);
            Assert.assertNotNull(frameEmpty);
        });
    }
}