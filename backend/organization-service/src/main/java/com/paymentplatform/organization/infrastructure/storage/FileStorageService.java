package com.paymentplatform.organization.infrastructure.storage;

import com.paymentplatform.shared.domain.exception.ConflictException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final long MAX_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png");

    @Value("${app.upload.dir:uploads/products}")
    private String uploadDir;

    /**
     * Enregistre le contenu binaire d'une image produit sur disque.
     * Volontairement découplé de Spring Web : l'adaptateur REST extrait
     * le flux depuis le MultipartFile et délègue ici le stockage pur.
     */
    public String storeFile(InputStream contenu, String nomFichier, String contentType, long taille,
                            UUID supplierId, UUID productId) {
        if (contenu == null || taille <= 0) {
            throw new ConflictException("Le fichier ne peut pas être vide");
        }
        if (taille > MAX_SIZE) {
            throw new ConflictException("La taille du fichier ne doit pas dépasser 5 Mo");
        }
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new ConflictException("Seuls les formats JPG et PNG sont acceptés");
        }
        try {
            String extension = contentType.contains("png") ? ".png" : ".jpg";
            String filename = supplierId + "/" + productId + "_" + UUID.randomUUID() + extension;
            Path targetDir = Paths.get(uploadDir, supplierId.toString());
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(filename.replace(supplierId + "/", ""));
            Files.copy(contenu, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/products/" + filename;
        } catch (IOException e) {
            throw new ConflictException("Erreur lors de l'enregistrement du fichier : " + e.getMessage());
        }
    }
}
