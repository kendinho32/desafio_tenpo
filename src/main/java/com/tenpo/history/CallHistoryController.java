package com.tenpo.history;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/history")
public class CallHistoryController {

    private final CallHistoryService historyService;

    public CallHistoryController(CallHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public Mono<PageResponse<CallLog>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return historyService.findHistory(page, size);
    }
}
