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

// TODO: Document JavaScript interface helper!!

semispace.util.intface = {

    implement:function(object){

        if(arguments.length < 2) {
            throw{
                name: 'Error',
                message: 'Interface.implements(); called with ' + arguments.length  + 'arguments. Expected at least 2!'
            };
        }

        for(var i = 1, len = arguments.length; i < len; i++) {
            var intf = arguments[i];
            if(intf.constructor !== semispace.util.intface.Interface) {
                throw{
                    name: 'Error',
                    message: 'Interface.implements(); expects arguments two and above to be instances of Interface!'
                };
            }

            var k = intf.methods.length;
            while(k--){
                var method = intf.methods[k];
                if(!object[method] || typeof object[method] !== 'function') {
                    throw{
                        name: 'Error',
                        message: 'Interface.implements(); object does not implement the ' + intf.name + ' interface. ' + method + ' was not found!'
                    };
                }
            }

        }

    }
};


semispace.util.intface.Interface = function(name, methods){
    if(arguments.length != 2) {
        throw{
            name: 'Error',
            message: 'Interface constructor called with ' + arguments.length + ' arguments. Expected exactly 2!'
        };
    }

    this.name = name;
    this.methods = [];

    var i = methods.length;
    while(i--){
        if(typeof methods[i] !== 'string') {
            throw{
                name: 'Error',
                message: 'Interface constructor expects method names to be passed in as a string!'
            };
        }
        this.methods.push(methods[i]);
    }
};
