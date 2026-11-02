package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.domain.model.DisputeAttachment;
import com.paymentplatform.organization.domain.repository.DisputeAttachmentRepository;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/** Stockage local et lecture des pieces jointes de litige. */
@Service
public class DisputeAttachmentService {

    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/jpg", "image/png");

    private final DisputeAttachmentRepository attachments;
    private final Path baseDirectory;

    public DisputeAttachmentService(DisputeAttachmentRepository attachments,
                                    @Value("${app.upload.dir:uploads/products}") String uploadDir) {
        this.attachments = attachments;
        this.baseDirectory = Paths.get(uploadDir, "dispute-attachments").toAbsolutePath().normalize();
    }

    @Transactional
    public DisputeAttachment store(UUID disputeId, UUID uploadedBy, String originalFileName,
                                   String contentType, long sizeBytes, InputStream input) {
        if (sizeBytes <= 0 || input == null) {
            throw new ConflictException("Le fichier est requis");
        }
        if (sizeBytes > MAX_SIZE_BYTES) {
            throw new ConflictException("La pièce jointe ne doit pas dépasser 5 Mo");
        }
        String normalizedType = contentType == null ? "" : contentType.toLowerCase();
        if (!ALLOWED_CONTENT_TYPES.contains(normalizedType)) {
            throw new ConflictException("Format non autorisé : seuls PDF, JPEG et PNG sont acceptés");
        }
        String fileName = sanitizeFileName(originalFileName);
        String storedName = UUID.randomUUID() + extension(fileName);
        Path directory = baseDirectory.resolve(disputeId.toString());
        Path target = directory.resolve(storedName).normalize();
        if (!target.startsWith(directory)) {
            throw new ConflictException("Nom de fichier invalide");
        }
        try {
            Files.createDirectories(directory);
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ConflictException("Impossible d'enregistrer la pièce jointe");
        }
        DisputeAttachment attachment = DisputeAttachment.create(disputeId, null, fileName, normalizedType,
                sizeBytes, target.toString(), uploadedBy);
        return attachments.save(attachment);
    }

    @Transactional(readOnly = true)
    public AttachmentDownload load(UUID disputeId, UUID attachmentId) {
        DisputeAttachment attachment = attachments.findById(attachmentId)
                .orElseThrow(() -> new NotFoundException("Pièce jointe non trouvée : " + attachmentId));
        if (!attachment.getDisputeId().equals(disputeId)) {
            throw new NotFoundException("Pièce jointe non trouvée : " + attachmentId);
        }
        Path path = Paths.get(attachment.getStoragePath()).toAbsolutePath().normalize();
        if (!path.startsWith(baseDirectory) || !Files.isRegularFile(path)) {
            throw new NotFoundException("Fichier de la pièce jointe introuvable");
        }
        return new AttachmentDownload(path, attachment.getContentType(), attachment.getFileName(),
                attachment.getSizeBytes());
    }

    public record AttachmentDownload(Path path, String contentType, String fileName, long sizeBytes) {
    }

    private static String sanitizeFileName(String original) {
        String name = original == null ? "piece-jointe" : Paths.get(original).getFileName().toString();
        String cleaned = name.replaceAll("[\\r\\n\\t]", " ").trim();
        if (cleaned.isEmpty() || cleaned.length() > 200) {
            return "piece-jointe";
        }
        return cleaned;
    }

    private static String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        String ext = fileName.substring(dot).toLowerCase();
        return ext.matches("\\.[a-z0-9]{1,5}") ? ext : "";
    }
}
