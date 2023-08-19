package com.allenheath.k2.set1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

import com.rhcommons.json.JsonObject;
import com.rhcommons.json.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class HttpJsonHandler implements HttpHandler {
    
    private final TraktorState traktorState;
    
    public HttpJsonHandler(TraktorState traktorState) {
        this.traktorState = traktorState;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if("POST".equals(exchange.getRequestMethod())) {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int length; (length = exchange.getRequestBody().read(buffer)) != -1; ) {
                result.write(buffer, 0, length);
            }
            String info = result.toString();
            if(info.startsWith("{\"deckId\"")) {
                JsonObject data = new JsonParser(info).parse();
                final Optional<String> deck = data.getStringValue("deckId");
                deck.ifPresent(deckId-> {
                    int deckIndex = deckId.charAt(0)-'A';
                    JsonObject metadata = data.getJsonObject("metadata");
                    if(metadata != null) {
                        AllenHeathK2ControllerExtension.println(" DECK %s %d",deckId,deckIndex);
                        traktorState.setKey(deckIndex, metadata.getStringValue("key").orElse("ERR"));
                    }
                });
            }
            OutputStream outputStream = exchange.getResponseBody();
            exchange.sendResponseHeaders(200, 0);
            outputStream.flush();
            outputStream.close();
        } else {
            AllenHeathK2ControllerExtension.println(" OTHER %s",exchange.getRequestMethod());
        }
    }


}
