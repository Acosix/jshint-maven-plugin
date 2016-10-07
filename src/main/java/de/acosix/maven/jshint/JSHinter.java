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
import java.util.List;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public interface JSHinter
{

    /**
     * Executes JSHint validation on a single script file
     *
     * @param baseDirectory
     *            the base directory that contains the script file
     * @param path
     *            the path relative to the base directory that denotes the script file
     * @param defaultJSHintConfig
     *            the default JSHint configuration in JSON format
     * @param ignoreJSHintConfigFileOnPaths
     *            {@code true} if {@code .jshintrc} files found on the path should be ignored, {@code false} otherwise
     * @return the errors found during validation
     */
    List<Error> executeJSHint(File baseDirectory, String path, String defaultJSHintConfig, boolean ignoreJSHintConfigFileOnPaths);
}
