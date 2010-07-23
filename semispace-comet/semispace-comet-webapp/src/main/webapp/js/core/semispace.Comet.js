/**
 * Copyright 2010 Trygve Lie
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
 *
 *
 * @constructor
 *
 * @description
 * Connector object for connecting with the tuple space.
 *
 * @example
 * dojo.require("dojox.cometd");
 * var connection = new semispace.Comet(dojox.cometd,serverUrl);
 * connection.connect();
 *
 * @param   cometdImpl     {Object}    Which Cometd implementation to use
 * @param   server         {String}    URL to the server
 */

semispace.Comet = function(server) {

    var cometd = undefined;
    var connected = false;
    var metaListener = undefined;


    var init = function() {

        var handshake = undefined;
        var connect = undefined;
        var disconnect = undefined;
        var subscribe = undefined;
        var unsubscribe = undefined;
        var publish = undefined;
        var unsuccessful = undefined;

        
        // Detect framework in use
        if (typeof dojo !== 'undefined') {
            dojo.require("dojox.cometd");
            cometd = dojox.cometd;
        } else if (typeof jQuery !== 'undefined') {
            cometd = jQuery.cometd;
        } else {
            return;
        }


        // Configuration
        cometd.configure({
            url: server,
            logLevel: 'error'
        });


        // Handshake messages
        if(handshake){
            cometd.removeListener(handshake);
        }
        handshake = cometd.addListener('/meta/handshake', function(message){
            metaListener(1, message);
        });


        // Conecction messages
        if(connect){
            cometd.removeListener(connect);
        }
        cometd.addListener('/meta/connect', function(message){
            var wasConnected = connected;
            connected = message.successful;
            if (!wasConnected && connected){
                metaListener(2, message);       // Connected - Server is up
            }else if (wasConnected && !connected){
                metaListener(3, message);       // Not connected - Server is down
            }else{
                metaListener(4, message);       // Communicationg with server
            }
        });


        // Disconnect messages
        if(disconnect){
            cometd.removeListener(disconnect);
        }
        cometd.addListener('/meta/disconnect', function(message){
            metaListener(5, message);
        });


        // Subscription messages
        if(subscribe){
            cometd.removeListener(subscribe);
        }
        cometd.addListener('/meta/subscribe', function(message){
            metaListener(6, message);
        });


        // Unsubscribe messages
        if(unsubscribe){
            cometd.removeListener(unsubscribe);
        }
        cometd.addListener('/meta/unsubscribe', function(message){
            metaListener(7, message);
        });


        // Publish messages
        if(publish){
            cometd.removeListener(publish);
        }
        cometd.addListener('/meta/publish', function(message){
            metaListener(8, message);
        });


        //Unsuccessful communication messages
        if(unsuccessful){
            cometd.removeListener(unsuccessful);
        }
        cometd.addListener('/meta/unsuccessful', function(message){
            metaListener(9, message);
        });

    }();


    /**
     * @description
     * Ads a callback functions for execution on every meta channel message.<br/>
     * The function added can be used to listen in on the communication between the client and the server. The function
     * added must take two function variables where the first variable is a status code (int) and the second is the
     * message (String) from the server.<br/>
     * <br/>
     * The status codes are:<br/>
     * 1 - The client have done a handshake with the server<br/>
     * 2 - The client have connected to the server - Server is up<br/>
     * 3 - The client lost connection with the server - Server is down<br/>
     * 4 - The client is communicating with the server - "Keep alive"<br/>
     * 5 - The client disconnected from the server<br/>
     * 6 - The client did subscribe to a channel<br/>
     * 7 - The client did unsubscribe from a channel<br/>
     * 8 - The client did a publish<br/>
     * 9 - The client can not communicate with the server<br/>
     *
     * @example
     * var connection = new semispace.Comet(dojox.cometd,serverUrl);
     * connection.addMetaListener(function logger(status, message){
     *    var msg = 'Status is: ' + status;
     *    switch(status){
     *        case 1:
     *            msg = msg + ' - Handshake is done';
     *            break;
     *        case 2:
     *            msg = msg + ' - Got connection to server';
     *            break;
     *        case 3:
     *            msg = msg + ' - No connection to server';
     *            break;
     *    }
     *    console.log(msg);
     * });
     *
     * @param   {function}  callback        A callback function.
     */
    this.addMetaListener = function(callback){
        metaListener = callback;
    };


    /**
     * @description
     * Connects the client to the server
     *
     * @returns {Object}    connection      The cometd connection.
     */
    this.connect = function(){
        cometd.handshake();
        return cometd;
    };


    /**
     * @description
     * Disconnects the client from the server
     */
    this.disconnect = function(){
        cometd.disconnect();
        connected = false;
    };


    /**
     * @description
     * Get the cometd connection
     *
     * @returns {Object}    connection        The cometd connection object.
     */
    this.getConnection = function(){
        return cometd;
    };

};