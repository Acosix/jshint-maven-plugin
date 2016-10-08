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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.codehaus.plexus.util.ReaderFactory;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class TestProjectStub extends MavenProjectStub
{

    protected final String testResourcesPath;

    /**
     * Default constructor
     */
    public TestProjectStub(final String testResourcesPath)
    {
        this.testResourcesPath = testResourcesPath;

        final MavenXpp3Reader pomReader = new MavenXpp3Reader();
        Model model;
        final File basedir = this.getBasedir();
        try
        {
            model = pomReader.read(ReaderFactory.newXmlReader(new File(basedir, "pom.xml")));
            this.setModel(model);
        }
        catch (final Exception e)
        {
            throw new RuntimeException(e);
        }

        this.setGroupId(model.getGroupId());
        this.setArtifactId(model.getArtifactId());
        this.setVersion(model.getVersion());
        this.setName(model.getName());
        this.setUrl(model.getUrl());
        this.setPackaging(model.getPackaging());

        final Build build = new Build();
        build.setFinalName(model.getArtifactId());
        build.setDirectory(basedir + "/target");
        build.setSourceDirectory(basedir + "/src/main/java");
        build.setOutputDirectory(basedir + "/target/classes");
        build.setTestSourceDirectory(basedir + "/src/test/java");
        build.setTestOutputDirectory(basedir + "/target/test-classes");
        this.setBuild(build);

        final List<String> compileSourceRoots = new ArrayList<>();
        compileSourceRoots.add(basedir + "/src/main/java");
        this.setCompileSourceRoots(compileSourceRoots);

        final List<String> testCompileSourceRoots = new ArrayList<>();
        testCompileSourceRoots.add(basedir + "/src/test/java");
        this.setTestCompileSourceRoots(testCompileSourceRoots);
    }

    /** {@inheritDoc} */
    @Override
    public File getBasedir()
    {
        return new File(super.getBasedir() + "/" + this.testResourcesPath + "/");
    }
}
