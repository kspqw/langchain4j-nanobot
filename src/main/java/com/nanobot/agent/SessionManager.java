package com.nanobot.agent;

import com.nanobot.session.Session;
import com.google.gson.*;
import com.google.gson.stream.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private final Path workspace;
    private final ConcurrentHashMap<String, Session> cache = new ConcurrentHashMap<>();
    private final Gson gson;

    public SessionManager(Path workspace) {
        this.workspace = workspace;
        this.gson = createGson();
    }

    private static Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .create();
    }

    private static class InstantTypeAdapter extends TypeAdapter<Instant> {
        @Override
        public void write(JsonWriter out, Instant value) throws java.io.IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.toString());
            }
        }

        @Override
        public Instant read(JsonReader in) throws java.io.IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String value = in.nextString();
            return value != null && !value.isEmpty() ? Instant.parse(value) : Instant.now();
        }
    }

    public Session getOrCreate(String key) {
        return cache.computeIfAbsent(key, k -> {
            Session s = new Session(k);
            loadFromFile(s);
            return s;
        });
    }

    public void save(Session session) {
        Path dir = workspace.resolve("sessions");
        try {
            Files.createDirectories(dir);
            String safeKey = session.key.replaceAll("[^a-zA-Z0-9_-]", "_");
            Path file = dir.resolve(safeKey + ".json");
            String json = gson.toJson(session);
            Files.writeString(file, json);
        } catch (Exception e) {
            System.err.println("Failed to save session: " + e.getMessage());
        }
    }

    public void invalidate(String key) {
        cache.remove(key);
    }

    private void loadFromFile(Session session) {
        Path dir = workspace.resolve("sessions");
        try {
            if (!Files.exists(dir)) return;
            String safeKey = session.key.replaceAll("[^a-zA-Z0-9_-]", "_");
            Path file = dir.resolve(safeKey + ".json");
            if (Files.exists(file)) {
                String json = Files.readString(file);
                Session loaded = gson.fromJson(json, Session.class);
                session.messages = loaded.messages;
                session.lastConsolidated = loaded.lastConsolidated;
                session.createdAt = loaded.createdAt;
                session.updatedAt = loaded.updatedAt;
            }
        } catch (Exception e) {
            // ignore - return empty session
        }
    }
}
