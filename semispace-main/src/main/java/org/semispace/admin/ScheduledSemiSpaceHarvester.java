package org.semispace.admin;

import org.semispace.SemiSpace;
import org.semispace.SemiSpaceInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Timed instance which will periodically call semispace harvest method. It will also
 * periodically check that a master is present.
 */
public class ScheduledSemiSpaceHarvester implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ScheduledSemiSpaceHarvester.class);
    private SemiSpaceAdmin semiSpaceAdmin;
    private long lastCheck;
    /** At least a 2 minute wait between checking for presence of master */
    private static final long MIN_CHECK_WAIT_MS = 1000*60*2;

    public ScheduledSemiSpaceHarvester(SemiSpaceAdmin semiSpaceAdmin) {
        this.semiSpaceAdmin = semiSpaceAdmin;
        this.lastCheck = System.currentTimeMillis();
    }
    public void run() {
        SemiSpaceInterface space = semiSpaceAdmin.getSpace();
        if ( semiSpaceAdmin.isMaster() ) {
            if ( space instanceof SemiSpace) {
                //log.debug("Harvesting - start - ...");
                ((SemiSpace)space).harvest();
                //log.debug("Harvesting - end - ...");
            }
        } else {
            ensurePresenceOfAdmin();
        }

    }

    private void ensurePresenceOfAdmin() {
        if ( semiSpaceAdmin.isMaster() ) {
            // Rather unlikely that this method has been called if current instance is master
            return;
        }
        if ( lastCheck + MIN_CHECK_WAIT_MS > System.currentTimeMillis()) {
            // In order not to check too often
            return;
        }
        lastCheck = System.currentTimeMillis();
        IdentifyAdminQuery admin = semiSpaceAdmin.populateListOfAllSpaces(new ArrayList<IdentifyAdminQuery>());
        if ( admin == null ) {
            log.info("Assuming admin responsibility in current space, as admin instance was not found");
            // Always writing time answer, as if the admin was lost, there is a good chance there are more instances present.
            semiSpaceAdmin.assumeAdminResponsibility(true);
        }
    }
}
