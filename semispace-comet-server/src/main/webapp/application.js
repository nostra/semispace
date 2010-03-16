dojo.require("dojox.cometd");

dojo.addOnLoad(function()
{
    var cometd = dojox.cometd;

    // Idempotent function called every time the connection
    // with the Bayeux server is (re-)established
    var _subscription;
    function _connectionSucceeded()
    {
        dojo.byId('body').innerHTML = '<div>Cometd Connection Succeeded</div>';

        cometd.batch(function()
        {
            if (_subscription)
            {
                cometd.unsubscribe(_subscription);
            }
            var param ="";
            _subscription = cometd.subscribe('/semispace/reply/read/1', function(message)
            {
                var data;
                if ( message && message.data && message.data.result) {
                    //var x = message.data.result;
                    //data = JSON.parse(x);
                    data =  message.data.result;
                    param = JSON.parse(message.data.result);
                } else {
                    data = "No response from server";
                }
                // Note - transforming back: JSON.stringify(param)
                dojo.byId('body').innerHTML += '<div>Server Says: <b>'+data+'</b><pre>' + param.org__semispace__comet__demo__FieldHolder.fieldB+ '</pre></div>';
            });
            //// Publish on a service channel since the message is for the server only
            //cometd.publish('/service/hello', { name: 'World' });
            // Waiting an hour, if need be
            cometd.publish('/semispace/call/read/1', {duration: '600000', searchMap: {semispaceObjectTypeKey: 'org.semispace.comet.demo.FieldHolder'}});
        });//
    }

    function _connectionBroken()
    {
        dojo.byId('body').innerHTML = 'Cometd Connection Broken';
    }

    // Function that manages the connection status with the Bayeux server
    var _connected = false;
    function _metaConnect(message)
    {
        var wasConnected = _connected;
        _connected = message.successful === true;
        if (!wasConnected && _connected)
        {
            _connectionSucceeded();
        }
        else if (wasConnected && !_connected)
        {
            _connectionBroken();
        }
    }

    // Disconnect when the page unloads
    dojo.addOnUnload(function()
    {
        cometd.disconnect();
    });

    var cometURL = location.protocol + "//" + location.host + config.contextPath + "/cometd";
    cometd.configure({
        url: cometURL,
        logLevel: 'debug'
    });

    cometd.addListener('/meta/connect', _metaConnect);

    cometd.handshake();
});
