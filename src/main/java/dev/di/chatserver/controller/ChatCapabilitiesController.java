package dev.di.chatserver.controller;

import org.springframework.web.bind.annotation.*;

@RestController
public class ChatCapabilitiesController {
    public record Capabilities(boolean demoMode, boolean paymentRequestsAvailable) {}
    @GetMapping("/chat/capabilities")
    public Capabilities capabilities() { return new Capabilities(true, false); }
}
