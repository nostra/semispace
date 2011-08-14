/*
 * Copyright 2010 Erlend Nossum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.semispace.comet.common;

import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.ReaderWrapper;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.WriterWrapper;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.io.json.JettisonStaxWriter;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.StaxReader;
import com.thoughtworks.xstream.io.xml.XmlFriendlyNameCoder;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLInputFactory;
import org.codehaus.jettison.mapped.MappedXMLOutputFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Exchanging dots in package names with underscore (_) - which again
 * is transformed by XStream into double underscores (__)
 *
 * Code adapted from com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver
 * @see JettisonMappedXmlDriver
 */
public class SemiSpaceJettisonDriver implements HierarchicalStreamDriver {
    private static final Logger log = LoggerFactory.getLogger(SemiSpaceJettisonDriver.class);
    private final MappedXMLOutputFactory mof;
    private final MappedXMLInputFactory mif;
    private final MappedNamespaceConvention convention;

    public SemiSpaceJettisonDriver() {
        final Map nstjsons = new HashMap();
        final Configuration config = new Configuration(nstjsons);
        mof = new MappedXMLOutputFactory(config);
        mif = new MappedXMLInputFactory(config);
        convention = new MappedNamespaceConvention(config);
    }

    @Override
    public HierarchicalStreamReader createReader(final Reader reader) {
        try {
            return new ReaderWrapper(
                new StaxReader(new QNameMap(), mif.createXMLStreamReader(reader),
                        new XmlFriendlyNameCoder("$","_"))) {
                    @Override
                    public String getNodeName() {
                        return super.getNodeName().replace('_', '.');
                    }
                };
        } catch (final XMLStreamException e) {
            throw new StreamException(e);
        }
    }

    @Override
    public HierarchicalStreamReader createReader(final InputStream input) {
        try {
            return new StaxReader(new QNameMap(), mif.createXMLStreamReader(input), new XmlFriendlyNameCoder("$","_"));
        } catch (final XMLStreamException e) {
            throw new StreamException(e);
        }
    }

    @Override
    public HierarchicalStreamReader createReader(URL in) {
        try {
            return createReader(in.openStream());
        } catch (IOException e) {
            log.error("Got exception", e);
        }
        return null;
    }

    @Override
    public HierarchicalStreamReader createReader(File in) {
        try {
            return createReader( new FileInputStream(in));
        } catch (FileNotFoundException e) {
            log.error("Got exception", e);
        }
        return null;
    }

    @Override
    public HierarchicalStreamWriter createWriter(final Writer writer) {
        try {
            return new WriterWrapper(new JettisonStaxWriter(new QNameMap(), mof.createXMLStreamWriter(writer),
                    true, true, new XmlFriendlyNameCoder("$","_"), convention)) {
                @Override
                public void startNode(String name, Class clazz) {
                    super.startNode(name.replace('.', '_'), clazz);
                }
                @Override
                public void startNode(String name) {
                    super.startNode(name.replace('.', '_'));
                }
            };
        } catch (final XMLStreamException e) {
            throw new StreamException(e);
        }
    }

    @Override
    public HierarchicalStreamWriter createWriter(final OutputStream output) {
        try {
            return new JettisonStaxWriter(new QNameMap(), mof.createXMLStreamWriter(output), 
                    true, true, new XmlFriendlyNameCoder("$","_"), convention);
        } catch (final XMLStreamException e) {
            throw new StreamException(e);
        }
    }
}
