package com.tomo.tomoassistant.controller;

import com.tomo.tomoassistant.common.result.Result;
import com.tomo.tomoassistant.config.LlmModelHolder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final LlmModelHolder modelHolder;

    @GetMapping("/llm-mode")
    public Result<Map<String, String>> getLlmMode() {
        return Result.ok(Map.of("mode", modelHolder.getCurrentMode()));
    }

    @PutMapping("/llm-mode")
    public Result<Map<String, String>> switchLlmMode(@RequestBody SwitchModeRequest request) {
        try {
            modelHolder.switchMode(request.getMode());
            return Result.ok(Map.of("mode", modelHolder.getCurrentMode()));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @Data
    public static class SwitchModeRequest {
        private String mode;
    }
}
