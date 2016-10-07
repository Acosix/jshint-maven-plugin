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
package de.acosix.maven.jshint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

/**
 * This Mojo provides a goal to run a JSHint validation of JavaScript sources files in the current project during the "processSources"
 * phase.
 *
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
@Mojo(name = "jshint", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class JSHintMojo extends AbstractMojo
{

    private static final boolean NASHORN_AVAILABLE;
    static
    {
        boolean nashornAvailable = false;
        try
        {
            final ScriptEngineManager engineManager = new ScriptEngineManager();
            final ScriptEngine nashornEngine = engineManager.getEngineByName("nashorn");
            nashornAvailable = nashornEngine != null;
        }
        catch (final Exception ex)
        {
            // ignore
        }
        NASHORN_AVAILABLE = nashornAvailable;
    }

    /**
     * The base directory of the current project
     */
    @Parameter(defaultValue = "${project.basedir}", property = "baseDirectory", required = true, readonly = true)
    protected File baseDirectory;

    /**
     * The source directory to process
     */
    @Parameter(defaultValue = "${basedir}/src/main", property = "sourceDirectory", required = true)
    protected File sourceDirectory;

    /**
     * The default JSHint config file to use for running JSHint validations. When {@link #ignoreJSHintConfigFiles} is set to {@code false}
     * this may be overriden by {@code .jshintrc} files found in the {@link #sourceDirectory}. This can be a path that will be resolved
     * first against the {@link #baseDirectory} of the current project, falling back on classpath resolution against classpath of the plugin
     * and its configured dependency.
     */
    @Parameter(defaultValue = "jshint.config.json", property = "sourceDirectory", required = true)
    protected String jsHintDefaultConfigFile;

    /**
     * The files to explicitly include in JSHint validation - defaults to all files within the source directory if not set
     */
    @Parameter(property = "includes", required = false)
    protected List<String> includes;

    /**
     * The files to explicitly exclude from JSHint validation - this will always override includes
     */
    @Parameter(property = "excludes", required = false)
    protected List<String> excludes;

    /**
     * Flag to specify if JSHint errors should fail the build
     */
    @Parameter(property = "failOnError", required = false)
    protected boolean failOnError = true;

    /**
     * Flag to specify if Rhino should always be used even if Nashorn is available
     */
    @Parameter(property = "preferRhino", required = false)
    protected boolean preferRhino = false;

    /**
     * Flag to specify that any {@code .jshintignore} files found in the source directory should be ignored
     */
    @Parameter(property = "ignoreJSHintIgnoreFiles", required = false)
    protected boolean ignoreJSHintIgnoreFiles = false;

    /**
     * Flag to specify that any {@code .jshintrc} files found in the source directory should be ignored
     */
    @Parameter(property = "ignoreJSHintConfigFiles", required = false)
    protected boolean ignoreJSHintConfigFiles = false;

    /**
     * The version of the embedded JSHint script to use. This setting is ignored if {@link #jshintScript} is set.
     */
    @Parameter(defaultValue = "2.9.3", property = "jshintVersion", required = true)
    protected String jshintVersion;

    /**
     * The path to the JSHint script to use - this path is resolved against the project base directory first and then the classpath of the
     * plugin including any configured plugin dependencies. This overrides the {@link #jshintVersion} setting.
     */
    @Parameter(property = "jshintScript", required = false)
    protected String jshintScript;

    // setters primarily to facilitate testing

    /**
     * @param baseDirectory
     *            the baseDirectory to set
     */
    public void setBaseDirectory(final File baseDirectory)
    {
        this.baseDirectory = baseDirectory;
    }

    /**
     * @param sourceDirectory
     *            the sourceDirectory to set
     */
    public void setSourceDirectory(final File sourceDirectory)
    {
        this.sourceDirectory = sourceDirectory;
    }

    /**
     * @param jsHintDefaultConfigFile
     *            the jsHintDefaultConfigFile to set
     */
    public void setJsHintDefaultConfigFile(final String jsHintDefaultConfigFile)
    {
        this.jsHintDefaultConfigFile = jsHintDefaultConfigFile;
    }

    /**
     * @param includes
     *            the includes to set
     */
    public void setIncludes(final List<String> includes)
    {
        this.includes = includes;
    }

    /**
     * @param excludes
     *            the excludes to set
     */
    public void setExcludes(final List<String> excludes)
    {
        this.excludes = excludes;
    }

    /**
     * @param failOnError
     *            the failOnError to set
     */
    public void setFailOnError(final boolean failOnError)
    {
        this.failOnError = failOnError;
    }

    /**
     * @param preferRhino
     *            the preferRhino to set
     */
    public void setPreferRhino(final boolean preferRhino)
    {
        this.preferRhino = preferRhino;
    }

    /**
     * @param ignoreJSHintIgnoreFiles
     *            the ignoreJSHintIgnoreFiles to set
     */
    public void setIgnoreJSHintIgnoreFiles(final boolean ignoreJSHintIgnoreFiles)
    {
        this.ignoreJSHintIgnoreFiles = ignoreJSHintIgnoreFiles;
    }

    /**
     * @param ignoreJSHintConfigFiles
     *            the ignoreJSHintConfigFiles to set
     */
    public void setIgnoreJSHintConfigFiles(final boolean ignoreJSHintConfigFiles)
    {
        this.ignoreJSHintConfigFiles = ignoreJSHintConfigFiles;
    }

    /**
     * @param jshintVersion
     *            the jshintVersion to set
     */
    public void setJshintVersion(final String jshintVersion)
    {
        this.jshintVersion = jshintVersion;
    }

    /**
     * @param jshintScript
     *            the jshintScript to set
     */
    public void setJshintScript(final String jshintScript)
    {
        this.jshintScript = jshintScript;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        try
        {
            final List<String> scriptFilesToProcess = this.lookupJavaScriptFilesToInclude();

            final JSHinter hinter;

            if (this.preferRhino || !NASHORN_AVAILABLE)
            {
                if (this.jshintScript != null)
                {
                    final File scriptFile = new File(this.baseDirectory, this.jshintScript);
                    if (scriptFile.isFile() && scriptFile.exists())
                    {
                        hinter = new RhinoJSHinter(this.getLog(), scriptFile);
                    }
                    else
                    {
                        hinter = new RhinoJSHinter(this.getLog(), this.jshintScript, true);
                    }
                }
                else
                {
                    hinter = new RhinoJSHinter(this.getLog(), this.jshintVersion, false);
                }
            }
            else
            {
                throw new MojoExecutionException("Nashorn is not yet supported");
            }

            final String defaultJSHintConfigContent = this.loadDefaultJSHintConfig();

            int filesChecked = 0;
            int filesWithErrors = 0;
            for (final String scriptFile : scriptFilesToProcess)
            {
                final List<Error> errors = hinter.executeJSHint(this.sourceDirectory, scriptFile, defaultJSHintConfigContent,
                        this.ignoreJSHintConfigFiles);

                if (!errors.isEmpty())
                {
                    filesWithErrors++;
                }
                filesChecked++;
            }

            this.getLog().info("JSHint validation complete");

            if (filesWithErrors > 0)
            {
                final String message = MessageFormat.format("JSHint errors found in {0} source files", String.valueOf(filesWithErrors));
                this.getLog().error(message);

                if (this.failOnError)
                {
                    throw new MojoFailureException(message);
                }
            }
            else
            {
                this.getLog().info(MessageFormat.format("No JSHint errors found in {0} source files", String.valueOf(filesChecked)));
            }
        }
        catch (final RuntimeException re)
        {
            if (re.getCause() instanceof MojoExecutionException)
            {
                throw (MojoExecutionException) re.getCause();
            }
            throw re;
        }
    }

    protected String loadDefaultJSHintConfig()
    {
        String defaultJSHintConfigContent;
        final File jsHintDefaultConfigFile = new File(this.baseDirectory, this.jsHintDefaultConfigFile);
        if (!jsHintDefaultConfigFile.isFile() || !jsHintDefaultConfigFile.exists())
        {
            final URL jsHintDefaultConfigResource = JSHintMojo.class.getClassLoader().getResource(this.jsHintDefaultConfigFile);
            if (jsHintDefaultConfigResource != null)
            {
                Reader jsHintConfigReader = null;
                InputStream is = null;
                try
                {
                    is = jsHintDefaultConfigResource.openStream();
                    jsHintConfigReader = new InputStreamReader(is, StandardCharsets.UTF_8);
                    defaultJSHintConfigContent = IOUtil.toString(jsHintConfigReader);
                }
                catch (final IOException ioex)
                {
                    IOUtil.close(jsHintConfigReader);
                    IOUtil.close(is);

                    throw new RuntimeException(
                            new MojoExecutionException("Error reading default JSHint config from " + this.jsHintDefaultConfigFile, ioex));
                }
            }
            else
            {
                defaultJSHintConfigContent = "{}";
            }
        }
        else
        {
            Reader jsHintConfigReader = null;
            FileInputStream fis = null;
            try
            {
                fis = new FileInputStream(jsHintDefaultConfigFile);
                jsHintConfigReader = new InputStreamReader(fis, StandardCharsets.UTF_8);
                defaultJSHintConfigContent = IOUtil.toString(jsHintConfigReader);
            }
            catch (final IOException ioex)
            {
                IOUtil.close(jsHintConfigReader);
                IOUtil.close(fis);

                throw new RuntimeException(
                        new MojoExecutionException("Error reading default JSHint config file" + this.jsHintDefaultConfigFile, ioex));
            }
        }

        if (this.getLog().isDebugEnabled())
        {
            this.getLog().debug("Loaded default JSHint config: " + defaultJSHintConfigContent);
        }

        return defaultJSHintConfigContent;
    }

    protected List<String> lookupJavaScriptFilesToInclude()
    {
        final DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(this.sourceDirectory);

        if (this.includes != null && !this.includes.isEmpty())
        {
            if (this.getLog().isDebugEnabled())
            {
                this.getLog().debug("Using configured inclusion patterns: " + this.includes);
            }
            scanner.setIncludes(this.includes.toArray(new String[0]));
        }
        else
        {
            this.getLog().debug("Using default inclusion patterns *.js and  **/*.js");
            scanner.setIncludes(new String[] { "*.js", "**/*.js" });
        }

        final List<String> effectiveExcludes = new ArrayList<>();
        if (this.excludes != null && !this.excludes.isEmpty())
        {
            if (this.getLog().isDebugEnabled())
            {
                this.getLog().debug("Using configured exclusion patterns: " + this.excludes);
            }
            effectiveExcludes.addAll(this.excludes);
        }

        if (!this.ignoreJSHintIgnoreFiles)
        {
            final List<String> jshintIgnoreExcludes = this.loadExcludesFromJSHintIgnores();
            effectiveExcludes.addAll(jshintIgnoreExcludes);
        }

        scanner.setExcludes(effectiveExcludes.toArray(new String[0]));
        scanner.addDefaultExcludes();
        scanner.scan();

        final List<String> javaScriptFilesToInclude = Arrays.asList(scanner.getIncludedFiles());

        if (this.getLog().isDebugEnabled())
        {
            this.getLog().debug("Determined set of JavaScript files to process: " + javaScriptFilesToInclude);
        }

        return javaScriptFilesToInclude;
    }

    protected List<String> loadExcludesFromJSHintIgnores()
    {
        final DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(this.sourceDirectory);

        scanner.setIncludes(new String[] { ".jshintignore", "**/.jshintignore" });
        scanner.scan();

        final List<String> excludes = new ArrayList<>();
        final String[] jshintIgnores = scanner.getIncludedFiles();
        for (final String jshintIgnore : jshintIgnores)
        {
            final String path;
            if (jshintIgnore.contains(File.separator))
            {
                path = jshintIgnore.substring(0, jshintIgnore.lastIndexOf(File.separator));
            }
            else
            {
                path = null;
            }

            FileInputStream fin = null;
            InputStreamReader isr = null;
            BufferedReader reader = null;
            try
            {
                final File jshintIgnoreFile = new File(this.sourceDirectory, jshintIgnore);
                fin = new FileInputStream(jshintIgnoreFile);
                isr = new InputStreamReader(fin, StandardCharsets.UTF_8);
                reader = new BufferedReader(isr);

                String line = null;
                while ((line = reader.readLine()) != null)
                {
                    if (!StringUtils.isBlank(line))
                    {
                        if (path != null)
                        {
                            excludes.add(path + File.separator + line);
                        }
                        else
                        {
                            excludes.add(line);
                        }
                    }
                }
            }
            catch (final IOException ioex)
            {
                throw new RuntimeException(new MojoExecutionException("Error loading .jshintignore", ioex));
            }
            finally
            {
                IOUtil.close(reader);
                IOUtil.close(isr);
                IOUtil.close(fin);
            }
        }

        if (this.getLog().isDebugEnabled())
        {
            this.getLog().debug("Loaded excludes from .jshintignore files: " + excludes);
        }

        return excludes;
    }
}
