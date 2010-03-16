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

import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.ReaderWrapper;
import com.thoughtworks.xstream.io.WriterWrapper;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

import java.io.Reader;
import java.io.Writer;

/**
 * 
 */
public class DashifyJettisonDriver extends JettisonMappedXmlDriver {
    @Override
    public HierarchicalStreamWriter createWriter(Writer out) {
        return new WriterWrapper(super.createWriter(out)) {
            @Override
            public void startNode(String name) {
                startNode(name, null);
            }
            @Override
            public void startNode(String name, Class clazz) {
                wrapped.startNode(name.replace('.', '-'));
            }
        };
    }
    public HierarchicalStreamReader createReader(final Reader reader) {
        return new ReaderWrapper(super.createReader(reader)) {
            public String getNodeName() {
                return wrapped.getNodeName().replace('-', '.');
            }
        };
    }
}
