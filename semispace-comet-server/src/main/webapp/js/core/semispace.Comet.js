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
 * TODO: Add reload support: http://cometd.org/documentation/cometd/ext/reload
 *
 We have:
 2010-04-02 03:31:05.716:INFO:/semispace-comet-server:newChannel: /semispace
 2010-04-02 03:31:05.716:INFO:/semispace-comet-server:newChannel: /semispace/call
 2010-04-02 03:31:05.716:INFO:/semispace-comet-server:newChannel: /semispace/call/read
 2010-04-02 03:31:05.716:INFO:/semispace-comet-server:newChannel: /semispace/call/read/*

 2010-04-02 03:31:05.717:INFO:/semispace-comet-server:newChannel: /semispace/call/take
 2010-04-02 03:31:05.717:INFO:/semispace-comet-server:newChannel: /semispace/call/take/*

 2010-04-02 03:31:05.717:INFO:/semispace-comet-server:newChannel: /semispace/call/write
 2010-04-02 03:31:05.717:INFO:/semispace-comet-server:newChannel: /semispace/call/write/*

 2010-04-02 03:31:05.718:INFO:/semispace-comet-server:newChannel: /semispace/call/notify
 2010-04-02 03:31:05.718:INFO:/semispace-comet-server:newChannel: /semispace/call/notify/**

 *
 **/

semispace.Comet = function(connector, server){

    var cometd = connector;
    var connected = false;
    var metaListener = undefined;
    var _subscription;

    var init = function(){

        // Set configuration
        cometd.configure({
            url: server,
            logLevel: 'debug'
        });

        cometd.addListener('/meta/handshake', function(message){
            metaListener(1, message);
        });

        cometd.addListener('/meta/connect', function(message){

            var wasConnected = connected;
            connected = message.successful;
            if (!wasConnected && connected){
                metaListener(2, message);       // Connected - Server is up

                cometd.batch(function()
                {
                    if (_subscription)
                    {
                        cometd.unsubscribe(_subscription);
                    }
                    var param ="";
                    _subscription = cometd.subscribe('/semispace/reply/read/*', function(message)
                    {
                        var data;
                        if ( message && message.data && message.data.result) {
                            data =  message.data.result;
                            //param = JSON.parse(message.data.result);
                        } else {
                            data = "No response from server";
                        }
                        // Note - transforming back: JSON.stringify(param)
                        //dojo.byId('body').innerHTML += '<div>Server Says: <b>'+data+'</b><pre>' + param.org_semispace_comet_demo_FieldHolder.fieldB+ '</pre></div>';
                        alert('hello 1: ' + data);
                    });

                    // Waiting an hour, if need be
                    cometd.publish('/semispace/call/read/1', {duration: '600000', searchMap: {semispaceObjectTypeKey: 'org.semispace.comet.demo.FieldHolder'}});

                });

            }else if (wasConnected && !connected){
                metaListener(3, message);       // Not connected - Server is down
            }else{
                metaListener(4, message);       // Communicationg with server
            }

        });

        cometd.addListener('/meta/disconnect', function(message){
            metaListener(5, message);
        });

        cometd.addListener('/meta/subscribe', function(message){
            metaListener(6, message);
        });

        cometd.addListener('/meta/unsubscribe', function(message){
            metaListener(7, message);
        });

        cometd.addListener('/meta/publish', function(message){
            metaListener(8, message);
        });

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
    };

    this.doWrite = function(){
        // cometd.publish('/semispace/call/write', {duration: '600000', searchMap: {semispaceObjectTypeKey: 'org.semispace.comet.demo.FieldHolder'}});


        var m = JSON.stringify({"org_semispace_comet_demo_FieldHolder":{"fieldA":"InsertServlet","fieldB":"js side"}});
        cometd.publish('/semispace/call/write/1', {timeToLiveMs: '600000', searchMap: {semispaceObjectTypeKey: 'org.semispace.comet.demo.FieldHolder'}, semispaceObjectTypeKey: 'org.semispace.comet.demo.FieldHolder', json:m});
        //alert('hello');
    };

    this.doTake = function(){
        cometd.publish('/semispace/call/read/1', {duration: '600000', searchMap: {semispaceObjectTypeKey: 'org.semispace.comet.demo.FieldHolder'}});
        //alert('ohh');
    };

};

