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

package org.semispace.comet.server;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.server.AbstractService;
import org.semispace.SemiLease;
import org.semispace.comet.common.CometConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Listen for notifications to cancel lease
 */
public class LeaseCancellationService extends AbstractService {
    private static final Logger log = LoggerFactory.getLogger(LeaseCancellationService.class);
    private Map<String,SemiLease> leases = new HashMap<String, SemiLease>();
    private static LeaseCancellationService leaseCancellationService = null;

    public LeaseCancellationService(BayeuxServer bayeux) {
        super(bayeux, "leasecancel");
        if ( leaseCancellationService != null ) {
            // TODO Fix later
            log.error("Already have cancellation service. Need to cancel all elements.");
        }
        leaseCancellationService = this;
        addService( CometConstants.NOTIFICATION_CALL_CANCEL_LEASE_CHANNEL +"/*", "semispaceCancelLease");
    }

    public void semispaceCancelLease(final ServerSession remote, final Message message) {
        log.trace("Lease cancel: Remote id "+remote.getId()+" Ch: "+message.getChannel()+" clientId: "+message.getClientId()+" id: "+message.getId()+" data: "+message.getData());
        Object holderId = ((Map)message.getData()).get("callId");
        SemiLease sl = leases.get( message.getClientId()+"_"+holderId );
        Boolean result = Boolean.FALSE;
        if ( sl != null ) {
            log.trace("Cancelling lease with holder id "+sl.getHolderId());
            if ( sl.cancel()) {
                log.trace("Cancelling of lease successful");
                result = Boolean.TRUE;
            } else {
                log.trace("Lease could not be cancelled.");
            }
        } else {
            log.warn("No lease with holder id "+holderId);
        }
        // Not putting this in separate thread as it is expected to perform reasonably quickly
        Map<String, String> output = new HashMap<String, String>();
        output.put("cancelledOk", result.toString());
        remote.deliver(remote, message.getChannel().replace("/call/", "/reply/"), output, message.getId());
        log.trace("========= delivered lease cancel reply");
    }

    public static void registerCancelableLease(String callId, SemiLease lease, String clientId) {
        leaseCancellationService.performLeaseRegistration( callId, lease, clientId );
    }

    private void performLeaseRegistration(String callId, SemiLease lease, String clientId) {
        String key = clientId+"_"+callId;
        log.trace("Registered lease cancellation element with key {}",key);
        leases.put( key, lease );
    }
}
