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
    pageElements:{
        "page" : {
            "id" : "thispage",
            "elements" : [
                {
                    "id" : "edit1",
                    "locked" : false,
                    "user" : undefined
                },
                {
                    "id" : "edit2",
                    "locked" : false,
                    "user" : undefined
                },
                {
                    "id" : "edit3",
                    "locked" : false,
                    "user" : undefined
                }
            ]
        }
    },
*/
    pageElements:{
        "page" : {
            "id" : "thispage",
            "elements" : []
        }
    },

    pageElementsTemplat:{
        "page" : {
            "id" : "thispage"
        }
    },
    pageElementsTemplatAsString:undefined,


    init:function(){
        example.pageElementsTemplatAsString = JSON.stringify(example.pageElementsTemplat);
        example.getUser();
        example.setDefaultEventHandlersInDOM();
        example.connectToServer();
        example.addPageElementsListener();
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
    },


    setDefaultEventHandlersInDOM:function(){
        var elements = document.getElementsByClassName('el');
        var i = elements.length;
        while(i--){
            var el = elements[i];
            var link = el.getElementsByTagName('a');

            var id = 'edit' + i;
            el.id = id;

            link[0].title = id;

            link[0].onclick = function(){
                example.doEdit(this.title); // Dirty fix!! - id turns out to be zero!
            };

            var elObj = {
                    "id" : id,
                    "locked" : false,
                    "user" : undefined
            };
            example.pageElements.page.elements.push(elObj);

        }
    },


    addPageElementsListener:function(){
        example.Space.notify(example.pageElementsTemplatAsString, 'availability', example.timeoutNotify, example.getPageElementsFromSpace);
    },


    getPageElementsFromSpace:function(data){
        example.Space.readIfExists(example.pageElementsTemplatAsString, function(data){
            if(data){
                example.pageElements = JSON.parse(data);
                var i = example.pageElements.page.elements.length;
                while(i--){
                    example.updateLocking(i);
                }
            }
        });
    },

    updateLocking:function(index){
        var id = example.pageElements.page.elements[index].id;
        var locked = example.pageElements.page.elements[index].locked;
        var user = example.pageElements.page.elements[index].user;

        var el = document.getElementById(id);
        var link = el.getElementsByTagName('a');

        if(locked && (user === example.user)){

            // Edited by same user
            link[0].onclick = function(){
                example.doSave(id);
            };
            link[0].innerHTML = '[save]';
            el.className = 'editing';
            el.title = '';

        }else if(locked  && (user !== example.user)){

            // Edited by foreign user
            link[0].onclick = function(){
                example.isLocked(user);
            };
            link[0].innerHTML = '[locked]';
            el.className = 'locked';
            el.title = user + ' is editing this element';

        }else{

            // Not being edited
            link[0].onclick = function(){
                example.doEdit(id);
            };
            link[0].innerHTML = '[edit]';
            el.className = 'el';
            el.title = '';
        }
    },


    pushPageElementsToSpace:function(){
        example.Space.takeIfExists(example.pageElementsTemplatAsString, function(data){
            // Do nothing
        });

        var pageElementsAsString = JSON.stringify(example.pageElements);
        example.Space.write(pageElementsAsString, example.timeoutWrite, function(data){
            // Do nothing
        });
    },


    doEdit:function(id){
        var i = example.pageElements.page.elements.length;
        while(i--){
            if(example.pageElements.page.elements[i].id === id){
                example.pageElements.page.elements[i].locked = true;
                example.pageElements.page.elements[i].user = example.user;
            }
        }
        example.pushPageElementsToSpace();
    },


    doSave:function(id){
        var i = example.pageElements.page.elements.length;
        while(i--){
            if(example.pageElements.page.elements[i].id === id){
                example.pageElements.page.elements[i].locked = false;
                example.pageElements.page.elements[i].user = undefined;
            }
        }
        example.pushPageElementsToSpace();
    },


    isLocked:function(name){
        alert('This element is loced by ' + name + '.\nYou can not edit this element.');
    }

};