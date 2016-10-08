/*
 * Copyright 2016 Acosix GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.acosix.maven.jshint.test;

import java.util.Arrays;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.acosix.maven.jshint.JSHintMojo;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class JSHintMojoTest
{

    // isDebugEnabled in base class yields false instead of debug printing output
    private static final Log FIXED_SYSTEM_STREAM_LOG = new SystemStreamLog()
    {

        /**
         *
         * {@inheritDoc}
         */
        @Override
        public boolean isDebugEnabled()
        {
            return true;
        }
    };

    @Rule
    public MojoRule rule = new MojoRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testInvalidJSHintDefaultConfigWithCommentNashorn() throws Exception
    {
        final TestProjectStub projectStub = new TestProjectStub("src/test/resources/projects/generic-tests");
        final Mojo mojo = this.rule.lookupConfiguredMojo(projectStub, "jshint");

        Assert.assertNotNull("JSHintMojo was not found", mojo);
        Assert.assertTrue("JSHintMojo is not of expected type", mojo instanceof JSHintMojo);

        mojo.setLog(FIXED_SYSTEM_STREAM_LOG);

        final JSHintMojo jsHintMojo = (JSHintMojo) mojo;
        jsHintMojo.setJsHintDefaultConfigFile("jshint.config-with-comments.json");
        // need to exclude the locations with custom config overrides
        jsHintMojo.setExcludes(Arrays.asList("test-jshintrc/*.js"));

        this.thrown.expect(MojoExecutionException.class);
        jsHintMojo.execute();
    }

    @Test
    public void testInvalidJSHintDefaultConfigWithCommentRhino() throws Exception
    {
        final TestProjectStub projectStub = new TestProjectStub("src/test/resources/projects/generic-tests");
        final Mojo mojo = this.rule.lookupConfiguredMojo(projectStub, "jshint");

        Assert.assertNotNull("JSHintMojo was not found", mojo);
        Assert.assertTrue("JSHintMojo is not of expected type", mojo instanceof JSHintMojo);

        mojo.setLog(FIXED_SYSTEM_STREAM_LOG);

        final JSHintMojo jsHintMojo = (JSHintMojo) mojo;
        jsHintMojo.setJsHintDefaultConfigFile("jshint.config-with-comments.json");
        jsHintMojo.setPreferRhino(true);
        // need to exclude the locations with custom config overrides
        jsHintMojo.setExcludes(Arrays.asList("test-jshintrc/*.js"));

        this.thrown.expect(MojoExecutionException.class);
        jsHintMojo.execute();
    }

    @Test
    public void testJSHintIgnoreNashorn() throws Exception
    {
        final TestProjectStub projectStub = new TestProjectStub("src/test/resources/projects/generic-tests");
        final Mojo mojo = this.rule.lookupConfiguredMojo(projectStub, "jshint");

        Assert.assertNotNull("JSHintMojo was not found", mojo);
        Assert.assertTrue("JSHintMojo is not of expected type", mojo instanceof JSHintMojo);

        mojo.setLog(FIXED_SYSTEM_STREAM_LOG);

        final JSHintMojo jsHintMojo = (JSHintMojo) mojo;
        jsHintMojo.setJsHintDefaultConfigFile("jshint.config-acosix-default.json");
        jsHintMojo.setIncludes(Arrays.asList("*.js", "test-jshintignore/*.js"));

        // should not fail as long as .jshintignore is respected
        jsHintMojo.execute();

        // should now fail since .jshintignore should not be respected
        jsHintMojo.setIgnoreJSHintIgnoreFiles(true);
        this.thrown.expect(MojoFailureException.class);
        jsHintMojo.execute();
    }

    @Test
    public void testJSHintIgnoreRhino() throws Exception
    {
        final TestProjectStub projectStub = new TestProjectStub("src/test/resources/projects/generic-tests");
        final Mojo mojo = this.rule.lookupConfiguredMojo(projectStub, "jshint");

        Assert.assertNotNull("JSHintMojo was not found", mojo);
        Assert.assertTrue("JSHintMojo is not of expected type", mojo instanceof JSHintMojo);

        mojo.setLog(FIXED_SYSTEM_STREAM_LOG);

        final JSHintMojo jsHintMojo = (JSHintMojo) mojo;
        jsHintMojo.setPreferRhino(true);
        jsHintMojo.setJsHintDefaultConfigFile("jshint.config-acosix-default.json");
        jsHintMojo.setIncludes(Arrays.asList("*.js", "test-jshintignore/*.js"));

        // should not fail as long as .jshintignore is respected
        jsHintMojo.execute();

        // should now fail since .jshintignore should not be respected
        jsHintMojo.setIgnoreJSHintIgnoreFiles(true);
        this.thrown.expect(MojoFailureException.class);
        jsHintMojo.execute();
    }

    @Test
    public void testJSHintInlineConfigNashorn() throws Exception
    {
        final TestProjectStub projectStub = new TestProjectStub("src/test/resources/projects/generic-tests");
        final Mojo mojo = this.rule.lookupConfiguredMojo(projectStub, "jshint");

        Assert.assertNotNull("JSHintMojo was not found", mojo);
        Assert.assertTrue("JSHintMojo is not of expected type", mojo instanceof JSHintMojo);

        mojo.setLog(FIXED_SYSTEM_STREAM_LOG);

        final JSHintMojo jsHintMojo = (JSHintMojo) mojo;
        jsHintMojo.setJsHintDefaultConfigFile("jshint.config-acosix-default.json");
        jsHintMojo.setIncludes(Arrays.asList("*.js", "test-jshintrc/*.js"));

        // should not fail as long as .jshintrc is respected
        jsHintMojo.execute();

        // should now fail since .jshintrc should not be respected
        jsHintMojo.setIgnoreJSHintConfigFiles(true);
        this.thrown.expect(MojoFailureException.class);
        jsHintMojo.execute();
    }

    @Test
    public void testJSHintInlineConfigRhino() throws Exception
    {
        final TestProjectStub projectStub = new TestProjectStub("src/test/resources/projects/generic-tests");
        final Mojo mojo = this.rule.lookupConfiguredMojo(projectStub, "jshint");

        Assert.assertNotNull("JSHintMojo was not found", mojo);
        Assert.assertTrue("JSHintMojo is not of expected type", mojo instanceof JSHintMojo);

        mojo.setLog(FIXED_SYSTEM_STREAM_LOG);

        final JSHintMojo jsHintMojo = (JSHintMojo) mojo;
        jsHintMojo.setPreferRhino(true);
        jsHintMojo.setJsHintDefaultConfigFile("jshint.config-acosix-default.json");
        jsHintMojo.setIncludes(Arrays.asList("*.js", "test-jshintrc/*.js"));

        // should not fail as long as .jshintrc is respected
        jsHintMojo.execute();

        // should now fail since .jshintrc should not be respected
        jsHintMojo.setIgnoreJSHintConfigFiles(true);
        this.thrown.expect(MojoFailureException.class);
        jsHintMojo.execute();
    }
}
