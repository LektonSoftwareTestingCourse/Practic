package com.processing.binlookup.controller;

import com.processing.binlookup.dto.BinLookupResponse;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for BIN-to-issuer resolution.
 *
 * <p>Exposes a static lookup table for testing. Supports artificial
 * delay and failure via query parameters for timeout/retry scenarios.
 */
@RestController
@RequestMapping("/api/bin")
public class BinLookupController {

    private static final Map<String, BinLookupResponse> BIN_TABLE = Map.of(
            "400000", new BinLookupResponse("400000", "ISS001", "Test Bank 1"),
            "400001", new BinLookupResponse("400001", "ISS002", "Test Bank 2"),
            "400002", new BinLookupResponse("400002", "ISS003", "Test Bank 3"),
            "400003", new BinLookupResponse("400003", "ISS004", "Test Bank 4"),
            "400004", new BinLookupResponse("400004", "ISS005", "Test Bank 5")
    );

    /**
     * Looks up a BIN in the static table.
     *
     * @param bin   6-digit BIN to resolve
     * @param delay artificial response delay in milliseconds (0 = no delay)
     * @param fail  if {@code true}, returns HTTP 500 regardless of BIN
     * @return 200 with {@link BinLookupResponse}, 404 if unknown, 500 if fail=true
     */
    @GetMapping("/{bin}")
    public ResponseEntity<BinLookupResponse> lookup(
            @PathVariable String bin,
            @RequestParam(required = false, defaultValue = "0") int delay,
            @RequestParam(required = false, defaultValue = "false") boolean fail) {

        if (fail) {
            return ResponseEntity.status(500).build();
        }
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        BinLookupResponse response = BIN_TABLE.get(bin);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
}
