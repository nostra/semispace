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
 * TODO: Add reload support: http://cometd.org/documentation/cometd/ext/reload
 **/

semispace.Comet = function(connector, server){

    // TODO: Introduce json config
    var cometd = connector;
    var connected = false;
    var metaListener = undefined;

    var defaultOnConnect = function(){
        // TODO: set some defaults when connection to server
    };


    var init = function(){

        var handshake = undefined;
        var connect = undefined;
        var disconnect = undefined;
        var subscribe = undefined;
        var unsubscribe = undefined;
        var publish = undefined;
        var unsuccessful = undefined;

        // Set configuration
        cometd.configure({
            url: server,
            logLevel: 'error'
        });


        if(handshake){
            cometd.removeListener(handshake);
        }
        handshake = cometd.addListener('/meta/handshake', function(message){
            metaListener(1, message);
        });


        if(connect){
            cometd.removeListener(connect);
        }
        cometd.addListener('/meta/connect', function(message){
            var wasConnected = connected;
            connected = message.successful;
            if (!wasConnected && connected){
                metaListener(2, message);       // Connected - Server is up
                defaultOnConnect();             // Load default on connect
            }else if (wasConnected && !connected){
                metaListener(3, message);       // Not connected - Server is down
            }else{
                metaListener(4, message);       // Communicationg with server
            }
        });


        if(disconnect){
            cometd.removeListener(disconnect);
        }
        cometd.addListener('/meta/disconnect', function(message){
            metaListener(5, message);
        });


        if(subscribe){
            cometd.removeListener(subscribe);
        }
        cometd.addListener('/meta/subscribe', function(message){
            metaListener(6, message);
        });


        if(unsubscribe){
            cometd.removeListener(unsubscribe);
        }
        cometd.addListener('/meta/unsubscribe', function(message){
            metaListener(7, message);
        });


        if(publish){
            cometd.removeListener(publish);
        }
        cometd.addListener('/meta/publish', function(message){
            metaListener(8, message);
        });


        if(unsuccessful){
            cometd.removeListener(unsuccessful);
        }
        cometd.addListener('/meta/unsuccessful', function(message){
            metaListener(9, message);
        });

    }();


    this.addMetaListener = function(fn){
        metaListener = fn;
    };


    this.connect = function(){
        cometd.handshake();
    };


    this.disconnect = function(){
        cometd.disconnect();
        connected = false;
    };


    this.getConnection = function(){
        return cometd;
    };

};