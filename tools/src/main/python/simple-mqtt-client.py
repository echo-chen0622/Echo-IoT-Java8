import paho.mqtt.client as mqtt

# The callback for when the client receives a CONNACK response from the server.
def on_connect(client, userdata, rc, *extra_params):
   print('Connected with result code '+str(rc))
   #print('***' + str(r))
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

client.username_pw_set("B1_TEST_TOKEN")
client.connect('127.0.0.1', 1883, 1)

# Blocking call that processes network traffic, dispatches callbacks and
# handles reconnecting.
# Other loop*() functions are available that give a threaded interface and a
# manual interface.
client.loop_forever()
