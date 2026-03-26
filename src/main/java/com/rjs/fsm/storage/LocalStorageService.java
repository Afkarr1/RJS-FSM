package com.rjs.fsm.storage;

import com.rjs.fsm.config.AppProperties;
import com.rjs.fsm.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private final Path rootDir;
    private final int maxWidth;
    private final double quality;

    public LocalStorageService(AppProperties props) {
        this.rootDir = Paths.get(props.getStorage().getUploadDir()).toAbsolutePath().normalize();
        this.maxWidth = props.getStorage().getMaxImageWidth();
        this.quality = props.getStorage().getCompressQuality();
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(rootDir);
        log.info("Storage directory: {}", rootDir);
    }

    @Override
    public String store(MultipartFile file, String subDirectory) {
        if (file.isEmpty()) throw new BadRequestException("File kosong");

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BadRequestException("Tipe file tidak didukung. Gunakan JPEG, PNG, atau WebP.");
        }

        try {
            LocalDate now = LocalDate.now();
            String datePath = now.getYear() + "/" + String.format("%02d", now.getMonthValue());
            Path dir = rootDir.resolve(subDirectory).resolve(datePath);
            Files.createDirectories(dir);

            String ext = contentType.equals("image/png") ? ".png" :
                         contentType.equals("image/webp") ? ".webp" : ".jpg";
            String fileName = UUID.randomUUID() + ext;
            Path target = dir.resolve(fileName);

            // Compress image
            try (InputStream is = file.getInputStream()) {
                BufferedImage original = ImageIO.read(is);
                if (original == null) throw new BadRequestException("File bukan gambar yang valid");

                if (original.getWidth() > maxWidth) {
                    Thumbnails.of(original)
                            .width(maxWidth)
                            .outputQuality(quality)
                            .toFile(target.toFile());
                } else {
                    Thumbnails.of(original)
                            .scale(1.0)
                            .outputQuality(quality)
                            .toFile(target.toFile());
                }
            }

            String relativePath = subDirectory + "/" + datePath + "/" + fileName;
            log.info("File stored: {}", relativePath);
            return relativePath;

        } catch (IOException e) {
            throw new RuntimeException("Gagal menyimpan file", e);
        }
    }

    @Override
    public Path resolve(String relativePath) {
        return rootDir.resolve(relativePath).normalize();
    }

    @Override
    public void delete(String relativePath) {
        try {
            Path path = resolve(relativePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", relativePath, e);
        }
    }
}
