/*
 * ============================================================================
 *
 *  File:     GoogledController.java
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
 *  Created:      Feb 26, 2008
 * ============================================================================ 
 */

package org.semispace.google.webapp;

import java.util.HashSet;

import org.semispace.SemiSpace;
import org.semispace.SemiSpaceInterface;
import org.semispace.google.transport.AddressQuery;
import org.semispace.google.transport.GoogleAddress;
import org.semispace.google.webapp.beans.GoogleKey;
import org.semispace.google.webapp.beans.Token;
import org.semispace.google.webapp.beans.UserAuthBean;
import org.semispace.google.webapp.beans.UserList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class GoogledController {
    private static final Logger log = LoggerFactory.getLogger(GoogledController.class);
    private static final long DURATION_TEN_YEARS = SemiSpace.ONE_DAY*365*10;
    
    @Autowired
    private SemiSpaceInterface space;
    
    @RequestMapping("/index.html")
    public String entryPage() {
        return "entry";
    }

    @ModelAttribute("googleKey")
    public GoogleKey googleKey() {
        GoogleKey key = new GoogleKey();
        if ( space.readIfExists(key) == null ) {
            return key;
        }
        return null;
    }

    @ModelAttribute("user")
    public UserAuthBean user() {
        return new UserAuthBean();
    }

    @ModelAttribute("userList")
    public UserList userList() {
        UserList userList = (UserList) space.read(new UserList(), 750);
        if ( userList == null ) {
            log.info("Creating new userlist as it was not present.");
            userList = new UserList();
            userList.setUsers(new HashSet<String>());
            space.write(userList, DURATION_TEN_YEARS);
        }
        return userList;        
    }
    
    @ModelAttribute("searchForAddress")
    public AddressQuery address() {
        return new AddressQuery();
    }

    @RequestMapping("/submitAddress.html")
    public ModelAndView submitAddress(@ModelAttribute AddressQuery query) {
        space.write(query, 5000);
        GoogleAddress ga = new GoogleAddress();
        ga.setAddress( query.getAddress());
        ga = (GoogleAddress) space.read(ga, 5500);
        ModelAndView mv = new ModelAndView("entry");
        mv.addObject("addressResult", ga);
        return mv;
    }

    @RequestMapping("/submitUser.html")
    public RedirectView submitUser(@ModelAttribute UserAuthBean user) {
        UserAuthBean template = new UserAuthBean();
        template.setUsername(user.getUsername());
        // First remove the user, if the user exists
        space.takeIfExists(template );
        
        // Remove the token, if it exists
        Token token = new Token ();
        token.setUsername(user.getUsername());
        if ( space.takeIfExists(token ) == null) {
            log.debug("Did not remove token for user "+token.getUsername());
        } else {
            log.info("Removed token for user "+token.getUsername());
        }
        
        UserList userList = (UserList) space.take(new UserList(), 500);
        // Need to collate potential "slow" results at the off chance that some may exist
        UserList seldom = (UserList) space.takeIfExists(new UserList());
        while ( seldom != null) {
            log.warn("Notice: The space has obviously responded slowly at some time, and I therefore need to collate results");
            userList.getUsers().addAll(seldom.getUsers());
            seldom = (UserList) space.takeIfExists(new UserList());
        }
        userList.getUsers().remove(user.getUsername());
        
        if ( user.getPassword() != null && !"".equals(user.getPassword().trim()) ) {
            userList.getUsers().add(user.getUsername());
            space.write(user, GoogledController.DURATION_TEN_YEARS);
            log.debug("Added user "+user.getUsername());
        }
        space.write(userList, GoogledController.DURATION_TEN_YEARS);
        return new RedirectView("index.html");
    }

    @RequestMapping("/submitGoogleKey.html")
    public RedirectView submitGoogleKey(@ModelAttribute GoogleKey key) {
        GoogleKey template = new GoogleKey();
        space.takeIfExists(template );
        space.write(key, GoogledController.DURATION_TEN_YEARS);
        return new RedirectView("index.html");
    }

    @RequestMapping("/removeKey.html")
    public RedirectView removeGoogleKey() {
        GoogleKey template = new GoogleKey();
        space.takeIfExists(template );
        return new RedirectView("index.html");
    }
    
}
