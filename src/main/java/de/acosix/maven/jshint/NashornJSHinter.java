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

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class NashornJSHinter extends AbstractJSHinter
{

    protected final ScriptEngine nashornEngine = new ScriptEngineManager().getEngineByName("nashorn");

    protected final Bindings bindings = this.nashornEngine.createBindings();

    protected final Object jshintScript;

    protected boolean jshintScriptLoaded = false;

    public NashornJSHinter(final Log log, final String versionOrResourcePath, final boolean resourcePath)
    {
        super(log);

        if (StringUtils.isBlank(versionOrResourcePath))
        {
            throw new IllegalArgumentException("versionOrResourcePath not provided");
        }

        if (!resourcePath)
        {
            final String scriptName = "jshint-" + versionOrResourcePath + "-nashorn.js";
            this.jshintScript = NashornJSHinter.class.getResource(scriptName);

            if (this.jshintScript == null)
            {
                this.log.error("JSHint script could not be resolved for version " + versionOrResourcePath);
                throw new RuntimeException(new MojoExecutionException("Error resolving " + scriptName));
            }
        }
        else
        {
            this.jshintScript = NashornJSHinter.class.getClassLoader().getResource(versionOrResourcePath);

            if (this.jshintScript == null)
            {
                this.log.error("JSHint script could not be resolved from resource path " + versionOrResourcePath);
                throw new RuntimeException(new MojoExecutionException("Error resolving " + versionOrResourcePath));
            }
        }
    }

    public NashornJSHinter(final Log log, final File jshintScriptFile)
    {
        super(log);
        this.jshintScript = jshintScriptFile;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected List<Error> executeJSHintImpl(final File baseDirectory, final String path, final String effectiveJSHintConfigContent)
    {
        this.ensureEngineInitialisation();

        final List<Error> errors = new ArrayList<>();

        final List<String> sourceLines = this.readSourceLines(baseDirectory, path);
        this.bindings.put("sourceLines", sourceLines);
        this.bindings.put("errors", errors);
        this.bindings.put("jshintConfig", effectiveJSHintConfigContent);

        final URL runnerScript = NashornJSHinter.class.getResource("jshint-nashorn-runner.js");
        this.bindings.put("runnerScript", runnerScript);

        try
        {
            this.nashornEngine.eval("load(runnerScript);", this.bindings);
        }
        catch (final ScriptException sex)
        {
            throw new RuntimeException(new MojoExecutionException("Error running jshint validations", sex));
        }

        return errors;
    }

    protected void ensureEngineInitialisation()
    {
        if (!this.jshintScriptLoaded)
        {
            this.log.debug("Initialising Nashorn context for JSHint");

            this.bindings.put("jshintScript", this.jshintScript);
            try
            {
                this.nashornEngine.eval("load(jshintScript);", this.bindings);
            }
            catch (final ScriptException sex)
            {
                throw new RuntimeException(new MojoExecutionException("Error loading jshint script", sex));
            }

            this.jshintScriptLoaded = true;
        }
    }
}
