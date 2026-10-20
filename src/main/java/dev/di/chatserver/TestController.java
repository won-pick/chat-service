package dev.di.chatserver;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestController {

    @GetMapping("/chat/test")
    public Map<String, String> test() {
        return Map.of("message", "채팅 서버 연결 성공!");
    }
}
