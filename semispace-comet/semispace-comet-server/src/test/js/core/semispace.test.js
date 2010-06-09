
// Set up test cases
var SemiSpaceTests = TestCase('SemiSpaceTests');


/*
 * Construct a object we want to test
 */
//var semi = new Person();

// Where the server is
var server = location.protocol + '//' + location.host + '/semispace-comet-server/cometd';

// Get DOJO cometd library
dojo.require("dojox.cometd");
var com = dojox.cometd;

var semiComet = new semispace.Comet(com, server);



// Test version number

SemiSpaceTests.prototype.testVersion = function(){

    var version = '1.0.0';

    assertEquals(version, semispace.VERSION);
};



SemiSpaceTests.prototype.testConnection = function(){

    var jall = 1;

    var kkk;
    semiComet.addMetaListener(function(data, message){
        kkk = data;
    });
    
    var mm = semiComet.connect();

    assertEquals(1, kkk);
};