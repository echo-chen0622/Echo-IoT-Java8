import paho.mqtt.client as mqtt
import ssl, socket

# The callback for when the client receives a CONNACK response from the server.
def on_connect(client, userdata, rc, *extra_params):
   print('Connected with result code '+str(rc))
   # Subscribing in on_connect() means that if we lose the connection and
   # reconnect then subscriptions will be renewed.
   client.subscribe('v1/devices/me/attributes')
   client.subscribe('v1/devices/me/attributes/response/+')
   client.subscribe('v1/devices/me/rpc/request/+')


# The callback for when a PUBLISH message is received from the server.
def on_message(client, userdata, msg):
   print 'Topic: ' + msg.topic + '\nMessage: ' + str(msg.payload)
   if msg.topic.startswith( 'v1/devices/me/rpc/request/'):
       requestId = msg.topic[len('v1/devices/me/rpc/request/'):len(msg.topic)]
       print 'This is a RPC call. RequestID: ' + requestId + '. Going to reply now!'
       client.publish('v1/devices/me/rpc/response/' + requestId, "{\"value1\":\"A\", \"value2\":\"B\"}", 1)


client = mqtt.Client()
client.on_connect = on_connect
client.on_message = on_message
client.publish('v1/devices/me/attributes/request/1', "{\"clientKeys\":\"model\"}", 1)

client.tls_set(ca_certs="mqttserver.pub.pem", certfile=None, keyfile=None, cert_reqs=ssl.CERT_REQUIRED,
                       tls_version=ssl.PROTOCOL_TLSv1, ciphers=None);

client.username_pw_set("TEST_TOKEN")
client.tls_insecure_set(False)
client.connect(socket.gethostname(), 8883, 1)


# Blocking call that processes network traffic, dispatches callbacks and
# handles reconnecting.
# Other loop*() functions are available that give a threaded interface and a
# manual interface.
client.loop_forever()
