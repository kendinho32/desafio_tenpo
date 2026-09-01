package com.tenpo.history;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

@Tag(name = "History", description = "Consulta paginada del historial de llamadas")
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
        if (page < 0) {
            throw new ServerWebInputException("page debe ser mayor o igual a 0");
        }
        if (size < 1) {
            throw new ServerWebInputException("size debe ser mayor o igual a 1");
        }
        return historyService.findHistory(page, size);
    }
}
