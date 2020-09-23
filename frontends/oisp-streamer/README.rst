OISP Streamer
=============

The streamer is a websocket based service that serves a a bridge between internal or external clients and Kafka channels. For example, a beam service can write to a specific channel, to be read by a client outside the Kubernetes cluster.

Protocol
--------

.. note:: The streamer is WIP, breaking changes might occur!

The streamer uses a very simple application level protocol on top of websockets. After a client is connected to the websocket, it sends a single message to subscribe to channels. In order to unsubscribe or change the channels, the connection has to be closed and remade.

An example initialization message:

.. code-block:: json

  {"token": <account token>,
   "service": "metrics",
   "components": [
     [<aid>, <cid>]
   ]
  }

The `service` key describes the channel prefix. If the given token is valid for ALL <aid>s, `OK` is returned, all given `metrics.<aid>.<cid>` messages are forwarded to the client.

If the formatting is wrong, `Invalid Input` is returned and the websocket is closed. If token is not valid for a given account `Invalid Token For Account <aid>` is returned and the websocket is closed.
