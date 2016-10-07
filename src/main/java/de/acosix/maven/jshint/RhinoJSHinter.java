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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class RhinoJSHinter implements JSHinter
{

    protected final Log log;

    protected final Object jshintScript;

    protected Map<File, String> effectiveJSHintConfig = new HashMap<>();

    protected Scriptable scope;

    protected Script runnerScript;

    public RhinoJSHinter(final Log log, final String versionOrResourcePath, final boolean resourcePath)
    {
        if (log == null)
        {
            throw new IllegalArgumentException("log not provided");
        }

        if (StringUtils.isBlank(versionOrResourcePath))
        {
            throw new IllegalArgumentException("versionOrResourcePath not provided");
        }

        this.log = log;

        if (!resourcePath)
        {
            final String scriptName = "jshint-" + versionOrResourcePath + "-rhino.js";
            this.jshintScript = RhinoJSHinter.class.getResource(scriptName);

            if (this.jshintScript == null)
            {
                this.log.error("JSHint script could not be resolved for version " + versionOrResourcePath);
                throw new RuntimeException(new MojoExecutionException("Error resolving " + scriptName));
            }
        }
        else
        {
            this.jshintScript = RhinoJSHinter.class.getClassLoader().getResource(versionOrResourcePath);

            if (this.jshintScript == null)
            {
                this.log.error("JSHint script could not be resolved from resource path " + versionOrResourcePath);
                throw new RuntimeException(new MojoExecutionException("Error resolving " + versionOrResourcePath));
            }
        }
    }

    public RhinoJSHinter(final Log log, final File jshintScriptFile)
    {
        if (log == null)
        {
            throw new IllegalArgumentException("log not provided");
        }

        if (jshintScriptFile == null)
        {
            throw new IllegalArgumentException("jshintScriptFile not provided");
        }

        this.log = log;

        this.jshintScript = jshintScriptFile;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public List<Error> executeJSHint(final File baseDirectory, final String path, final String defaultJSHintConfigContent,
            final boolean ignoreJSConfigFileOnPaths)
    {
        if (baseDirectory == null)
        {
            throw new IllegalArgumentException("baseDirectory not provided");
        }

        if (StringUtils.isBlank(path))
        {
            throw new IllegalArgumentException("path not provided");
        }

        if (StringUtils.isBlank(defaultJSHintConfigContent))
        {
            throw new IllegalArgumentException("defaultJSHintConfigContent not provided");
        }

        this.ensureEngineInitialisation();

        this.log.info(MessageFormat.format("Executing JSHint on {0}{1}{2}", baseDirectory.getPath(), File.separator, path));

        String effectiveJSHintConfigContent;
        if (ignoreJSConfigFileOnPaths)
        {
            effectiveJSHintConfigContent = defaultJSHintConfigContent;
        }
        else
        {
            effectiveJSHintConfigContent = this.lookupCustomJSHintConfig(baseDirectory, path);
            if (StringUtils.isBlank(effectiveJSHintConfigContent))
            {
                effectiveJSHintConfigContent = defaultJSHintConfigContent;
            }
        }

        final List<Error> errors = new ArrayList<>();
        final Context cx = Context.enter();
        try
        {
            final List<String> sourceLines = this.readSourceLines(baseDirectory, path);
            final Scriptable sourceLinesArr = cx.newArray(this.scope, sourceLines.toArray(new Object[0]));

            this.scope.put("errors", this.scope, errors);
            this.scope.put("sourceLines", this.scope, sourceLinesArr);
            this.scope.put("jshintConfig", this.scope, effectiveJSHintConfigContent);

            this.runnerScript.exec(cx, this.scope);

            this.reportErrors(errors);
        }
        finally
        {
            Context.exit();
        }
        return errors;
    }

    protected void reportErrors(final List<Error> errors)
    {
        for (final Error error : errors)
        {
            this.log.error(MessageFormat.format("{0},{1}: {2} ({3})", String.valueOf(error.getLine()), String.valueOf(error.getCharacter()),
                    error.getReason(), error.getCode()));
        }
    }

    protected void ensureEngineInitialisation()
    {
        if (this.scope == null)
        {
            this.log.debug("Initialising Rhino context for JSHint");

            final Context cx = Context.enter();
            try
            {
                this.scope = cx.initStandardObjects(null, false);

                final Script jshintScript = this.compileJSHintScript(cx);
                // execute once to actually load JSHINT
                jshintScript.exec(cx, this.scope);

                // the runner script is our wrapper for repeated execution
                this.runnerScript = this.compileRunnerScript(cx);
            }
            catch (final IOException ioex)
            {
                this.log.error("Error initialising Rhino context for JSHint");
                throw new RuntimeException(new MojoExecutionException("Error initialising Rhino context for JSHint", ioex));
            }
            finally
            {
                Context.exit();
            }
        }
    }

    protected Script compileJSHintScript(final Context cx) throws IOException
    {
        Script jshintScript;
        if (this.jshintScript instanceof URL)
        {
            final URL jshintScriptURL = (URL) this.jshintScript;
            final InputStream scriptInputStream = jshintScriptURL.openStream();
            try
            {
                jshintScript = this.compileScript(cx, jshintScriptURL.toExternalForm(), scriptInputStream);
            }
            finally
            {
                IOUtil.close(scriptInputStream);
            }
        }
        else if (this.jshintScript instanceof File)
        {
            final InputStream scriptInputStream = new FileInputStream((File) this.jshintScript);
            try
            {
                jshintScript = this.compileScript(cx, ((File) this.jshintScript).getAbsolutePath(), scriptInputStream);
            }
            finally
            {
                IOUtil.close(scriptInputStream);
            }
        }
        else
        {
            throw new RuntimeException(new MojoExecutionException("JSHint script has not been resolved"));
        }

        return jshintScript;
    }

    protected Script compileRunnerScript(final Context cx) throws IOException
    {
        final InputStream scriptInputStream = RhinoJSHinter.class.getResourceAsStream("jshint-rhino-runner.js");
        try
        {
            final Script script = this.compileScript(cx, "jshint-rhino-runner.js", scriptInputStream);
            return script;
        }
        finally
        {
            IOUtil.close(scriptInputStream);
        }
    }

    protected Script compileScript(final Context cx, final String name, final InputStream is) throws IOException
    {
        final Reader scriptReader = new InputStreamReader(is, StandardCharsets.UTF_8);
        final Script script = cx.compileReader(scriptReader, name, 1, null);
        return script;
    }

    protected List<String> readSourceLines(final File baseDirectory, final String path)
    {
        final List<String> sourceLines = new ArrayList<>();

        FileInputStream fin = null;
        InputStreamReader isr = null;
        BufferedReader reader = null;
        try
        {
            final File jshintIgnoreFile = new File(baseDirectory, path);
            fin = new FileInputStream(jshintIgnoreFile);
            isr = new InputStreamReader(fin, StandardCharsets.UTF_8);
            reader = new BufferedReader(isr);

            String line = null;
            while ((line = reader.readLine()) != null)
            {
                sourceLines.add(line);
            }
        }
        catch (final IOException ioex)
        {
            throw new RuntimeException(new MojoExecutionException("Error loading " + baseDirectory + File.separator + path, ioex));
        }
        finally
        {
            IOUtil.close(reader);
            IOUtil.close(isr);
            IOUtil.close(fin);
        }

        return sourceLines;
    }

    protected String lookupCustomJSHintConfig(final File baseDirectory, final String path)
    {
        String customJSHintConfig = null;

        final int lastSeparatorIdx = path.lastIndexOf(File.separator);
        if (lastSeparatorIdx != -1)
        {
            final String subPath = path.substring(0, lastSeparatorIdx);

            final File pathKey = new File(baseDirectory, subPath);
            if (this.effectiveJSHintConfig.containsKey(pathKey))
            {
                customJSHintConfig = this.effectiveJSHintConfig.get(pathKey);
            }
            else
            {
                final File configFile = new File(pathKey, ".jshintrc");
                if (configFile.isFile() && configFile.exists())
                {
                    customJSHintConfig = this.readSource(configFile);
                    this.effectiveJSHintConfig.put(pathKey, customJSHintConfig);
                }
                else
                {
                    customJSHintConfig = this.lookupCustomJSHintConfig(baseDirectory, subPath);
                }
            }
        }
        else
        {
            if (this.effectiveJSHintConfig.containsKey(baseDirectory))
            {
                customJSHintConfig = this.effectiveJSHintConfig.get(baseDirectory);
            }
            else
            {
                final File configFile = new File(baseDirectory, ".jshintrc");
                if (configFile.isFile() && configFile.exists())
                {
                    customJSHintConfig = this.readSource(configFile);
                    this.effectiveJSHintConfig.put(baseDirectory, customJSHintConfig);
                }
            }
        }

        return customJSHintConfig;
    }

    protected String readSource(final File contentFile)
    {
        final String source;

        FileInputStream fin = null;
        InputStreamReader isr = null;
        try
        {
            fin = new FileInputStream(contentFile);
            isr = new InputStreamReader(fin, StandardCharsets.UTF_8);
            source = IOUtil.toString(isr);
        }
        catch (final IOException ioex)
        {
            throw new RuntimeException(new MojoExecutionException("Error loading " + contentFile.toPath(), ioex));
        }
        finally
        {
            IOUtil.close(isr);
            IOUtil.close(fin);
        }

        return source;
    }
}
