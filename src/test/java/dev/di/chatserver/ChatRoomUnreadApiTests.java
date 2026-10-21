package dev.di.chatserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.core.env.Environment;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatRoomUnreadApiTests {
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

    private JsonNode summary(long roomId, long memberId) throws Exception {
        var response = request("GET", "/chat/rooms?memberId=" + memberId, null);
        assertEquals(200, response.statusCode(), response.body());
        for (var room : json(response)) {
            if (room.get("id").asLong() == roomId) return room;
        }
        throw new AssertionError("참여한 채팅방이 목록에 없습니다.");
    }

    @Test
    void unreadCountsExcludeOwnMessagesAndFollowReadPosition() throws Exception {
        long roomId = json(request("POST", "/chat/rooms",
                "{\"productId\":901,\"buyerId\":21,\"sellerId\":22}"))
                .get("id").asLong();
        String root = "/chat/rooms/" + roomId;
        assertEquals(0, summary(roomId, 21).get("unreadCount").asLong());
        assertTrue(summary(roomId, 21).get("lastMessage").isNull());
        long first = json(request("POST", root + "/messages",
                "{\"senderId\":21,\"content\":\"첫 문의\"}")).get("id").asLong();
        assertEquals(0, summary(roomId, 21).get("unreadCount").asLong());
        assertEquals(1, summary(roomId, 22).get("unreadCount").asLong());
        long reply = json(request("POST", root + "/messages",
                "{\"senderId\":22,\"content\":\"답장\"}")).get("id").asLong();
        long third = json(request("POST", root + "/messages",
                "{\"senderId\":21,\"content\":\"추가 문의\"}")).get("id").asLong();
        assertEquals(1, summary(roomId, 21).get("unreadCount").asLong());
        assertEquals(2, summary(roomId, 22).get("unreadCount").asLong());
        assertEquals(third, summary(roomId, 22).get("lastMessage").get("id").asLong());
        assertEquals("추가 문의", summary(roomId, 22).get("lastMessage").get("content").asText());

        var partial = request("PATCH", root + "/read-position",
                "{\"memberId\":22,\"lastReadMessageId\":" + first + "}");
        assertEquals(200, partial.statusCode());
        assertEquals(1, summary(roomId, 22).get("unreadCount").asLong());
        assertEquals(1, summary(roomId, 21).get("unreadCount").asLong());
        request("PATCH", root + "/read-position",
                "{\"memberId\":22,\"lastReadMessageId\":" + third + "}");
        assertEquals(0, summary(roomId, 22).get("unreadCount").asLong());
        request("PATCH", root + "/read-position",
                "{\"memberId\":22,\"lastReadMessageId\":" + first + "}");
        assertEquals(0, summary(roomId, 22).get("unreadCount").asLong());
        request("PATCH", root + "/read-position",
                "{\"memberId\":21,\"lastReadMessageId\":" + reply + "}");
        assertEquals(0, summary(roomId, 21).get("unreadCount").asLong());

        long otherId = json(request("POST", "/chat/rooms",
                "{\"productId\":902,\"buyerId\":21,\"sellerId\":22}"))
                .get("id").asLong();
        request("POST", "/chat/rooms/" + otherId + "/messages",
                "{\"senderId\":21,\"content\":\"다른 방\"}");
        assertEquals(1, summary(otherId, 22).get("unreadCount").asLong());
        assertEquals(0, summary(roomId, 22).get("unreadCount").asLong());
        assertEquals(0, json(request("GET", "/chat/rooms?memberId=23", null)).size());
        request("POST", root + "/messages", "{\"senderId\":21,\"content\":\"새 메시지\"}");
        assertEquals(1, summary(roomId, 22).get("unreadCount").asLong());
    }
}
