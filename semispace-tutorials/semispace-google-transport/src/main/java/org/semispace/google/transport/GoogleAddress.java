/*
 * ============================================================================
 *
 *  File:     GoogleAddress.java
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
 *  Created:      Dec 31, 2008
 * ============================================================================ 
 */

package org.semispace.google.transport;

public class GoogleAddress {
    private String address;
    private String statusCode;
    private String accuracy;
    private String latitude;
    private String longitude;
    public String getAddress() {
        return this.address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public String getStatusCode() {
        return this.statusCode;
    }
    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }
    public String getAccuracy() {
        return this.accuracy;
    }
    public void setAccuracy(String accuracy) {
        this.accuracy = accuracy;
    }
    public String getLatitude() {
        return this.latitude;
    }
    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }
    public String getLongitude() {
        return this.longitude;
    }
    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("address="+address).append("\n");
        sb.append("statusCode="+statusCode).append("\n");
        sb.append("accuracy="+accuracy).append("\n");
        sb.append("latitude="+latitude).append("\n");
        sb.append("longitude="+longitude).append("\n");
        return sb.toString();
    }
}
