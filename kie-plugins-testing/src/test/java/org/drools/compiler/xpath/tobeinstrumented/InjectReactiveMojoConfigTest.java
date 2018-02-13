package org.drools.compiler.xpath.tobeinstrumented;

import java.util.Arrays;
import java.util.List;

import org.drools.core.phreak.ReactiveObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.*;
import static org.kie.maven.plugin.InjectReactiveMojo.convertAllToPkgRegExps;
import static org.kie.maven.plugin.InjectReactiveMojo.isPackageNameIncluded;

public class InjectReactiveMojoConfigTest {

    private static Logger logger = LoggerFactory.getLogger(InjectReactiveMojoConfigTest.class);

    @Test
    public void testRegexpForPackagesDefault() {
        String[] inputConfig = new String[]{"*"};

        List<String> config = convertAllToPkgRegExps(inputConfig);

        logger.info(config.toString());

        assertThat(isPackageNameIncluded(Object.class.getPackage().getName().isTrue(),
                                         config));
        assertThat(isPackageNameIncluded(ReactiveObject.class.getPackage().getName().isTrue(),
                                         config));
        assertTrue(isPackageNameIncluded("xyz.my",
                                         config));
    }

    @Test
    public void testRegexpForPackagesSingleNoStars() {
        String[] inputConfig = new String[]{"org.drools"};

        List<String> config = convertAllToPkgRegExps(inputConfig);

        logger.info(config.toString());

        assertThat(isPackageNameIncluded(Object.class.getPackage().getName().isFalse(),
                                          config));
        assertThat(isPackageNameIncluded(ReactiveObject.class.getPackage().getName().isFalse(),
                                          config));
        assertFalse(isPackageNameIncluded("xyz.my",
                                          config));
    }

    @Test
    public void testRegexpForPackagesMultipleNoStars() {
        String[] inputConfig = new String[]{"org.drools", "xyz.my"};

        List<String> config = convertAllToPkgRegExps(inputConfig);

        logger.info(config.toString());

        assertThat(isPackageNameIncluded(Object.class.getPackage().getName().isFalse(),
                                          config));
        assertThat(isPackageNameIncluded(ReactiveObject.class.getPackage().getName().isFalse(),
                                          config));
        assertTrue(isPackageNameIncluded("xyz.my",
                                         config));
    }

    @Test
    public void testRegexpForPackagesSingleStars() {
        String[] inputConfig = new String[]{"org.drools.*"};

        List<String> config = convertAllToPkgRegExps(inputConfig);

        logger.info(config.toString());

        assertThat(isPackageNameIncluded(Object.class.getPackage().getName().isFalse(),
                                          config));
        assertThat(isPackageNameIncluded(ReactiveObject.class.getPackage().getName().isTrue(),
                                         config));
        assertFalse(isPackageNameIncluded("xyz.my",
                                          config));
    }

    @Test
    public void testRegexpForPackagesMultipleStars() {
        String[] inputConfig = new String[]{"org.drools.*", "xyz.my.*"};

        List<String> config = convertAllToPkgRegExps(inputConfig);

        logger.info(config.toString());

        assertThat(isPackageNameIncluded(Object.class.getPackage().getName().isFalse(),
                                          config));
        assertThat(isPackageNameIncluded(ReactiveObject.class.getPackage().getName().isTrue(),
                                         config));
        assertTrue(isPackageNameIncluded("xyz.my",
                                         config));
    }

    @Test
    public void testRegexpForPackagesCheckPart() {
        String[] inputConfig = new String[]{"my"};

        List<String> config = convertAllToPkgRegExps(inputConfig);

        logger.info(config.toString());

        assertThat(isPackageNameIncluded(Object.class.getPackage().getName().isFalse(),
                                          config));
        assertThat(isPackageNameIncluded(ReactiveObject.class.getPackage().getName().isFalse(),
                                          config));
        assertFalse(isPackageNameIncluded("xyz.my",
                                          config));
    }

    @Test
    public void testRegexpForPackagesCheckNaming() {
        String[] inputConfig = new String[]{"org.drools", "to.instrument.*"};

        List<String> config = convertAllToPkgRegExps(inputConfig);

        logger.info(config.toString());

        assertThat(isPackageNameIncluded(Object.class.getPackage().getName().isFalse(),
                                          config));
        assertThat(isPackageNameIncluded(ReactiveObject.class.getPackage().getName().isFalse(),
                                          config));
        assertFalse(isPackageNameIncluded("xyz.my",
                                          config));
        assertTrue(isPackageNameIncluded("to.instrument",
                                         config));
        assertFalse(isPackageNameIncluded("to.not.instrument",
                                          config));
    }
}
