package com.rjs.fsm.photo;

import com.rjs.fsm.photo.dto.JobPhotoResponse;
import com.rjs.fsm.security.CurrentUserProvider;
import com.rjs.fsm.storage.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@RestController
public class PhotoController {

    private final JobPhotoService photoService;
    private final StorageService storageService;
    private final CurrentUserProvider currentUser;

    public PhotoController(JobPhotoService photoService, StorageService storageService,
                           CurrentUserProvider currentUser) {
        this.photoService = photoService;
        this.storageService = storageService;
        this.currentUser = currentUser;
    }

    // Tech uploads photo
    @PostMapping("/api/tech/jobs/{jobId}/photos")
    public JobPhotoResponse techUploadPhoto(@PathVariable UUID jobId,
                                             @RequestParam("file") MultipartFile file) {
        return photoService.uploadPhoto(jobId, file, currentUser.getCurrentUserId());
    }

    // Tech views photos of their job
    @GetMapping("/api/tech/jobs/{jobId}/photos")
    public List<JobPhotoResponse> techListPhotos(@PathVariable UUID jobId) {
        return photoService.listPhotos(jobId);
    }

    // Admin views photos of any job
    @GetMapping("/api/admin/jobs/{jobId}/photos")
    public List<JobPhotoResponse> adminListPhotos(@PathVariable UUID jobId) {
        return photoService.listPhotos(jobId);
    }

    // Admin can also upload
    @PostMapping("/api/admin/jobs/{jobId}/photos")
    public JobPhotoResponse adminUploadPhoto(@PathVariable UUID jobId,
                                              @RequestParam("file") MultipartFile file) {
        return photoService.uploadPhoto(jobId, file, currentUser.getCurrentUserId());
    }

    // Download photo (authenticated)
    @GetMapping("/api/photos/{photoId}/download")
    public ResponseEntity<Resource> downloadPhoto(@PathVariable UUID photoId) throws MalformedURLException {
        JobPhoto photo = photoService.getPhoto(photoId);
        Path path = storageService.resolve(photo.getFilePath());
        Resource resource = new UrlResource(path.toUri());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        photo.getMimeType() != null ? photo.getMimeType() : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + photo.getFileName() + "\"")
                .body(resource);
    }
}
