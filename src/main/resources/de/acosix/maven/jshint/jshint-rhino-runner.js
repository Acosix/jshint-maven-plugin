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

if (typeof this.runJSHint !== 'function')
{
    this.runJSHint = (function()
    {
        var Error, RuntimeException, MojoExecutionException;

        Error = Packages.de.acosix.maven.jshint.Error;
        RuntimeException = Packages.java.lang.RuntimeException;
        MojoExecutionException = Packages.org.apache.maven.plugin.MojoExecutionException;

        return function runJSHint(sourceLines)
        {
            var data, eidx, error, config;

            try
            {
                config = JSON.parse(jshintConfig);
            }
            catch (e)
            {
                throw new RuntimeException(new MojoExecutionException('Error parsing JSHint JSON config: ' + e.message));
            }

            JSHINT(sourceLines, config, config.globals || {});

            data = JSHINT.data();

            if (Array.isArray(data.errors))
            {
                for (eidx = 0; eidx < data.errors.length; eidx++)
                {
                    error = data.errors[eidx];
                    if (error !== null)
                    {
                        errors.add(new Error(error.id, error.code, error.raw, error.evidence, error.reason, error.line, error.character));
                    }
                }
            }
        };
    }());
}

runJSHint(sourceLines);
