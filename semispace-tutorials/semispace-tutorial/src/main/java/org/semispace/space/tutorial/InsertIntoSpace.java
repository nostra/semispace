/*
 * ============================================================================
 *
 *  File:     InsertIntoSpace.java
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

public class InsertIntoSpace {
    public static void main( String[] args) {
        if ( args.length < 2 ) {
            System.out.println("Please supply 2 arguments, which");
            System.out.println("will signify name / value.");
        } else {
            
            try {
                // START SNIPPET: intoSpace 
                Element element = new Element();
                element.setName(args[0]);
                element.setValue(args[1]);
                SemiSpaceInterface space = SemiSpace.retrieveSpace();
                // Life time of 5 minutes.
                space.write( element, 1000*5*60);
                System.out.println("Element inserted successfully: "+element.getName()+"="+element.getValue());
                // END SNIPPET: intoSpace
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
            
        }
    }
}
