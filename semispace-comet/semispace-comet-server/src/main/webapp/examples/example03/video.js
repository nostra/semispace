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

var example = {

    user:undefined,

    comet:undefined,
    Connection:undefined,
    Space:undefined,

    timeoutNotify:1200000,
    timeoutWrite:1200000,

/*
    registry:{
        "live" : {
            "videos" : ['video1', 'video2', 'video3']
        }
    },
*/
    registry:{
        "live" : {
            "videos" : []
        }
    },

    registryTemplat:{
        "live" : { }
    },
    registryTemplatAsString:undefined,


    // Some temporary variables - to be removed
    tempCounter:0,
    tempElementTemplat:{
        "video" : {
            "id" : 'video1'
        }
    },
    tempElementTemplatAsString:undefined,


    init:function(){
        example.pageElementsTemplatAsString = JSON.stringify(example.registryTemplat);
        example.tempElementTemplatAsString = JSON.stringify(example.tempElementTemplat);
        example.getUser();
        example.connectToServer();
    },


    getUser:function(){
        example.user = window.prompt("Please enter a username");
    },


    // Esablish communication with the semispace server and creates a semispace object
    connectToServer:function(){
        dojo.require("dojox.cometd");
        example.comet = dojox.cometd;

        var host = location.protocol + '//' + location.host + '/semispace-comet-server/cometd';
        example.Connection = new semispace.Comet(example.comet,host);
        example.Connection.connect();

        example.Space = new semispace.SemiSpace(example.Connection.getConnection());


        //var tempElementTemplatAsString = JSON.stringify(example.tempElementTemplat);
        example.Space.notify(example.tempElementTemplatAsString, 'availability', 60000, example.getPlayingElementInSpace);

    },


    getPlayingElementInSpace:function(data){
        
        example.Space.readIfExists(example.tempElementTemplatAsString, function(data){
            if(data){
                var remoteElement = JSON.parse(data);
                var e = document.getElementById(remoteElement.video.id);
                e.innerHTML = remoteElement.video.time;
            }
        });

    },


    pushCurrentPlayingToSpace:function(id){

        var activeElement = {
            "video" : {
                "id" : id,
                "user" : example.user,
                "time" : example.tempCounter
            }
        };

        example.tempCounter++;

        var activeElementAsString = JSON.stringify(activeElement);
        example.Space.write(activeElementAsString, 500, function(data){
            // Do nothing
        });
    },


    selectVideo:function(id){
        // Put current playing object into space
        var el = document.getElementById('select');
        el.innerHTML = 'value: ' + id;
        example.pushCurrentPlayingToSpace(id);

    }


};