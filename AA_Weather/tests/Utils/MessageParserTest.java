package Utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.json.simple.JSONObject;
import org.junit.Test;

public class MessageParserTest {
    private final MessageParser parser = new MessageParser();

    @Test
    public void getLRCTest1() {
        assertEquals(parser.getStringLRC("{\"ciudades\": 5}"), 33);
    }

    @Test
    public void getLRCTest2() {
        assertEquals(parser.getStringLRC("{\"Alicante\": 26,\"Sydney\": 32,\"Londres\": 13,\"Madrid\": 17,\"Washington DC\": 5}"), 72);
    }

    @Test
    public void parseMessageTest1() {
        Exception e = assertThrows(MessageParserException.class, () -> {
            parser.parseMessage("");
        });
    }

    @Test
    public void parseMessageTest2() {
        Exception e = assertThrows(MessageParserException.class, () -> {
            parser.parseMessage("" + Character.toString(MessageParser.ETXChar));
        });
    }

    @Test
    public void parseMessageTest3() {
        StringBuilder sb = new StringBuilder();
        sb.append(MessageParser.STXChar);
        sb.append("blabla");

        Exception e = assertThrows(MessageParserException.class, () -> {
            parser.parseMessage(sb.toString());
        });
    }

    @Test
    public void parseMessageTest4() {
        StringBuilder sb = new StringBuilder();
        sb.append(MessageParser.STXChar);
        sb.append("blabla");
        sb.append(MessageParser.ETXChar);
        sb.append("blaaa");

        Exception e = assertThrows(MessageParserException.class, () -> {
            parser.parseMessage(sb.toString());
        });
    }

    @Test
    public void parseMessageTest5() {
        StringBuilder sb = new StringBuilder();
        sb.append(MessageParser.STXChar);
        sb.append("blabla");
        sb.append(MessageParser.ETXChar);
        sb.append("45");

        Exception e = assertThrows(MessageParserException.class, () -> {
            parser.parseMessage(sb.toString());
        });
    }

    @Test
    public void parseMessageTest6() {
        StringBuilder sb = new StringBuilder();
        sb.append(MessageParser.STXChar);
        sb.append("blabla");
        sb.append(MessageParser.ETXChar);
        sb.append("0");

        Exception e = assertThrows(MessageParserException.class, () -> {
            parser.parseMessage(sb.toString());
        });
    }

    @Test
    public void parseMessageTest7() throws MessageParserException {
        StringBuilder sb = new StringBuilder();
        sb.append(MessageParser.STXChar);
        sb.append("{\"ciudades\": 5}");
        sb.append(MessageParser.ETXChar);
        sb.append("33");

        JSONObject obj = new JSONObject();
        obj.put("ciudades", 5);

        assertEquals(obj, parser.parseMessage(sb.toString()));
    }
}
