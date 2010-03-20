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

package org.semispace.take;

/**
 * @see org.semispace.take.TakeUniquenessTest
 */
public class Item {
    private String writer;
    private String value;
    private Long timestamp;

    public Item() {
    }

    public Item(String writer, String value, Long timestamp) {
        this.writer = writer;
        this.value = value;
        this.timestamp = timestamp;
    }

    public String getWriter() {
        return writer;
    }

    public void setWriter(String writer) {
        this.writer = writer;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Item other = (Item) obj;
        if ((this.writer == null) ? (other.writer != null) : !this.writer.equals(other.writer)) {
            return false;
        }
        if ((this.value == null) ? (other.value != null) : !this.value.equals(other.value)) {
            return false;
        }
        if (this.timestamp != other.timestamp && (this.timestamp == null || !this.timestamp.equals(other.timestamp))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash += ( writer==null?13:writer.hashCode());
        hash += ( value == null?19:value.hashCode());
        hash += ( timestamp == null?23:timestamp.hashCode());
        return hash;
    }

}
