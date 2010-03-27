/*
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
 */

semispace.Comet = function(connector, server){

    var cometd = connector;
    var connected = false;
    var messenger = undefined;


    var m = function(message){
        if(typeof messenger  !== undefined){
            messenger(message);
        }
    };


    var metaConnect = function(message){
        var wasConnected = connected;
        connected = message.successful === true;
        if(!wasConnected && connected){
            m('Connected');
        }else if (wasConnected && !connected){
            m('Not connected');
        }
    };

    var metaUnsuccessful = function(message){
        m('Connection was unsuccessful');
    };


    var init = function(){

        // Set configuration
        cometd.configure({
            url: server,
            logLevel: 'debug'
        });

        // Set meta channel listeners
        cometd.addListener('/meta/connect', metaConnect);
        cometd.addListener('/meta/unsuccessful', metaUnsuccessful);

        cometd.handshake();
    }();


    this.isConnected = function(){
        return connected;
    };


    this.setMessenger = function(fn){
        messenger = fn;
    };


};

