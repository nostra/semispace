/*
 * ============================================================================
 *
 *  File:     TakeFromSpace.java
 *----------------------------------------------------------------------------
 *
 * No copying allowed without explicit permission.
 *
 *  All rights reserved.
 *
 *  Description:  See javadoc below
 *
 *  Created:      27. jan.. 2008
 * ============================================================================ 
 */

package org.semispace.space.tutorial;

import org.semispace.SemiSpace;
import org.semispace.SemiSpaceInterface;

public class TakeFromSpace {
    public static void main( String[] args) {
        if ( args.length < 1 ) {
            System.out.println("Please supply 1 arguments, which");
            System.out.println("is the name for which to take (the ");
            System.out.println("first element of).");
        } else {
            
            try {
                Element searchFor = new Element();
                searchFor.setName(args[0]);
                // START SNIPPET: retrieveSpace
                SemiSpaceInterface space = SemiSpace.retrieveSpace();
                // END SNIPPET: retrieveSpace
                // does not really need a timeout, but supply it nevertheless
                // START SNIPPET: takeFromSpace
                Element read = space.take(searchFor, 60000);
                // END SNIPPET: takeFromSpace
                if ( read == null ) {
                    System.out.println("Could not find an element with name "+searchFor.getName());
                } else {                    
                    System.out.println("Element found: "+read.getName()+"="+read.getValue());
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
            
        }
    }

}
