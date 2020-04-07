package com.example.mqttlib;

      import java.io.ByteArrayOutputStream;
      import java.io.IOException;
      import java.io.UnsupportedEncodingException;


public class MQTT {
   protected static byte VERSION = (byte) 0x01;
   protected static String PROTOCOL_NAME = "P2PMQTT";

	/*
	 * Message Types
	 */
   /** Client request to connect to a server */
   protected static final int CONNECT = 1;
   /** Connect Acknowledgement */
   protected static final int CONNACK = 2;
   /** Publish message */
   protected static final int PUBLISH = 3;
   /** Publish Acknowledgment */
   protected static final int PUBACK = 4;
   /** Publish Received */
   protected static final int PUBREC = 5;
   /** Publish Release */
   protected static final int PUBREL = 6;
   /** Publish Complete */
   protected static final int PUBCOMP = 7;
   /** Client Subscription request */
   protected static final int SUBSCRIBE = 8;
   /** Subscription Acknowledgment */
   protected static final int SUBACK = 9;
   /** Client Unsubscribe request */
   protected static final int UNSUBSCRIBE = 10;
   /** Unsubscribe Acknowledgment */
   protected static final int UNSUBACK = 11;
   /** PING Request */
   protected static final int PINGREQ = 12;
   /** PING Response */
   protected static final int PINGRESP = 13;
   /** Client is Disconnecting */
   protected static final int DISCONNECT = 14;

	/*
	 * Quality of Service levels
	 */
   /** Fire and Forget */
   protected static final int AT_MOST_ONCE = 0;
   /** Acknowledged deliver */
   protected static final int AT_LEAST_ONCE = 1;
   /** Assured Delivery */
   protected static final int EXACTLY_ONCE = 2;

   public static byte[] unsubscribe(int message_id, String topic) throws IOException {
      return encode(UNSUBSCRIBE, AT_LEAST_ONCE, topic.getBytes(), Integer.toString(message_id));
   }

   /**
    * Create the PINGREQ message, it doesn't use the fixed header parameters
    * other than message type, is has no payload and no variable header.
    *
    * @return The MQTT package.
    * @throws IOException
    */
   public static byte[] ping() throws IOException {
      return encode(PINGREQ, AT_MOST_ONCE, new byte[0]);
   }

   /**
    * Create a SUBSCRIBE message, it has a QoS of {@link #AT_LEAST_ONCE}.
    *
    * @param message_id
    *            The message id of the subscribe, handled by the client.
    * @param subscribe_topic
    *            The topic to which the client wants to subscribe.
    * @param subscribed_qos
    *            The wanted QoS for the subscription, can be
    *            {@link #AT_MOST_ONCE}, {@link #AT_LEAST_ONCE}, or
    *            {@link #EXACTLY_ONCE}.
    * @return The MQTT package.
    * @throws IOException
    */
   public static byte[] subscribe(int message_id, String subscribe_topic, int subscribed_qos) throws IOException {
      ByteArrayOutputStream payload = new ByteArrayOutputStream();

      payload.write((byte) ((subscribe_topic.length() >> 8) & 0xFF));
      payload.write((byte) (subscribe_topic.length() & 0xFF));
      payload.write(subscribe_topic.getBytes("UTF-8"));
      payload.write(subscribed_qos);

      return encode(SUBSCRIBE, AT_LEAST_ONCE, payload.toByteArray(), Integer.toString(message_id));
      // "...  Closing a ByteArrayOutputStream has no effect...."
   }

   /**
    * Create a PUBLISH MQTT message with QoS {@link #AT_MOST_ONCE} and message
    * ID 0.
    *
    * @param topic
    *            Which topic to subscribe to.
    * @param message
    *            The message to send.
    * @return The resulting MQTT package.
    * @throws IOException
    */
   public static byte[] publish(String topic, byte[] message) throws IOException {
      return encode(PUBLISH, AT_MOST_ONCE, message, Integer.toString(0), topic);
   }

   /**
    * Create a CONNECT MQTT message.
    *
    * @return The resulting MQTT package.
    * @throws IOException
    * @throws UnsupportedEncodingException
    */
   public static byte[] connect() throws UnsupportedEncodingException, IOException {
      String identifier = "android";
      ByteArrayOutputStream payload = new ByteArrayOutputStream();
      payload.write(0);
      payload.write(identifier.length());
      payload.write(identifier.getBytes("UTF-8"));
      return encode(CONNECT, AT_MOST_ONCE, payload.toByteArray(), "false", "false", "false", "false", "false");
   }

   /**
    * Low level compilation of an MQTT package.
    *
    * @param type
    *            Message type, can be {@link #CONNACK}, {@link #PUBLISH},
    *            {@link #PUBACK}, {@link #PUBREC}, {@link #PUBREL},
    *            {@link #PUBCOMP}, {@link #SUBSCRIBE}, {@link #SUBACK},
    *            {@link #UNSUBSCRIBE}, {@link #UNSUBACK}, {@link #PINGREQ},
    *            {@link #PINGRESP} or {@link #DISCONNECT}
    * @param retain
    *            Only used on publish messages. The server holds on to the last
    *            message for each topic.
    * @param qos
    *            Quality of Service, can be {@link #AT_MOST_ONCE},
    *            {@link #AT_LEAST_ONCE}, or {@link EXACTLY_ONCE}
    * @param dup
    *            Should be set to true when a message is being re-delivered.
    *            Only when QoS is {@link #AT_LEAST_ONCE} or
    *            {@link #EXACTLY_ONCE}
    * @param payload
    *            The message payload, varies depending on message type.
    * @param params
    *            The parameters for the Variable Header.
    * @return The MQTT message as a byte array.
    * @throws IOException
    */
   protected static byte[] encode(int type, int qos, byte[] payload, String... params)
         throws IOException {
      ByteArrayOutputStream mqttStream = new ByteArrayOutputStream();
      boolean retain = false;
      boolean dup = false;
      // Fixed Header properties
      mqttStream.write((byte) ((retain ? 1 : 0) | qos << 1 | (dup ? 1 : 0) << 3 | type << 4));

      ByteArrayOutputStream variableHeader = new ByteArrayOutputStream();

      switch (type) {
         case CONNECT:
            // Variable Header, read the params.
            boolean username = Boolean.parseBoolean(params[0]);
            boolean password = Boolean.parseBoolean(params[1]);
            boolean will = Boolean.parseBoolean(params[2]);
            boolean will_retain = Boolean.parseBoolean(params[3]);
            boolean cleansession = Boolean.parseBoolean(params[4]);

            variableHeader.write(0x00); // LSB
            variableHeader.write(PROTOCOL_NAME.getBytes("UTF-8").length); // 7
            variableHeader.write(PROTOCOL_NAME.getBytes("UTF-8")); // "P2PMQTT"
            variableHeader.write(VERSION); // 0x01
            // Connect flags
            variableHeader.write((cleansession ? 1 : 0) << 1 | (will ? 1 : 0) << 2 | (qos) << 3
                  | (will_retain ? 1 : 0) << 5 | (password ? 1 : 0) << 6 | (username ? 1 : 0) << 7);
            variableHeader.write(0x00); // Keep Alive MSB
            variableHeader.write(0x000A); // Keep Alive LSB (10 seconds)
            break;

         case PUBLISH:
            // Variable header, read the params.
            int message_id = Integer.parseInt(params[0]);
            String topic_name = params[1];
            variableHeader.write(0x00); // Topic MSB
            variableHeader.write(topic_name.getBytes("UTF-8").length); // Topic
            variableHeader.write(topic_name.getBytes("UTF-8")); // Topic
            // variableHeader.write((message_id >> 8) & 0xFF); // Message ID MSB
            // variableHeader.write(message_id & 0xFF); // Message ID LSB
            break;

         case SUBSCRIBE:
            // Variable header, read the params.
            message_id = Integer.parseInt(params[0]);
            variableHeader.write((message_id >> 8) & 0xFF); // Message ID MSB
            variableHeader.write(message_id & 0xFF); // Message ID LSB
            break;

         case PINGREQ:
            // PINGREQ Doesn't have a variable header.
            break;
      }  // switch type

      // Remaining length
      int length = payload.length + variableHeader.size();
      do {
         byte digit = (byte) (length % 128);
         length /= 128;
         if (length > 0)
            digit = (byte) (digit | 0x80);
         mqttStream.write(digit);
      } while (length > 0);

      // Variable header
      mqttStream.write(variableHeader.toByteArray());

      // Payload
      mqttStream.write(payload);

      return mqttStream.toByteArray();
   }

   public static MQTTMessage decode(byte[] message) {
      int i = 0;
      MQTTMessage mqtt = new MQTTMessage();
      mqtt.type = (message[i] >> 4) & 0x0F;
      mqtt.DUP = ((message[i] >> 3) & 0x01) == 0 ? false : true;
      mqtt.QoS = (message[i] >> 1) & 0x03;
      mqtt.retain = (message[i++] & 0x01) == 0 ? false : true;

      int multiplier = 1;
      int len = 0;
      byte digit = 0;
      // Used to keep track of where the payload starts
      int headerOffset = 1;
      do {
         headerOffset++; // Incremend the payload
         digit = message[i++];
         len += (digit & 127) * multiplier;
         multiplier *= 128;
      } while ((digit & 128) != 0);
      mqtt.remainingLength = len;


      switch (mqtt.type) {
         case CONNECT:
            int protocol_name_len = (message[i++] << 8 | message[i++]);
            mqtt.variableHeader.put("protocol_name", new String(message, i, protocol_name_len));
            mqtt.variableHeader.put("protocol_version", message[i++]);
            mqtt.variableHeader.put("has_username", ((message[i++] << 7) & 0x01) == 0 ? false : true);
            mqtt.variableHeader.put("has_password", ((message[i] << 6) & 0x01) == 0 ? false : true);
            mqtt.variableHeader.put("will_retain", ((message[i] << 5) & 0x01) == 0 ? false : true);
            mqtt.variableHeader.put("will_qos", ((message[i] << 3) & 0x03));
            mqtt.variableHeader.put("will", ((message[i] << 2) & 0x01) == 0 ? false : true);
            mqtt.variableHeader.put("clean_session", ((message[i] << 1) & 0x01) == 0 ? false : true);
            int keep_alive_len = (message[i++] << 8 | message[i++]);
            mqtt.variableHeader.put("keep_alive", new String(message, i, keep_alive_len));
            break;
         case PUBLISH:
            int topic_name_len = (message[i++] * 256 + message[i++]);
            String topic_name = new String(message, i, topic_name_len);
            mqtt.variableHeader.put("topic_name", topic_name);
            i += topic_name_len;
            // Only read the message id if the QoS is above AT_MOST_ONCE
            if (mqtt.QoS > AT_MOST_ONCE) {
               int message_id = (message[i++] << 8 & 0xFF00 | message[i++] & 0xFF);
               mqtt.variableHeader.put("message_id",
                     Integer.toString(message_id));
            }
            break;
         case SUBSCRIBE:
            mqtt.variableHeader.put("message_id", (message[i++] << 8 | message[i++]));
            break;
         case PINGREQ:
            break;
      }

      ByteArrayOutputStream payload = new ByteArrayOutputStream();
      // Don't include any of the headers in the payload
      for (int b = i; b < headerOffset + mqtt.remainingLength; b++)
         payload.write(message[b]);

      mqtt.payload = payload.toByteArray();

      return mqtt;
   }
}
