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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.IOUtil;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrappedException;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class RhinoJSHinter extends AbstractJSHinter
{

    protected Scriptable scope;

    protected Script runnerScript;

    public RhinoJSHinter(final Log log, final String versionOrResourcePath, final boolean resourcePath)
    {
        super(log, versionOrResourcePath, resourcePath);
    }

    public RhinoJSHinter(final Log log, final File jshintScriptFile)
    {
        super(log, jshintScriptFile);
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
        final Context cx = Context.enter();
        try
        {
            final List<String> sourceLines = this.readSourceLines(baseDirectory, path);
            final Scriptable sourceLinesArr = cx.newArray(this.scope, sourceLines.toArray(new Object[0]));

            this.scope.put("errors", this.scope, errors);
            this.scope.put("sourceLines", this.scope, sourceLinesArr);
            this.scope.put("jshintConfig", this.scope, effectiveJSHintConfigContent);

            this.runnerScript.exec(cx, this.scope);
        }
        catch (final JavaScriptException jse)
        {
            // a Java exception triggered via JS code may be wrapped in this
            Object value = jse.getValue();
            if (value instanceof NativeJavaObject)
            {
                value = ((NativeJavaObject) value).unwrap();
            }

            if (value instanceof RuntimeException)
            {
                throw (RuntimeException) value;
            }
            throw jse;
        }
        catch (final WrappedException we)
        {
            final Throwable wrapped = we.getWrappedException();
            if (wrapped instanceof RuntimeException)
            {
                throw (RuntimeException) wrapped;
            }
            throw we;
        }
        finally
        {
            Context.exit();
        }
        return errors;
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

}
