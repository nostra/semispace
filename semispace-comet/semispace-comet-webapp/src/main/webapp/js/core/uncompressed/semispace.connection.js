// Copyright 2010 Trygve Lie
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


semispace.connection = {

    connected : false,
    metaListener : undefined,



    // @description
    // Initialize the semispace client
    //
    // @param {Object}      config     A communication configuration for the server

    init : function(config) {

        var semispaceConfig = config || {url:location.protocol + '//' + location.host + '/semispace-comet-server/cometd',logLevel:'debug'};
        var handshake, connect, disconnect, subscribe, unsubscribe, publish, unsuccessful;

        semispace.cometdImpl = semispace.utils.detectFramework();

        // Configuration

        semispace.cometdImpl.configure(semispaceConfig);


        // Handshake messages

        if (handshake) {
            semispace.cometdImpl.removeListener(handshake);
        }
        handshake = semispace.cometdImpl.addListener('/meta/handshake', function(message) {
            semispace.connection.metaListener(1, message);
        });


        // Conecction messages

        if (connect) {
            semispace.cometdImpl.removeListener(connect);
        }
        semispace.cometdImpl.addListener('/meta/connect', function(message){

            var wasConnected = semispace.connection.connected;
            semispace.connection.connected = message.successful;

            if (!wasConnected && semispace.connection.connected) {
                semispace.connection.metaListener(2, message);       // Connected - Server is up

            }else if (wasConnected && !semispace.connection.connected) {
                semispace.connection.metaListener(3, message);       // Not connected - Server is down

            }else{
                semispace.connection.metaListener(4, message);       // Communicationg with server
            }
        });


        // Disconnect messages

        if (disconnect) {
            semispace.cometdImpl.removeListener(disconnect);
        }
        semispace.cometdImpl.addListener('/meta/disconnect', function(message) {
            semispace.connection.metaListener(5, message);
        });


        // Subscription messages

        if (subscribe) {
            semispace.cometdImpl.removeListener(subscribe);
        }
        semispace.cometdImpl.addListener('/meta/subscribe', function(message) {
            semispace.connection.metaListener(6, message);
        });


        // Unsubscribe messages

        if (unsubscribe) {
            semispace.cometdImpl.removeListener(unsubscribe);
        }
        semispace.cometdImpl.addListener('/meta/unsubscribe', function(message) {
            semispace.connection.metaListener(7, message);
        });


        // Publish messages

        if (publish) {
            semispace.cometdImpl.removeListener(publish);
        }
        semispace.cometdImpl.addListener('/meta/publish', function(message) {
            semispace.connection.metaListener(8, message);
        });


        //Unsuccessful communication messages

        if (unsuccessful) {
            semispace.cometdImpl.removeListener(unsuccessful);
        }
        semispace.cometdImpl.addListener('/meta/unsuccessful', function(message) {
            semispace.connection.metaListener(9, message);
        });

    },



    // @description
    // Connects to the space

    connect : function() {
        semispace.cometdImpl.handshake();
    },



    // @description
    // Disconnects from the space

    disconnect : function() {
        semispace.cometdImpl.disconnect();
        this.connected = false;
    }

};