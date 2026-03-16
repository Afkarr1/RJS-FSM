package com.rjs.fsm.notification;

import com.rjs.fsm.config.AppProperties;
import com.rjs.fsm.job.Job;
import com.rjs.fsm.review.ReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final WhatsAppService whatsApp;
    private final ReviewService reviewService;
    private final String baseUrl;

    public NotificationService(WhatsAppService whatsApp, ReviewService reviewService,
                                AppProperties props) {
        this.whatsApp = whatsApp;
        this.reviewService = reviewService;
        this.baseUrl = props.getBaseUrl();
    }

    /**
     * Send review link to customer after job is finished.
     * Creates a review token and sends a WA message with the link.
     */
    public void sendReviewLinkToCustomer(Job job) {
        try {
            String token = reviewService.createReviewLink(job);
            String reviewUrl = baseUrl + "/api/public/reviews/" + token;

            String message = String.format(
                    "Halo %s,\n\n" +
                    "Terima kasih telah menggunakan layanan Restu Jaya Sentosa.\n\n" +
                    "Pekerjaan \"%s\" telah selesai dikerjakan oleh teknisi kami.\n\n" +
                    "Mohon berikan konfirmasi dan rating melalui link berikut:\n%s\n\n" +
                    "Link berlaku 48 jam.\n\n" +
                    "Terima kasih,\nRestu Jaya Sentosa",
                    job.getCustomerName() != null ? job.getCustomerName() : "Pelanggan",
                    job.getTitle(),
                    reviewUrl
            );

            boolean sent = whatsApp.sendMessage(job.getCustomerPhone(), message);
            if (sent) {
                log.info("Review link sent to customer {} for job {}", job.getCustomerPhone(), job.getId());
            } else {
                log.warn("Failed to send review link to {} for job {}. Review URL: {}",
                        job.getCustomerPhone(), job.getId(), reviewUrl);
            }
        } catch (Exception e) {
            log.error("Error sending review link for job {}", job.getId(), e);
        }
    }

    /**
     * Remind technician to upload photo for a completed job.
     */
    public void remindPhotoUpload(Job job, String techPhone) {
        String message = String.format(
                "Reminder: Job \"%s\" belum memiliki foto bukti pekerjaan.\n" +
                "Mohon upload foto sebelum menyelesaikan pekerjaan.\n\n" +
                "- RJS FSM System",
                job.getTitle()
        );

        whatsApp.sendMessage(techPhone, message);
    }
}
