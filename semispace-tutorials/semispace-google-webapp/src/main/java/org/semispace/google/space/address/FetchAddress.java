/*
 * ============================================================================
 *
 *  File:     FetchAddress.java
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
 *  Created:      Oct 4, 2008
 * ============================================================================ 
 */

package org.semispace.google.space.address;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.semispace.SemiSpace;
import org.semispace.SemiSpaceInterface;
import org.semispace.google.transport.AddressQuery;
import org.semispace.google.transport.GoogleAddress;
import org.semispace.google.webapp.beans.GoogleKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FetchAddress implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(FetchAddress.class);
    private static final long MAX_SEMAPHORE_WAIT_MS = 10000;
    private SemiSpaceInterface space;

    public FetchAddress(SemiSpaceInterface space) {
        this.space = space;
    }

    public void run() {
        AddressQuery query = (AddressQuery) space.takeIfExists(new AddressQuery());
        if ( query == null ) {
            log.warn("Could not get address query even when space flagged availability. Just returning.");
            return;
        }
        log.info("Read query OK in thread.");
        GoogleAddress ga = new GoogleAddress();
        ga.setAddress( query.getAddress());
        if ( space.readIfExists(ga) != null ) {
            log.info("I alreay have a cached instance of the query for address, and will not query again: "+ga.getAddress());
            return;
        }
        
        AddressLookupSemaphore semaphore = (AddressLookupSemaphore) space.take(new AddressLookupSemaphore(), MAX_SEMAPHORE_WAIT_MS);
        if ( semaphore == null ) {
            log.warn("Service seems to be saturated. Did not get semaphore.");
            return;
        }
        try {
            GoogleKey key = (GoogleKey) space.readIfExists(new GoogleKey());
            if ( key == null ) {
                log.warn("Google key is null. You need to supply a valid key.");
                ga = new GoogleAddress();
                ga.setAccuracy("-2");
                ga.setAddress("-- missing google key on server --");
                // Caching missing key an hour:
                space.write(ga, 1000 * 3600 );
            } else {
                ga = resolveAddress(query.getAddress(), key);
                // Caching lookup a day. This implies that results with an 
                // incorrect key is cached a day as well.
                space.write(ga, SemiSpace.ONE_DAY );
                log.debug("Resolving "+ga.getAddress()+" resulted in status code "+ga.getStatusCode()+" and accuracy "+ga.getAccuracy());
            }
        } finally {
            // Putting semaphore back
            space.write(semaphore, GoogleAddressFetcher.TEN_YEARS );
        }
    }

    public GoogleAddress resolveAddress(String address, GoogleKey key) {
        GoogleAddress result = new GoogleAddress();
        String url = encodeAddressAsHttpParameter(address, key);
        log.debug("Query url: "+url);
        HttpMethod method = new GetMethod(url);
        try {
            result.setAddress(address);
            HttpClient client = new HttpClient();
            method.setFollowRedirects(false);
            method.setDoAuthentication(false);
            client.executeMethod(method);
            byte[] buffer = method.getResponseBody();
            fillResponseInAddress( result, new String(buffer));
        } catch (IOException e) {
            result.setStatusCode("-1");
            log.error("Got exception", e);
        } finally {
            method.releaseConnection();
        }
        return result;
    }

    private void fillResponseInAddress(GoogleAddress result, String csv) {
        log.debug("CSV from google: "+csv);
        String values[] = csv.split(",");
        result.setStatusCode( values[0] );
        result.setAccuracy( values[1] );
        result.setLatitude(values[2] );
        result.setLongitude(values[3] );
    }

    private String encodeAddressAsHttpParameter(String address, GoogleKey key) {
        StringBuilder sb = new StringBuilder();
        sb.append("http://maps.google.com/maps/geo?q=");
        
        try {
            sb.append(URLEncoder.encode(address, "UTF8"));
        } catch (UnsupportedEncodingException e) {
            log.error("Got exception", e);
        }
        sb.append("&gl=no&output=csv&");
        sb.append("key="+key.getKey());
        return sb.toString();
    }
}
