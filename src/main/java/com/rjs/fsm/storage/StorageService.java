package com.rjs.fsm.storage;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface StorageService {

    /**
     * Store a file and return the relative path.
     */
    String store(MultipartFile file, String subDirectory);

    /**
     * Resolve a relative path to absolute Path for serving.
     */
    Path resolve(String relativePath);

    /**
     * Delete a stored file.
     */
    void delete(String relativePath);
}
