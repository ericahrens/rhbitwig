package com.rhcommons.json;

import java.util.Stack;

public class JsonParser {

    private final String input;
    private int pos = 0;

    private Token token = Token.NONE;
    private JsonObject current;
    private StringBuilder key;
    private StringBuilder value;
    private Stack<JsonObject> stack = new Stack<>();
    private Stack<String> keyStack = new Stack<>();

    public static void main(String[] args) {
        String test = """
                {"abc":"deff",  "nested":{ "other":"x", "nxcc": true }  "it":  true}
                """;

        String test2 = """
                {"deckId":"A","metadata":{"title":"Contradictions","artist":"Native Instruments","album":"Decoded Forms (Expansion)","cover":"124/2PK3GTD2OXK3KC0M1GHFAUJH22ZA","duration":"03:40","bpm":162,"key":"10A","bitrate":320000},"player":{"isPlaying":false,"isCueing":false,"isLooping":false,"loopSize":"4","loopActive":false,"slip":false,"isSlipping":false,"sync":false,"syncInPhase":false,"syncInRange":true,"tempoRange":"8%","tempo":0,"bpm":162,"keyAdjust":0,"resultingKey":"10A","keySync":false}}                       
                """;
        JsonParser parser = new JsonParser(test2);
        JsonObject json = parser.parse();

        System.out.println("DECKID = " + json.getValue("deckId"));
        JsonObject meta = json.getJsonObject("metadata");
        if(meta != null) {
            System.out.println("   KEY = "+ meta.getValue("key"));
        }

        //System.out.println(json);

    }

    private enum Token {
        NONE, OBJECT, START_KEY, EXPECT_VALUE, VALUE;
    }

    public JsonParser(String input) {
        this.input = input;
    }

    public JsonObject parse() {
        JsonObject root = new JsonObject();
        stack.push(root);
        current = root;
        while(pos < input.length()) {
            char c = input.charAt(pos);
            switch (token) {
                case NONE -> handleNone(c);
                case OBJECT ->  handleObject(c);
                case START_KEY -> handleInKey(c);
                case EXPECT_VALUE -> handleForValue(c);
                case VALUE -> readValue(c);
            }
            //System.out.println(" <%c> %s".formatted(c,token));
            pos++;
        }
        return root;
    }

    private void readValue(char c) {
        if(c==',') {
            current.set(key.toString().trim(),value.toString().trim());
            token = Token.OBJECT;
        } else if (c == '}') {
            current.set(key.toString().trim(), value.toString().trim());
            if(!keyStack.isEmpty()) {
                current.set(key.toString().trim(), value.toString().trim());
                String keyPrev = keyStack.pop();
                JsonObject parent = stack.pop();
                parent.set(keyPrev, current);
                current = parent;
            }
            token = Token.OBJECT;
        } else if(c=='{') {
            keyStack.push(key.toString().trim());
            stack.push(current);
            current = new JsonObject();
            token = Token.OBJECT;
        } else {
            value.append(c);
        }
    }

    private void handleForValue(char c) {
        if(c==':') {
            token = Token.VALUE;
            value = new StringBuilder();
        }
    }

    private void handleInKey(char c) {
        if(c=='\"') {
            token=Token.EXPECT_VALUE;
        } else {
            key.append(c);
        }
    }

    private void handleObject(char c) {
        if(c=='\"') {
            token=Token.START_KEY;
            key=new StringBuilder();
        }
    }

    private void handleNone(char c) {
        if(c=='{') {
            token = Token.OBJECT;
        }
    }

}
