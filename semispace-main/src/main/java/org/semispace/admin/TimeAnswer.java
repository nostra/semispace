/*
 * ============================================================================
 *
 *  File:     TimeAnswer.java
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
 *  Created:      Feb 24, 2008
 * ============================================================================
 */

package org.semispace.admin;

/**
 * Internally used by admin. Used when space needs to query for time.
 *
 * @see TimeQuery
 */
public class TimeAnswer implements InternalQuery {
    public Long timeFromMaster;
    public int masterId;
}
