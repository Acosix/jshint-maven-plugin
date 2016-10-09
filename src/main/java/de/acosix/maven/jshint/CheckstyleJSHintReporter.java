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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.maven.plugin.MojoExecutionException;

import javanet.staxutils.IndentingXMLStreamWriter;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class CheckstyleJSHintReporter implements JSHintReporter
{

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void generateReport(final Map<String, List<Error>> errorsByFile, final OutputStream os)
    {
        final XMLOutputFactory outFactory = XMLOutputFactory.newInstance();

        try
        {
            XMLStreamWriter w = outFactory.createXMLStreamWriter(os, StandardCharsets.UTF_8.name());
            w = new IndentingXMLStreamWriter(w);

            w.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
            w.writeStartElement("checkstyle");
            w.writeAttribute("version", "7.1.2");

            for (final Entry<String, List<Error>> fileEntry : errorsByFile.entrySet())
            {
                final String filePath = fileEntry.getKey();
                final List<Error> errors = fileEntry.getValue();

                w.writeStartElement("file");
                w.writeAttribute("name", filePath);

                for (final Error error : errors)
                {
                    w.writeStartElement("error");

                    w.writeAttribute("line", String.valueOf(error.getLine()));
                    w.writeAttribute("column", String.valueOf(error.getCharacter()));

                    String severity;
                    final String code = error.getCode();
                    // JSHint should only produce EXXX and WXXX error codes
                    if (code.startsWith("E"))
                    {
                        severity = "error";
                    }
                    else
                    {
                        severity = "warning";
                    }
                    w.writeAttribute("severity", severity);
                    w.writeAttribute("message", error.getReason());

                    w.writeEndElement();
                }

                w.writeEndElement();
            }

            w.writeEndElement();
        }
        catch (final XMLStreamException xsex)
        {
            throw new RuntimeException(new MojoExecutionException("Error writing checkstyle report output", xsex));
        }
    }

}
