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
class ChatReliabilityApiTests {
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

    private HttpResponse<String> keyed(String path, String body, String key) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:"
                + environment.getProperty("local.server.port") + path))
                .header("Content-Type", "application/json");
        if (key != null) builder.header("Idempotency-Key", key);
        return client.send(builder.POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void duplicateConcurrentMessageRequestsCreateOnlyOneMessage() throws Exception {
        long id = json(request("POST", "/chat/rooms",
                "{\"productId\":1001,\"buyerId\":31,\"sellerId\":32}"))
                .get("id").asLong();
        String path = "/chat/rooms/" + id + "/messages";
        String body = "{\"senderId\":31,\"content\":\"한 번만 저장\"}";
        long messageId;
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(8)) {
            var jobs = new java.util.ArrayList<java.util.concurrent.Callable<HttpResponse<String>>>();
            for (int i=0;i<20;i++) jobs.add(() -> keyed(path, body, "same-key"));
            var results = pool.invokeAll(jobs);
            messageId = json(results.getFirst().get()).get("id").asLong();
            for (var result : results) {
                var response = result.get();
                assertEquals(201, response.statusCode(), response.body());
                assertEquals(messageId, json(response).get("id").asLong());
                assertEquals("/chat/rooms/" + id + "/messages/" + messageId,
                        response.headers().firstValue("Location").orElseThrow());
            }
        }
        assertEquals(1, json(request("GET", path + "?memberId=32", null)).size());
        var conflict = keyed(path,"{\"senderId\":31,\"content\":\"다른 내용\"}","same-key");
        assertEquals(409, conflict.statusCode());
        assertEquals("IDEMPOTENCY_CONFLICT", json(conflict).get("code").asText());
        assertEquals(400, keyed(path,body,null).statusCode());
        assertEquals(400, keyed(path,body,"bad key").statusCode());
        var newIntent = keyed(path,body,"new-key");
        assertEquals(201,newIntent.statusCode());
        assertNotEquals(messageId,json(newIntent).get("id").asLong());
        assertEquals(2, json(request("GET", path + "?memberId=32", null)).size());
        // 키는 방·발신자별로 분리됩니다.
        assertEquals(201,keyed(path,"{\"senderId\":32,\"content\":\"답장\"}","same-key").statusCode());
        long other = json(request("POST", "/chat/rooms",
                "{\"productId\":1002,\"buyerId\":31,\"sellerId\":32}"))
                .get("id").asLong();
        assertEquals(201,keyed("/chat/rooms/"+other+"/messages",body,"same-key").statusCode());
        error("GET","/chat/rooms/"+id+"?memberId=33",null,403,"CHAT_ACCESS_DENIED");
        assertEquals(200,request("GET","/chat/rooms/"+id+"?memberId=31",null).statusCode());
        assertEquals(200,request("GET",path+"/"+messageId+"?memberId=32",null).statusCode());
        error("GET",path+"/"+messageId+"?memberId=33",null,403,"CHAT_ACCESS_DENIED");
        error("GET",path+"?memberId=-1",null,400,"INVALID_INPUT");
        error("GET",path+"?memberId=31&limit=101",null,400,"INVALID_INPUT");
        error("GET",path+"?memberId=31&afterId=0&beforeId=99",null,400,"INVALID_INPUT");
    }

    @Test
    void cursorPaginationAndPeerReadAreConsistent() throws Exception {
        long room = json(request("POST", "/chat/rooms",
                "{\"productId\":1003,\"buyerId\":41,\"sellerId\":42}"))
                .get("id").asLong();
        String root = "/chat/rooms/" + room;
        var ids = new java.util.ArrayList<Long>();
        for (int i=0; i<6; i++) ids.add(json(keyed(root+"/messages",
                "{\"senderId\":41,\"content\":\"메시지 " + i + "\"}", "page-"+i)).get("id").asLong());
        var recent = json(request("GET",root+"/messages?memberId=42&limit=2",null));
        assertEquals(ids.get(4).longValue(),recent.get(0).get("id").asLong());
        assertEquals(ids.get(5).longValue(),recent.get(1).get("id").asLong());
        var older = json(request("GET",root+"/messages?memberId=42&limit=2&beforeId="+ids.get(4),null));
        assertEquals(ids.get(2).longValue(),older.get(0).get("id").asLong());
        var newer = json(request("GET",root+"/messages?memberId=42&limit=2&afterId="+ids.get(0),null));
        assertEquals(ids.get(1).longValue(),newer.get(0).get("id").asLong());
        assertEquals(0,json(request("GET",root+"/read-position/peer?memberId=41",null)).get("lastReadMessageId").asLong());
        request("PATCH",root+"/read-position","{\"memberId\":42,\"lastReadMessageId\":"+ids.get(5)+"}");
        var peer = json(request("GET",root+"/read-position/peer?memberId=41",null));
        assertEquals(42,peer.get("memberId").asInt());
        assertEquals(ids.get(5).longValue(),peer.get("lastReadMessageId").asLong());
        error("GET",root+"/read-position/peer?memberId=43",null,403,"CHAT_ACCESS_DENIED");
    }

    @Test
    void concurrentRoomRequestsShareOneRoom() throws Exception {
        try (var pool=java.util.concurrent.Executors.newFixedThreadPool(8)) {
            var jobs=new java.util.ArrayList<java.util.concurrent.Callable<HttpResponse<String>>>();
            for(int i=0;i<12;i++) jobs.add(() -> request("POST","/chat/rooms",
                    "{\"productId\":1004,\"buyerId\":51,\"sellerId\":52}"));
            var results=pool.invokeAll(jobs);
            long id=json(results.getFirst().get()).get("id").asLong();
            int created=0;
            for(var future:results) {
                var response=future.get();
                if(response.statusCode()==201) created++;
                else assertEquals(200,response.statusCode());
                assertEquals(id,json(response).get("id").asLong());
            }
            assertEquals(1,created);
        }
    }
}
