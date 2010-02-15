/*
 * ============================================================================
 *
 *  File:     ReadFromSpace.java
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

public class SearchInSpace {
    public static void main( String[] args) {
        if ( args.length < 1 ) {
            System.out.println("Please supply 1 arguments, which");
            System.out.println("is the name for which to search (for ");
            System.out.println("the first element of).");
        } else {
            
            try {
                // START SNIPPET: readSpace 
                Element searchFor = new Element();
                searchFor.setName(args[0]);
                SemiSpaceInterface space = SemiSpace.retrieveSpace();
                // reading with a timeout of 60 seconds
                Element read = space.read(searchFor, 60000);
                if ( read == null ) {
                    System.out.println("Could not find an element with name "+searchFor.getName());
                } else {                    
                    System.out.println("Element found: "+read.getName()+"="+read.getValue());
                }
                // END SNIPPET: readSpace    
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
            
        }
    }

}
