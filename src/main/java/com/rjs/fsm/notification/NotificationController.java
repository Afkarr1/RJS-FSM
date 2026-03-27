package com.rjs.fsm.notification;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
public class NotificationController {

    private final WhatsAppService whatsAppService;

    public NotificationController(WhatsAppService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    /**
     * Test endpoint: kirim WA test ke nomor tertentu.
     * Contoh: POST /api/admin/notifications/test-wa?phone=628123456789
     */
    @PostMapping("/test-wa")
    public ResponseEntity<Map<String, Object>> testWa(@RequestParam String phone) {
        String message = "Halo! Ini adalah pesan test dari sistem RJS FSM.\n\n" +
                "Jika Anda menerima pesan ini, berarti integrasi WhatsApp berfungsi dengan baik.\n\n" +
                "- Tim Restu Jaya Sentosa";

        boolean sent = whatsAppService.sendMessage(phone, message);
        return ResponseEntity.ok(Map.of(
                "success", sent,
                "phone", phone,
                "message", sent ? "WA berhasil dikirim" : "Gagal kirim WA — cek log backend untuk detail"
        ));
    }
}
