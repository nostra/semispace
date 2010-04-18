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

    // TODO: Plan how to handle semispaceObjectTypeKey in a nice way for json objects
    // TODO: Add leasecancel on notify
    // TODO: Add check on all input metod parameters to see if they have values - Maby add defaults (time???)
    // TODO: Add check to see if there is a connection to the server
    // TODO: Add handling of callbacks

    var version = '1.0.1';
    var cometd = com;
    var channel = 0;

    var responseHandler = function(message){
        var data;
        if(message && message.data && message.data.result){
            data =  message.data.result;
            alert('Response - Data is: ' + data);
        } else {
            // data = "No response from server";
        }
    };


    this.notify = function(template, listener, duration){

        var subscriptionReply = undefined;
        var subscriptionEvent = undefined;

        if(subscriptionReply){
            cometd.unsubscribe(subscriptionReply);
        }
        subscriptionReply = cometd.subscribe('/semispace/reply/notify/' + channel + '/' + listener, responseHandler);


        if(subscriptionEvent){
            cometd.unsubscribe(subscriptionEvent);
        }
        subscriptionEvent = cometd.subscribe('/semispace/event/notify/' + channel + '/' + listener, responseHandler);


        cometd.publish('/semispace/call/notify/' + channel + '/' + listener, {duration: duration.toString(), searchMap: {semispaceObjectTypeKey: 'org.semispace.comet.demo.FieldHolder'}});

        channel++;

        return this;
    };


    this.read = function(template, timeout){

        var subscription = undefined;

        if(subscription){
            cometd.unsubscribe(subscription);
        }
        subscription = cometd.subscribe('/semispace/reply/read/' + channel, responseHandler);

        cometd.publish('/semispace/call/read/' + channel, {duration: timeout.toString(), searchMap: {semispaceObjectTypeKey: 'org.semispace.comet.demo.FieldHolder'}});

        channel++;

        return this;
    };


    this.readIfExists = function(template){
        this.read(template, 0);
        return this;
    };


    this.write = function(obj, lifeTimeInMs){

        var subscription = undefined;

        if(subscription){
            cometd.unsubscribe(subscription);
        }
        subscription = cometd.subscribe('/semispace/reply/write/' + channel, responseHandler);

        cometd.publish('/semispace/call/write/' + channel, {timeToLiveMs: lifeTimeInMs.toString(), searchMap: {semispaceObjectTypeKey: 'org.semispace.comet.demo.FieldHolder'}, semispaceObjectTypeKey: 'org.semispace.comet.demo.FieldHolder', json:obj});

        channel++;

        return this;
    };


    this.take = function(template, timeout){

        var subscription = undefined;

        if(subscription){
            cometd.unsubscribe(subscription);
        }
        subscription = cometd.subscribe('/semispace/reply/take/' + channel, responseHandler);

        cometd.publish('/semispace/call/take/' + channel, {duration: timeout.toString(), searchMap: {semispaceObjectTypeKey: 'org.semispace.comet.demo.FieldHolder'}});

        channel++;

        return this;
    };


    this.takeIfExists = function(template){
        this.take(template, 0);
        return this;
    };

};
