package Utils;

import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class MessageParser {
    public static final char STXChar = (char)0x2;
    public static final char ETXChar = (char)0x3;
    public static final char EOTChar = (char)0x4;
    public static final char ENQChar = (char)0x5;
    public static final char ACKChar = (char)0x6;
    public static final char NAKChar = (char)0x15;

    public MessageParser() {}

    public int getStringLRC(String str) {
        int lrc = 0;

        for (int i = 0; i < str.length(); i++) {
            lrc = lrc ^ (str.charAt(i) & 255);
        }

        return lrc;
    }

    private boolean checkLRC(String str, int lrc) {
        return getStringLRC(str) == lrc;
    }

    public JSONObject parseMessage(String str) throws MessageParserException {
        if (str.length() > 0) {
            if (str.charAt(0) == MessageParser.STXChar) {
                String[] strings = str.substring(1, str.length()).split(Character.toString(MessageParser.ETXChar));
            
                if (strings.length == 2) {
                    if (checkLRC(strings[1], ACKChar)) {
                        JSONParser parser = new JSONParser();

                        try {
                            return (JSONObject)parser.parse(strings[0]);
                        } catch (ParseException e) {
                            throw new MessageParserException();
                        }
                    }
                }
            }
        }

        throw new MessageParserException();
    }
}
