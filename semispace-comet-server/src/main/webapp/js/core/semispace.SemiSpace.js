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
semispace.SemiSpace = function(connection){

    // TODO: Test leasecancel
    // TODO: Handle callback better on notify and leasecancel 
    // TODO: Add check on all input metod parameters to see if they have values - Maby add defaults (time???)
    // TODO: Add check to see if there is a connection to the server

    var cometd = connection;
    var incrementedChannel = 0;

    var callbackHandler = function(message, callback){
        // TODO: Handle "undefined" - If no callback is added by the implementor
        var data = undefined;
        if(message && message.data && message.data.result){
            data =  message.data.result;
        }
        callback(data);
    };


    this.notify = function(template, listener, duration, callback){

        var subscriptionReply = undefined;
        var subscriptionEvent = undefined;

        var leasecancel = function(){
            var subscriptionLease = undefined;
            if(subscriptionLease){
                cometd.unsubscribe(subscriptionLease);
            }
            subscriptionLease = cometd.subscribe('/semispace/reply/leasecancel/' + incrementedChannel, function(){/* Do something */});
            cometd.publish('/semispace/call/leasecancel/' + incrementedChannel);
        };


        if(subscriptionReply){
            cometd.unsubscribe(subscriptionReply);
        }
        subscriptionReply = cometd.subscribe('/semispace/reply/notify/' + incrementedChannel + '/' + listener, function(message){
            callbackHandler(message, callback);
        });


        if(subscriptionEvent){
            cometd.unsubscribe(subscriptionEvent);
        }
        subscriptionEvent = cometd.subscribe('/semispace/event/notify/' + incrementedChannel + '/' + listener, function(message){
            callbackHandler(message, callback);
        });

        cometd.publish('/semispace/call/notify/' + incrementedChannel + '/' + listener, {duration:duration.toString(), json:template});

        incrementedChannel++;

        return leasecancel;
    };



    /**
     * @description
     * Write an object into the space.
     *
     * @example
     * var jsonAsString = JSON.stringify({"Person":{"firstName":"John","lastName":"Doe"}});
     * space.write(jsonAsString, 6000, function(data){
     *      alert(data);
     * });
     *
     * @param   {object}    obj             Object to be written into the space.
     * @param   {number}    lifeTimeInMs    How long the object should live in the space. Given in milliseconds.
     * @param   {function}  callback        A callback function to be executed when the write operation is completed. The response, if any, of the write operation will be passed on as a function variable to the callback function.
     *
     * @returns {object}    this            For chaining purposes. 
     */
    this.write = function(obj, lifeTimeInMs, callback){

        var subscription = undefined;

        if(subscription){
            cometd.unsubscribe(subscription);
        }
        subscription = cometd.subscribe('/semispace/reply/write/' + incrementedChannel, function(message){
            callbackHandler(message, callback);
        });

        cometd.publish('/semispace/call/write/' + incrementedChannel, {timeToLiveMs:lifeTimeInMs.toString(), json:obj});

        incrementedChannel++;

        return this;
    };



    /**
     * @description
     * Read an object from the space, which has matching fields with the template.
     *
     * @example
     * var templateAsString = JSON.stringify({"Person":{"firstName":"John","lastName":"Doe"}});
     * space.read(templateAsString, 6000, function(data){
     *      alert(data);
     * });
     *
     * @param   {object}    template    Object (JSON as string) of exactly the same type as what is wanted as return value, with zero or more none-null fields.
     * @param   {number}    timeout     How long you are willing to wait for an answer / match. Given in milliseconds.
     * @param   {function}  callback    A callback function to be executed when the read operation is completed. The response of the read operation will be passed on as a function variable to the callback function.
     *
     * @returns {object}    this        For chaining purposes.
     */
    this.read = function(template, timeout, callback){

        var subscription = undefined;

        if(subscription){
            cometd.unsubscribe(subscription);
        }
        subscription = cometd.subscribe('/semispace/reply/read/' + incrementedChannel, function(message){
            callbackHandler(message, callback);
        });

        cometd.publish('/semispace/call/read/' + incrementedChannel, {duration:timeout.toString(), json:template});

        incrementedChannel++;

        return this;
    };



    /**
     * @description
     * Same as read, with duration 0.
     *
     * @see read
     */
    this.readIfExists = function(template, callback){
        this.read(template, 0, callback);
        return this;
    };



    /**
     * @description
     * Same as read, except that the object is removed from the space.
     *
     * @example
     * var templateAsString = JSON.stringify({"Person":{"firstName":"John","lastName":"Doe"}});
     * space.take(templateAsString, 6000, function(data){
     *      alert(data);
     * });
     *
     * @param   {object}    template    Object (JSON as string) of exactly the same type as what is wanted as return value, with zero or more none-null fields.
     * @param   {number}    timeout     How long you are willing to wait for an answer / match. Given in milliseconds.
     * @param   {function}  callback    A callback function to be executed when the take operation is completed. The response of the take operation will be passed on as a function variable to the callback function.
     *
     * @returns {object}    this        For chaining purposes.
     */
    this.take = function(template, timeout, callback){

        var subscription = undefined;

        if(subscription){
            cometd.unsubscribe(subscription);
        }
        subscription = cometd.subscribe('/semispace/reply/take/' + incrementedChannel, function(message){
            callbackHandler(message, callback);
        });

        cometd.publish('/semispace/call/take/' + incrementedChannel, {duration:timeout.toString(), json:template});

        incrementedChannel++;

        return this;
    };



    /**
     * @description
     * Same as take, with duration 0.
     *
     * @see take
     */
    this.takeIfExists = function(template, callback){
        this.take(template, 0, callback);
        return this;
    };

};
