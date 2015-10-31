package org.sergez.splayer.ui;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * @author Sergii Zhuk
 *         Date: 29.09.2014
 *         Time: 11:37
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class PreferencesActivityTest {

    private PreferencesActivity activity;

    @Before
    public void setup() {
        activity = Robolectric.buildActivity(PreferencesActivity.class).create().get();
    }

    @Test
    public void checkActivityNotNull() throws Exception {
        Assert.assertNotNull(activity);

    }

}
