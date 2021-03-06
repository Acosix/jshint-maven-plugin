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
import java.io.InputStreamReader;
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

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public abstract class AbstractJSHinter implements JSHinter
{

    protected final Log log;

    protected Map<File, String> effectiveJSHintConfig = new HashMap<>();

    public AbstractJSHinter(final Log log)
    {
        if (log == null)
        {
            throw new IllegalArgumentException("log not provided");
        }

        this.log = log;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public synchronized List<Error> executeJSHint(final File baseDirectory, final String path, final String defaultJSHintConfigContent,
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

        if (this.log.isDebugEnabled())
        {
            this.log.debug("Using effective JSHint config: " + effectiveJSHintConfigContent);
        }

        final List<Error> errors = this.executeJSHintImpl(baseDirectory, path, effectiveJSHintConfigContent);

        this.reportErrors(errors);

        return errors;
    }

    protected abstract List<Error> executeJSHintImpl(File baseDirectory, String path, String defaultJSHintConfigContent);

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
                    if (this.log.isDebugEnabled())
                    {
                        this.log.debug(MessageFormat.format("Loaded custom JSHint config from {0}: {1}", configFile, customJSHintConfig));
                    }
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
                    if (this.log.isDebugEnabled())
                    {
                        this.log.debug(MessageFormat.format("Loaded custom JSHint config from {0}: {1}", configFile, customJSHintConfig));
                    }
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

    protected void reportErrors(final List<Error> errors)
    {
        for (final Error error : errors)
        {
            this.log.error(MessageFormat.format("{0},{1}: {2} ({3})", String.valueOf(error.getLine()), String.valueOf(error.getCharacter()),
                    error.getReason(), error.getCode()));
        }
    }
}
