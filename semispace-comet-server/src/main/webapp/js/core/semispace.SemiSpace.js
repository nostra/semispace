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
 * @fileOverview    SemiSpace JavaScript Client
 *
 * @version         1.0.1
 *
 * @description
 * Some description...
 *
 * @constructor
 *
 * @param   com     {Object}    Comet connection to work on
 */
semispace.SemiSpace = function(com){

    // TODO: Add leasecancel on notify
    // TODO: Add check on all input metod parameters to see if they have values - Maby add defaults (time???)
    // TODO: Add check to see if there is a connection to the server
    // TODO: Add handling of callbacks

    var cometd = com;
    var incrementedChannel = 0;

    var responseHandler = function(message){
        var data;
        if(message && message.data && message.data.result){
            data =  message.data.result;
            alert('Response - Data is: ' + data);
        } else {
            // data = "No response from server";
            // TODO: Notify will not match on the above if - add handling of notify
        }

    };


    this.notify = function(template, listener, duration){

        var subscriptionReply = undefined;
        var subscriptionEvent = undefined;

        if(subscriptionReply){
            cometd.unsubscribe(subscriptionReply);
        }
        subscriptionReply = cometd.subscribe('/semispace/reply/notify/' + incrementedChannel + '/' + listener, responseHandler);


        if(subscriptionEvent){
            cometd.unsubscribe(subscriptionEvent);
        }
        subscriptionEvent = cometd.subscribe('/semispace/event/notify/' + incrementedChannel + '/' + listener, responseHandler);

        cometd.publish('/semispace/call/notify/' + incrementedChannel + '/' + listener, {duration: duration.toString(), json: template});

        incrementedChannel++;

        return this;
    };


    this.read = function(template, timeout){

        var subscription = undefined;

        if(subscription){
            cometd.unsubscribe(subscription);
        }
        subscription = cometd.subscribe('/semispace/reply/read/' + incrementedChannel, responseHandler);

        cometd.publish('/semispace/call/read/' + incrementedChannel, {duration: timeout.toString(), json: template});

        incrementedChannel++;

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
        subscription = cometd.subscribe('/semispace/reply/write/' + incrementedChannel, responseHandler);

        cometd.publish('/semispace/call/write/' + incrementedChannel, {timeToLiveMs: lifeTimeInMs.toString(), json:obj});

        incrementedChannel++;

        return this;
    };


    this.take = function(template, timeout){

        var subscription = undefined;

        if(subscription){
            cometd.unsubscribe(subscription);
        }
        subscription = cometd.subscribe('/semispace/reply/take/' + incrementedChannel, responseHandler);

        cometd.publish('/semispace/call/take/' + incrementedChannel, {duration: timeout.toString(), json: template});

        incrementedChannel++;

        return this;
    };


    this.takeIfExists = function(template){
        this.take(template, 0);
        return this;
    };

};
