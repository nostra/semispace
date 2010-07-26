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
 * Operations possible to perform on the tuple space.
 *
 * @example
 * var connection = new semispace.Comet(cometImpl,serverUrl);
 * connection.connect();
 * var space = new semispace.SemiSpace(connection.getConnection());
 *
 * @param   connection     {Object}    Comet connection to work on
 */
semispace.SemiSpace = function(connection){

    // TODO: Handle callback better on notify and leasecancel 

    var cometd = connection;
    var incrementedChannel = 0;



    /**
     * @private
     *
     * @description
     * Helper to filter the data passed on to the callback function.
     *
     * @param {Object}      message     The data object / response from the server.
     * @param {function}    callback    Callback function to be executed when a event occur.
     */
    var callbackHandler = function(message, callback){
        if(callback){
            var data = undefined;
            if(message && message.data && message.data.result){
                data =  message.data.result;
            }
            callback(data);
        }
    };



    /**
     *
     * @description
     * Register a listener for a particular template search.
     *  
     * @example
     * var templateAsString = JSON.stringify({"Person":{"firstName":"John","lastName":"Doe"}});
     * space.notify(templateAsString, 'availability', 6000, function(data){
     *          alert(data);
     * });
     *
     * @param   {Object}    template    Template to be matched.
     * @param   {String}    listener    Listener to be notified when JSON object with a matching template is found.
     * @param   {number}    duration    How long this particular listener is valid.
     * @param   {function}  callback    A callback function to be executed when a match occur. The response, if any, of the notify operation will be passed on as a function variable to the callback function.
     *
     * @returns {function}  lease       A lease function to cancel the notification.
     */
    this.notify = function(template, listener, duration, callback){

        var subscriptionReply = undefined;
        var subscriptionEvent = undefined;

        var leaseCancelChannel = incrementedChannel;  // Break closure by putting incrementedChannel on local variable.

        var leasecancel = function(){

            var subscriptionLease = undefined;
            if(subscriptionLease){
                cometd.unsubscribe(subscriptionLease);
            }
            subscriptionLease = cometd.subscribe('/service/semispace/reply/leasecancel/' + incrementedChannel, function(){
                // Nothing to do...
            });

            cometd.publish('/service/semispace/call/leasecancel/' + incrementedChannel, {callId: leaseCancelChannel});
        };


        if(subscriptionReply){
            cometd.unsubscribe(subscriptionReply);
        }
        subscriptionReply = cometd.subscribe('/service/semispace/reply/notify/' + incrementedChannel + '/' + listener, function(message){
            // Nothing to do...
        });


        if(subscriptionEvent){
            cometd.unsubscribe(subscriptionEvent);
        }
        subscriptionEvent = cometd.subscribe('/service/semispace/event/notify/' + incrementedChannel + '/' + listener, function(message){
            callbackHandler(message, callback);
        });


        cometd.publish('/service/semispace/call/notify/' + incrementedChannel + '/' + listener, {duration:duration.toString(), json:template});

        incrementedChannel++;

        return leasecancel;
    };



    /**
     * @description
     * Write JSON object into tuple space, with a life time given in ms.
     *
     * @example
     * var jsonAsString = JSON.stringify({"Person":{"firstName":"John","lastName":"Doe"}});
     * space.write(jsonAsString, 6000, function(data){
     *      if(data){
     *          alert(data);
     *      }
     * });
     *
     * @param   {Object}    obj             Object to be written into the space.
     * @param   {number}    lifeTimeInMs    How long the object should live in the space. Given in milliseconds.
     * @param   {function}  callback        A callback function to be executed when the write operation is completed. The response, if any, of the write operation will be passed on as a function variable to the callback function.
     *
     * @returns {Object}    this            For chaining purposes.
     */
    this.write = function(obj, lifeTimeInMs, callback){

        var subscription = undefined;

        if(subscription){
            cometd.unsubscribe(subscription);
        }
        subscription = cometd.subscribe('/service/semispace/reply/write/' + incrementedChannel, function(message){
            callbackHandler(message, callback);
        });

        cometd.publish('/service/semispace/call/write/' + incrementedChannel, {timeToLiveMs:lifeTimeInMs.toString(), json:obj});

        incrementedChannel++;

        return this;
    };



    /**
     * @description
     * Read an JSON object from the space, which has matching fields with the template.
     *
     * @example
     * var templateAsString = JSON.stringify({"Person":{"firstName":"John","lastName":"Doe"}});
     * space.read(templateAsString, 6000, function(data){
     *      if(data){
     *          alert(data);
     *      }
     * });
     *
     * @param   {Object}    template    Object (JSON as string) of exactly the same type as what is wanted as return value, with zero or more none-null fields.
     * @param   {number}    timeout     How long you are willing to wait for an answer / match. Given in milliseconds.
     * @param   {function}  callback    A callback function to be executed when the read operation is completed. The response of the read operation will be passed on as a function variable to the callback function.
     *
     * @returns {Object}    this        For chaining purposes.
     */
    this.read = function(template, timeout, callback){

        var subscription = undefined;

        if(subscription){
            cometd.unsubscribe(subscription);
        }
        subscription = cometd.subscribe('/service/semispace/reply/read/' + incrementedChannel, function(message){
            callbackHandler(message, callback);
        });

        cometd.publish('/service/semispace/call/read/' + incrementedChannel, {duration:timeout.toString(), json:template});

        incrementedChannel++;

        return this;
    };



    /**
     * @description
     * Same as read, with duration 0.
     *
     * @see semispace.SemiSpace.read
     */
    this.readIfExists = function(template, callback){
        this.read(template, 0, callback);
        return this;
    };



    /**
     * @description
     * Same as read, except that the JSON object is removed from the space.
     *
     * @example
     * var templateAsString = JSON.stringify({"Person":{"firstName":"John","lastName":"Doe"}});
     * space.take(templateAsString, 6000, function(data){
     *      if(data){
     *          alert(data);
     *      }
     * });
     *
     * @param   {Object}    template    Object (JSON as string) of exactly the same type as what is wanted as return value, with zero or more none-null fields.
     * @param   {number}    timeout     How long you are willing to wait for an answer / match. Given in milliseconds.
     * @param   {function}  callback    A callback function to be executed when the take operation is completed. The response of the take operation will be passed on as a function variable to the callback function.
     *
     * @returns {Object}    this        For chaining purposes.
     */
    this.take = function(template, timeout, callback){

        var subscription = undefined;

        if(subscription){
            cometd.unsubscribe(subscription);
        }
        subscription = cometd.subscribe('/service/semispace/reply/take/' + incrementedChannel, function(message){
            callbackHandler(message, callback);
        });

        cometd.publish('/service/semispace/call/take/' + incrementedChannel, {duration:timeout.toString(), json:template});

        incrementedChannel++;

        return this;
    };



    /**
     * @description
     * Same as take, with duration 0.
     *
     * @see semispace.SemiSpace.take
     */
    this.takeIfExists = function(template, callback){
        this.take(template, 0, callback);
        return this;
    };

};
