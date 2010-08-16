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


semispace.utils = {


    // @description
    // Detects current framework in use.
    //
    // @returns {Object}    cometd        The CometD implementation object.

    detectFramework : function() {

        var cometdImpl = undefined;

        if (typeof dojo !== 'undefined') {
            dojo.require("dojox.cometd");
            cometdImpl = dojox.cometd;
        } else if (typeof jQuery !== 'undefined') {
            cometdImpl = jQuery.cometd;
        }
        
        return cometdImpl;
    },



    // @description
    // Helper to filter the data passed on to the callback function.
    //
    // @param {Object}      message     The data object / response from the server.
    // @param {function}    callback    Callback function to be executed when a event occur.

    callbackHandler : function(message, callback) {
        if (callback) {
            var data = undefined;
            if (message && message.data && message.data.result) {
                data =  message.data.result;
            }
            callback(data);
        }
    }

};