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
        var sysOut, Error;

        sysOut = Packages.java.lang.System.out;
        Error = Packages.de.acosix.maven.jshint.Error;

        return function runJSHint(sourceLines)
        {
            var data, eidx, error;

            JSHINT(sourceLines, JSON.parse(jshintConfig));

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