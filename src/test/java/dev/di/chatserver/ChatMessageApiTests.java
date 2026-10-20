package dev.di.chatserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatMessageApiTests {
    @Autowired Environment environment;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private HttpResponse<String> request(String method, String path, String body)
            throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:"
                + environment.getProperty("local.server.port") + path));
        builder.header("Content-Type", "application/json");
        if (method.equals("POST") && path.endsWith("/messages")) {
            builder.header("Idempotency-Key", java.util.UUID.randomUUID().toString());
        }
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> response) {
        return mapper.readTree(response.body());
    }

    private void error(String method, String path, String body, int status, String code)
            throws Exception {
        var response = request(method, path, body);
        assertEquals(status, response.statusCode(), response.body());
        assertEquals(code, json(response).get("code").asText());
    }

    @Test
    void twoParticipantsCanChatButOutsidersAndInvalidMessagesAreRejected() throws Exception {
        String roomBody = "{\"productId\":701,\"buyerId\":1,\"sellerId\":2}";
        var created = request("POST", "/chat/rooms", roomBody);
        assertEquals(201, created.statusCode());
        long roomId = json(created).get("id").asLong();
        var duplicate = request("POST", "/chat/rooms", roomBody);
        assertEquals(200, duplicate.statusCode());
        assertEquals(roomId, json(duplicate).get("id").asLong());
        String path = "/chat/rooms/" + roomId + "/messages";
        assertEquals(0, json(request("GET", path + "?memberId=1", null)).size());

        var first = request("POST", path, "{\"senderId\":1,\"content\":\"안녕하세요\"}");
        var second = request("POST", path, "{\"senderId\":2,\"content\":\"네, 판매 중입니다.\"}");
        assertEquals(201, first.statusCode(), first.body());
        assertEquals(201, second.statusCode(), second.body());
        assertNotNull(json(first).get("sentAt"));
        for (int member : new int[]{1, 2}) {
            var result = request("GET", path + "?memberId=" + member, null);
            assertEquals(200, result.statusCode());
            var messages = json(result);
            assertEquals(2, messages.size());
            assertEquals("안녕하세요", messages.get(0).get("content").asText());
            assertEquals(2, messages.get(1).get("senderId").asInt());
        }

        error("POST", path, "{\"senderId\":3,\"content\":\"침입\"}", 403, "CHAT_ACCESS_DENIED");
        error("GET", path + "?memberId=3", null, 403, "CHAT_ACCESS_DENIED");
        error("POST", path, "{\"senderId\":1,\"content\":\"   \"}", 400, "INVALID_INPUT");
        error("POST", path, "{\"senderId\":1,\"content\":\"" + "a".repeat(1001) + "\"}", 400, "INVALID_INPUT");
        error("POST", path, "{\"content\":\"hello\"}", 400, "INVALID_INPUT");
        error("POST", path, "{", 400, "INVALID_INPUT");
        error("GET", path, null, 400, "INVALID_INPUT");
        error("GET", path + "?memberId=abc", null, 400, "INVALID_INPUT");
        error("GET", "/chat/rooms/999999/messages?memberId=1", null, 404, "CHAT_ROOM_NOT_FOUND");
        error("POST", "/chat/rooms/999999/messages", "{\"senderId\":1,\"content\":\"hello\"}", 404, "CHAT_ROOM_NOT_FOUND");
        assertEquals(2, json(request("GET", path + "?memberId=1", null)).size());

        var other = request("POST", "/chat/rooms", "{\"productId\":702,\"buyerId\":1,\"sellerId\":2}");
        String otherPath = "/chat/rooms/" + json(other).get("id").asLong() + "/messages?memberId=1";
        assertEquals(0, json(request("GET", otherPath, null)).size());
        error("POST", "/chat/rooms", "{\"productId\":-1,\"buyerId\":1,\"sellerId\":2}", 400, "INVALID_INPUT");
        error("POST", "/chat/rooms", "{\"productId\":703,\"buyerId\":1,\"sellerId\":1}", 422, "SELF_CHAT_NOT_ALLOWED");
    }
    @Test
    void readPositionOnlyAdvancesAndIsScopedToRoomAndMember() throws Exception {
        var room = request("POST", "/chat/rooms",
                "{\"productId\":801,\"buyerId\":11,\"sellerId\":12}");
        long roomId = json(room).get("id").asLong();
        String root = "/chat/rooms/" + roomId;
        String path = root + "/read-position";
        assertEquals(0, json(request("GET", path + "?memberId=12", null))
                .get("lastReadMessageId").asLong());
        long first = json(request("POST", root + "/messages",
                "{\"senderId\":11,\"content\":\"첫 메시지\"}")).get("id").asLong();
        long second = json(request("POST", root + "/messages",
                "{\"senderId\":11,\"content\":\"둘째 메시지\"}")).get("id").asLong();
        String latest = "{\"memberId\":12,\"lastReadMessageId\":" + second + "}";
        for (String body : new String[]{latest, latest,
                "{\"memberId\":12,\"lastReadMessageId\":" + first + "}"}) {
            var result = request("PATCH", path, body);
            assertEquals(200, result.statusCode(), result.body());
            assertEquals(second, json(result).get("lastReadMessageId").asLong());
        }
        assertEquals(second, json(request("GET", path + "?memberId=12", null))
                .get("lastReadMessageId").asLong());
        assertEquals(0, json(request("GET", path + "?memberId=11", null))
                .get("lastReadMessageId").asLong());
        error("PATCH", path, "{\"memberId\":13,\"lastReadMessageId\":" + first + "}",
                403, "CHAT_ACCESS_DENIED");
        error("GET", path + "?memberId=13", null, 403, "CHAT_ACCESS_DENIED");
        error("PATCH", path, "{\"memberId\":12,\"lastReadMessageId\":999999}",
                404, "CHAT_MESSAGE_NOT_FOUND");
        error("PATCH", path, "{\"memberId\":12,\"lastReadMessageId\":0}",
                400, "INVALID_INPUT");
        error("PATCH", path, "{\"memberId\":12}", 400, "INVALID_INPUT");
        var otherRoom = request("POST", "/chat/rooms",
                "{\"productId\":802,\"buyerId\":11,\"sellerId\":12}");
        String otherRoot = "/chat/rooms/" + json(otherRoom).get("id").asLong();
        long foreign = json(request("POST", otherRoot + "/messages",
                "{\"senderId\":11,\"content\":\"다른 방\"}")).get("id").asLong();
        error("PATCH", path, "{\"memberId\":12,\"lastReadMessageId\":" + foreign + "}",
                404, "CHAT_MESSAGE_NOT_FOUND");
        assertEquals(second, json(request("GET", path + "?memberId=12", null))
                .get("lastReadMessageId").asLong());
        assertEquals(0, json(request("GET", otherRoot + "/read-position?memberId=12", null))
                .get("lastReadMessageId").asLong());
        error("PATCH", "/chat/rooms/999999/read-position", latest, 404, "CHAT_ROOM_NOT_FOUND");
    }

    @Test
    void concurrentReadUpdatesKeepLargestPosition() throws Exception {
        var repository = new dev.di.chatserver.repository.ReadPositionRepository();
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(8)) {
            var jobs = new java.util.ArrayList<java.util.concurrent.Callable<Long>>();
            for (long id = 100; id > 0; id--) {
                long messageId = id;
                jobs.add(() -> repository.advance(1L, 2L, messageId));
            }
            for (var result : executor.invokeAll(jobs)) result.get();
        }
        assertEquals(100L, repository.find(1L, 2L));
        assertEquals(0L, repository.find(1L, 3L));
    }
}
