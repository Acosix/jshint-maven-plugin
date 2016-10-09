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

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public interface JSHintReporter
{

    /**
     * Writes a report about JSHint findings (warnings and errors) to the provided output.
     *
     * @param errorsByFile
     *            the JSHint errors groupded by the path of the file for which they were reported
     * @param os
     *            the output to which to write
     */
    void generateReport(Map<String, List<Error>> errorsByFile, OutputStream os);
}
