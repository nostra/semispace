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
 **/


/**
 * Simple html log - Logs to a UL list.
 * 
 * @param   logElementId    {String}    The id of the div element the log should be submitted to
 * @param   message         {String}    The message to log
 */
var logger = function(logElementId, message){

    // Get debug container
    var log = document.getElementById(logElementId);

    // Remove items at the end of the list
    if(log.childNodes.length > 26){
        log.removeChild(log.childNodes[25]);
    }

    // Append new debug message
    var logItem = document.createElement('p');
    logItem.innerHTML = message;
    log.insertBefore(logItem, log.firstChild);
};