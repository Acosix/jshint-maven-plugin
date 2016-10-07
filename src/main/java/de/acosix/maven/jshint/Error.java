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

import java.io.Serializable;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class Error implements Serializable
{

    private static final long serialVersionUID = -8665893495601916972L;

    private final String id;

    private final String code;

    private final String raw;

    private final String evidence;

    private final String reason;

    private final int line;

    private final int character;

    public Error(final String id, final String code, final String raw, final String evidence, final String reason, final int line,
            final int character)
    {
        this.id = id;
        this.code = code;
        this.raw = raw;
        this.evidence = evidence;
        this.reason = reason;
        this.line = line;
        this.character = character;
    }

    /**
     * @return the id
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * @return the code
     */
    public String getCode()
    {
        return this.code;
    }

    /**
     * @return the raw
     */
    public String getRaw()
    {
        return this.raw;
    }

    /**
     * @return the evidence
     */
    public String getEvidence()
    {
        return this.evidence;
    }

    /**
     * @return the reason
     */
    public String getReason()
    {
        return this.reason;
    }

    /**
     * @return the line
     */
    public int getLine()
    {
        return this.line;
    }

    /**
     * @return the character
     */
    public int getCharacter()
    {
        return this.character;
    }

}
