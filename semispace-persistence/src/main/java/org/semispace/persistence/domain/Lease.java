/*
 * ============================================================================
 *
 *  File:     Lease.java
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


import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;

/**
 * The lease contains a number of tags and content link.
 */
@Entity
@Table(name = "lease")
@Proxy(lazy=false)
public class Lease implements java.io.Serializable {

    // Fields
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    @GenericGenerator(name = "hilo", strategy = "org.hibernate.id.MultipleHiLoPerTableGenerator", parameters = {
            @Parameter(name = "table", value = "hilo"), @Parameter(name = "max_lo", value = "0") })
    @Column(name = "id")
    private int id;

    /**
     * When the element is to be considered to be timed out
     */
    @Column(name = "liveUntil", nullable = false)
    private long liveUntil;

    @Column(name = "holderId", nullable = false)
    private long holderId;
    
    /**
     * The doc type of the element; Typically the persisted root class.
     */
    @Column(name = "doctype", nullable = false )
    @Type(type = "text" )
    private String doctype;

    /**
     * The actual content of the element
     */
    @Column(name = "actual", nullable = false)
    @Type(type = "text" )
    private String actual;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "lease_id", insertable = true, updatable = true, nullable = false)
    private Set<Tag> tags = new HashSet<Tag>();

    // Property accessors

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * * .
     */

    public long getLiveUntil() {
        return this.liveUntil;
    }

    public void setLiveUntil(long leaseTime) {
        this.liveUntil = leaseTime;
    }

    /**
     * * The doc type of the element; Typically the persisted root class.
     */

    public String getDoctype() {
        return this.doctype;
    }

    public void setDoctype(String doctype) {
        this.doctype = doctype;
    }

    /**
     * * The actual content of the element
     */

    public String getActual() {
        return this.actual;
    }

    public void setActual(String actual) {
        this.actual = actual;
    }

    public Set<Tag> getTags() {
        return this.tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public long getHolderId() {
        return this.holderId;
    }

    public void setHolderId(long holderId) {
        this.holderId = holderId;
    }

}
