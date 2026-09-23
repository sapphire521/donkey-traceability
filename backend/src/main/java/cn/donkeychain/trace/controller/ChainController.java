package cn.donkeychain.trace.controller;

import cn.donkeychain.trace.common.Result;
import cn.donkeychain.trace.service.ChainService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ChainController {

    private final ChainService chain;

    @GetMapping("/chain/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(chain.chainStats());
    }

    @GetMapping("/chain/verify")
    public Result<Map<String, Object>> verify() {
        return Result.ok(chain.verifyChain());
    }

    @GetMapping("/chain/blocks")
    public Result<Map<String, Object>> blocks(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String keyword) {
        return Result.ok(chain.listBlocks((int) page, (int) size, keyword));
    }

    @GetMapping("/chain/tx/{txId}")
    public Result<Map<String, Object>> tx(@PathVariable String txId) {
        return Result.ok(chain.findTx(txId));
    }
}
