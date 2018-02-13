package org.drools.examples.android;

import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.MediumTest;

import android.widget.Button;
import com.cnh.pf.product.test.AllTests;

import org.drools.examples.android.roboguice.kiecontainer.R;

import static org.junit.Assert.assertNotNull;


public class DroolsActivityInstrumentationTest extends ActivityInstrumentationTestCase2<DroolsActivity>  {


    public DroolsActivityInstrumentationTest() {
        super(DroolsActivity.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setActivityIntent(new Intent(getInstrumentation().getContext(), DroolsActivity.class));
    }
    
    @MediumTest
    public void testPreconditions() throws Throwable {
        final DroolsActivity a = getActivity();
        assertThat(a).isNotNull();
        assertThat(a.mContainer).as("kiecontainer").isNotNull();
        assertThat(a.mKieBase).as("kiebase").isNotNull();
        assertThat(a.kSession).as("kiesession").isNotNull();

        final Button fireRulesButton = (Button) a.findViewById(R.id.fireRules);
        assertThat(fireRulesButton).as("fireRulesButton").isNotNull();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                fireRulesButton.performClick();
            }
        });
        getInstrumentation().waitForIdleSync();
    }
}
