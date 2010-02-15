/*
 * ============================================================================
 *
 *  File:     DatabaseFactory.java
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

package org.semispace.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.PropertyOverrideConfigurer;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

public class DatabaseFactory {
    private static final String DATABASE_SERVICE_XML = "org/semispace/persistence/DatabaseService.xml";
    private static final Logger log = LoggerFactory.getLogger(DatabaseFactory.class);
    private static XmlBeanFactory factory = null;
    
    public synchronized static final DatabaseService retrieveDatabaseService() {
        if ( factory == null ) {
            if ( System.getProperty("spacecfg") == null ) {
                log.info("Setting config key spacecfg to mock as it was not defined. If something else is desired, give -Dspacecfg=something. (Usually \"live\"");
                System.setProperty("spacecfg", "mock" );
            }
            
            Resource resource = new ClassPathResource(DatabaseFactory.DATABASE_SERVICE_XML);
            log.debug("Shall get service from: "+DatabaseFactory.DATABASE_SERVICE_XML);
            
            factory = new XmlBeanFactory(resource);
            // The following will exchange $elements, 
            //PropertyPlaceholderConfigurer cfg = new PropertyPlaceholderConfigurer();
            //cfg.postProcessBeanFactory(factory);
            // The following will replace bean context elements:
            PropertyOverrideConfigurer cfg = new PropertyOverrideConfigurer();
            if ( System.getProperty("spaceoverride") != null ) {
                String cfgfile = System.getProperty("spaceoverride");
                log.info("Adding location for property place holder configuration: "+cfgfile);
                cfg.setLocation(new FileSystemResource(cfgfile ) );
            }
            cfg.postProcessBeanFactory(factory);
        }
        
        Object someService = factory.getBean("SemiSpaceDatabaseService", DatabaseService.class);
        return (DatabaseService) someService;
    }
    
}
