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

semispace.SemiSpace = function(com){
//    semispace.util.intface.implement(this,SemiSpaceInterface);
    // Dummy at this point - Testing interface

    // TODO: if time is number, call against space throws exception serverside. Add support for string convertion


    var cometd = com;
    var channel = 0;


    this.notify = function(template, listener, duration){

        var subscription = undefined;

        if(subscription){
            cometd.unsubscribe(subscription);
        }

        subscription = cometd.subscribe('/semispace/reply/notify/' + channel, function(message){
            var data;
            if(message && message.data && message.data.result){
                data =  message.data.result;
            } else {
                data = "No response from server";
            }
            alert('Notify performed - Data is: ' + data);
        });

        // NB: semispaceObjectTypeKey er det vi skriver og tar et objekt på
        cometd.publish('/semispace/call/notify/' + channel, {duration: duration, searchMap: {semispaceObjectTypeKey: 'org.semispace.comet.demo.FieldHolder'}});

        channel++;

    };


    this.read = function(template, timeout){

        var subscription = undefined;

        if(subscription){
            cometd.unsubscribe(subscription);
        }

        subscription = cometd.subscribe('/semispace/reply/read/' + channel, function(message){
            var data;
            if(message && message.data && message.data.result){
                data =  message.data.result;
            } else {
                data = "No response from server";
            }
            alert('Read performed - Data is: ' + data);
        });

        // NB: semispaceObjectTypeKey er det vi skriver og tar et objekt på
        cometd.publish('/semispace/call/read/' + channel, {duration: timeout, searchMap: {semispaceObjectTypeKey: 'org.semispace.comet.demo.FieldHolder'}});

        channel++;

    };


    this.readIfExists = function(){

    };


    this.write = function(obj, lifeTimeInMs){

        var subscription = undefined;

        if(subscription){
            cometd.unsubscribe(subscription);
        }

        subscription = cometd.subscribe('/semispace/reply/write/' + channel, function(message){
            var data;
            if(message && message.data && message.data.result){
                data =  message.data.result;
            } else {
                data = "No response from server";
            }

        });

        cometd.publish('/semispace/call/write/' + channel, {timeToLiveMs: lifeTimeInMs, searchMap: {semispaceObjectTypeKey: 'org.semispace.comet.demo.FieldHolder'}, semispaceObjectTypeKey: 'org.semispace.comet.demo.FieldHolder', json:obj});

        channel++;

    };


    this.take = function(template, timeout){

        var subscription = undefined;

        if(subscription){
            cometd.unsubscribe(subscription);
        }

        subscription = cometd.subscribe('/semispace/reply/take/' + channel, function(message){
            var data;
            if(message && message.data && message.data.result){
                data =  message.data.result;
            } else {
                data = "No response from server";
            }
            alert('Take performed - Data is: ' + data);
        });

        // NB: semispaceObjectTypeKey er det vi skriver og tar et objekt på
        cometd.publish('/semispace/call/take/' + channel, {duration: timeout, searchMap: {semispaceObjectTypeKey: 'org.semispace.comet.demo.FieldHolder'}});

        channel++;

    };

    this.takeIfExists = function(){

    };

};
