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


semispace.space = {

    incrementedChannel : 0,



    //  @description
    //  Register a listener for a particular template search.
    //
    //  @example
    //  var templateAsString = JSON.stringify({"Person":{"firstName":"John","lastName":"Doe"}});
    //  semispace.space.notify(templateAsString, 'availability', 6000, function(data){
    //           alert(data);
    //  });
    //
    //  @param   {Object}    template    Template to be matched.
    //  @param   {String}    listener    Listener to be notified when JSON object with a matching template is found.
    //  @param   {number}    duration    How long this particular listener is valid.
    //  @param   {function}  callback    A callback function to be executed when a match occur. The response, if any, of the notify operation will be passed on as a function variable to the callback function.
    //
    //  @returns {function}  lease       A lease function to cancel the notification.

    notify : function(template, listener, duration, callback) {

        var subscriptionReply = undefined;
        var subscriptionEvent = undefined;

        var leaseCancelChannel = this.incrementedChannel;  // Break closure by putting incrementedChannel on local variable.

        var leasecancel = function() {

            var subscriptionLease = undefined;
            if (subscriptionLease) {
                semispace.cometdImpl.unsubscribe(subscriptionLease);
            }
            subscriptionLease = semispace.cometdImpl.subscribe('/service/semispace/reply/leasecancel/' + this.incrementedChannel, function() {
                // Nothing to do...
            });

            semispace.cometdImpl.publish('/service/semispace/call/leasecancel/' + this.incrementedChannel, {callId: leaseCancelChannel});
        };


        if (subscriptionReply) {
            semispace.cometdImpl.unsubscribe(subscriptionReply);
        }
        subscriptionReply = semispace.cometdImpl.subscribe('/service/semispace/reply/notify/' + this.incrementedChannel + '/' + listener, function() {
            // Nothing to do...
        });


        if (subscriptionEvent) {
            semispace.cometdImpl.unsubscribe(subscriptionEvent);
        }
        subscriptionEvent = semispace.cometdImpl.subscribe('/service/semispace/event/notify/' + this.incrementedChannel + '/' + listener, function(message) {
            semispace.utils.callbackHandler(message, callback);
        });


        semispace.cometdImpl.publish('/service/semispace/call/notify/' + this.incrementedChannel + '/' + listener, {duration:duration.toString(), json:template});

        this.incrementedChannel++;

        return leasecancel;
    },



    //  @description
    //  Write JSON object into tuple space, with a life time given in ms.
    //
    //  @example
    //  var jsonAsString = JSON.stringify({"Person":{"firstName":"John","lastName":"Doe"}});
    //  semispace.space.write(jsonAsString, 6000, function(data){
    //       if(data){
    //           alert(data);
    //       }
    //  });
    //
    //  @param   {Object}    obj             Object to be written into the space.
    //  @param   {number}    lifeTimeInMs    How long the object should live in the space. Given in milliseconds.
    //  @param   {function}  callback        A callback function to be executed when the write operation is completed. The response, if any, of the write operation will be passed on as a function variable to the callback function.
    //
    //  @returns {Object}    this            For chaining purposes.

    write : function(obj, lifeTimeInMs, callback) {

        var subscription = undefined;

        if (subscription) {
            semispace.cometdImpl.unsubscribe(subscription);
        }
        subscription = semispace.cometdImpl.subscribe('/service/semispace/reply/write/' + this.incrementedChannel, function(message) {
            semispace.utils.callbackHandler(message, callback);
        });

        semispace.cometdImpl.publish('/service/semispace/call/write/' + this.incrementedChannel, {timeToLiveMs:lifeTimeInMs.toString(), json:obj});

        this.incrementedChannel++;

        return this;
    },



    //  @description
    //  Read an JSON object from the space, which has matching fields with the template.
    //
    //  @example
    //  var templateAsString = JSON.stringify({"Person":{"firstName":"John","lastName":"Doe"}});
    //  semispace.space.read(templateAsString, 6000, function(data){
    //       if(data){
    //           alert(data);
    //       }
    //  });
    //
    //  @param   {Object}    template    Object (JSON as string) of exactly the same type as what is wanted as return value, with zero or more none-null fields.
    //  @param   {number}    timeout     How long you are willing to wait for an answer / match. Given in milliseconds.
    //  @param   {function}  callback    A callback function to be executed when the read operation is completed. The response of the read operation will be passed on as a function variable to the callback function.
    //
    //  @returns {Object}    this        For chaining purposes.

    read : function(template, timeout, callback) {

        var subscription = undefined;

        if (subscription) {
            semispace.cometdImpl.unsubscribe(subscription);
        }
        subscription = semispace.cometdImpl.subscribe('/service/semispace/reply/read/' + this.incrementedChannel, function(message) {
            semispace.utils.callbackHandler(message, callback);
        });

        semispace.cometdImpl.publish('/service/semispace/call/read/' + this.incrementedChannel, {duration:timeout.toString(), json:template});

        this.incrementedChannel++;

        return this;
    },

    

    //  @description
    //  Same as read, with duration 0.
    //
    //  @see semispace.space.read

    readIfExists : function(template, callback) {
        this.read(template, 0, callback);
        return this;
    },



    //  @description
    //  Same as read, except that the JSON object is removed from the space.
    //
    //  @example
    //  var templateAsString = JSON.stringify({"Person":{"firstName":"John","lastName":"Doe"}});
    //  semispace.space.take(templateAsString, 6000, function(data){
    //       if(data){
    //           alert(data);
    //       }
    //  });
    //
    //  @param   {Object}    template    Object (JSON as string) of exactly the same type as what is wanted as return value, with zero or more none-null fields.
    //  @param   {number}    timeout     How long you are willing to wait for an answer / match. Given in milliseconds.
    //  @param   {function}  callback    A callback function to be executed when the take operation is completed. The response of the take operation will be passed on as a function variable to the callback function.
    //
    //  @returns {Object}    this        For chaining purposes.

    take : function(template, timeout, callback) {

        var subscription = undefined;

        if (subscription) {
            semispace.cometdImpl.unsubscribe(subscription);
        }
        subscription = semispace.cometdImpl.subscribe('/service/semispace/reply/take/' + this.incrementedChannel, function(message) {
            semispace.utils.callbackHandler(message, callback);
        });

        semispace.cometdImpl.publish('/service/semispace/call/take/' + this.incrementedChannel, {duration:timeout.toString(), json:template});

        this.incrementedChannel++;

        return this;
    },



    //  @description
    //  Same as take, with duration 0.
    //
    //  @see semispace.space.take

    takeIfExists : function(template, callback) {
        this.take(template, 0, callback);
        return this;
    }

};