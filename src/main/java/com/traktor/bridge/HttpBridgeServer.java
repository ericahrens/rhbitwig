package com.traktor.bridge;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class HttpBridgeServer {
    private final DeckManager deckManager;
    private HttpServer server;
    private boolean isRunning;
    
    private final String[] lastKeys = new String[4];

    public HttpBridgeServer(final DeckManager deckManager) {
        this.deckManager = deckManager;
        this.isRunning = false;
        for (int i = 0; i < lastKeys.length; i++) {
            lastKeys[i] = "";
        }
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/", new TraktorApiHandler());
            server.setExecutor(null);
            server.start();
            isRunning = true;
            System.out.println("✅ HTTP Bridge Server started on port 8080");
        } catch (final IOException e) {
            isRunning = false;
            System.err.println("❌ Failed to start HTTP server: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            isRunning = false;
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    private class TraktorApiHandler implements HttpHandler {
        @Override
        public void handle(final HttpExchange exchange) throws IOException {
            final String path = exchange.getRequestURI().getPath();
            final String method = exchange.getRequestMethod();

            if (!"POST".equalsIgnoreCase(method)) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            final InputStream requestBody = exchange.getRequestBody();
            final String body = new String(requestBody.readAllBytes(), StandardCharsets.UTF_8);

            System.out.println("Received: " + path + " -> " + body);

            if (path.startsWith("/deckLoaded/")) {
                handleDeckLoaded(path, body);
            } else if (path.startsWith("/updateDeck/")) {
                handleDeckUpdate(path, body);
            } else if (path.startsWith("/activeDeck/")) {
                handleActiveDeck(path);
            }

            sendResponse(exchange, 200, "OK");
        }

        private void handleDeckLoaded(final String path, final String body) {
            final int deckIndex = extractDeckIndex(path);
            
            try {
                final JSONObject trackData = new JSONObject(body);
                String key = extractKeyFromJson(trackData);
                
                if (key != null && !key.isEmpty() && !"null".equals(key)) {
                    String cleanKey = key.replace("~", "").trim();
                    if (!cleanKey.equals(lastKeys[deckIndex - 1])) {
                        lastKeys[deckIndex - 1] = cleanKey;
                        deckManager.setDeckKey(deckIndex - 1, cleanKey);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }

        private void handleDeckUpdate(final String path, final String body) {
            final int deckIndex = extractDeckIndex(path);
            
            try {
                final JSONObject deckState = new JSONObject(body);
                String key = extractKeyFromJson(deckState);
                
                if (key != null && !key.isEmpty() && !"null".equals(key)) {
                    String cleanKey = key.replace("~", "").trim();
                    if (!cleanKey.equals(lastKeys[deckIndex - 1])) {
                        lastKeys[deckIndex - 1] = cleanKey;
                        deckManager.setDeckKey(deckIndex - 1, cleanKey);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }

        private void handleActiveDeck(final String path) {
            final int deckIndex = extractDeckIndex(path);
            deckManager.activateDeck(deckIndex - 1);
        }
        
        private String extractKeyFromJson(JSONObject json) {
            if (json.has("resultingKey")) {
                String key = json.optString("resultingKey", null);
                if (key != null && !key.isEmpty() && !"null".equals(key)) {
                    return key;
                }
            }
            if (json.has("key_text")) {
                String key = json.optString("key_text", null);
                if (key != null && !key.isEmpty() && !"null".equals(key)) {
                    return key;
                }
            }
            if (json.has("key")) {
                String key = json.optString("key", null);
                if (key != null && !key.isEmpty() && !"null".equals(key)) {
                    return key;
                }
            }
            if (json.has("track_key")) {
                String key = json.optString("track_key", null);
                if (key != null && !key.isEmpty() && !"null".equals(key)) {
                    return key;
                }
            }
            return null;
        }

        private int extractDeckIndex(final String path) {
            final String[] parts = path.split("/");
            String lastPart = parts[parts.length - 1];
            return switch (lastPart.toUpperCase()) {
                case "A" -> 1;
                case "B" -> 2;
                case "C" -> 3;
                case "D" -> 4;
                default -> 1;
            };
        }

        private void sendResponse(final HttpExchange exchange, final int statusCode, final String response) throws IOException {
            exchange.sendResponseHeaders(statusCode, response.getBytes().length);
            final OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}