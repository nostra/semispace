/*
 * ============================================================================
 *
 *  File:     Tag.java
 *----------------------------------------------------------------------------
 *
 * Copyright 2008 Erlend Nossum
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
 *
 *  Description:  See javadoc below
 *
 *  Created:      Apr 6, 2008
 * ============================================================================ 
 */

package org.semispace.persistence.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;


/**
 * Tag contains a single tag element for a lease.
 */
@Entity
@Table(name = "tag")
@Proxy(lazy=false)
public class Tag implements java.io.Serializable {

    // Fields
    @Id 
    @GeneratedValue(strategy=GenerationType.TABLE)
    @GenericGenerator(name = "hilo", strategy = "org.hibernate.id.MultipleHiLoPerTableGenerator", parameters = {
            @Parameter(name = "table", value = "hilo"), @Parameter(name = "max_lo", value = "0") })
    @Column(name = "id")
    private int id;

    /**
     * Tag name
     */
    @Column(name = "name", nullable = false )
    @Type(type = "text" )
    private String name;

    /**
     * Tag content
     */
    @Column(name = "content", nullable = false )
    @Type(type = "text" )
    private String content;

    // Property accessors

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * * Tag name
     */

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * * Tag content
     */

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}
