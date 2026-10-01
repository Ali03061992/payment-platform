package com.paymentplatform.organization.infrastructure.storage;

import com.paymentplatform.shared.domain.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests de FileStorageService.
 * Périmètre : validation et enregistrement des images produits sur disque.
 * Moyens : JUnit pur avec répertoire temporaire et flux réels, zéro simulation.
 */
@DisplayName("Stockage des images produits : validation et écriture réelle sur disque.")
class FileStorageServiceTests {

    private FileStorageService stockage;

    @TempDir
    Path repertoireTemporaire;

    @BeforeEach
    void preparer() {
        stockage = new FileStorageService();
        ReflectionTestUtils.setField(stockage, "uploadDir", repertoireTemporaire.toString());
    }

    @Test
    @DisplayName("Une image JPG valide est enregistrée et retourne un chemin public.")
    void storeFile_imageJpgValide_enregistreFichier() {
        byte[] contenu = new byte[]{(byte) 0xFF, (byte) 0xD8, 0x01, 0x02};

        String chemin = stockage.storeFile(new ByteArrayInputStream(contenu), "photo.jpg",
                "image/jpeg", contenu.length,
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-000000000002"));

        assertThat(chemin).startsWith("/uploads/products/");
        assertThat(chemin).endsWith(".jpg");
    }

    @Test
    @DisplayName("Une image PNG valide est enregistrée avec l'extension png.")
    void storeFile_imagePngValide_enregistreFichier() {
        byte[] contenu = new byte[]{1, 2, 3};

        String chemin = stockage.storeFile(new ByteArrayInputStream(contenu), "photo.png",
                "image/png", contenu.length, UUID.randomUUID(), UUID.randomUUID());

        assertThat(chemin).endsWith(".png");
    }

    @Test
    @DisplayName("Un contenu vide est rejeté avec un message en français.")
    void storeFile_contenuVide_leveConflit() {
        assertThatThrownBy(() -> stockage.storeFile(new ByteArrayInputStream(new byte[0]), "vide.jpg",
                "image/jpeg", 0, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("vide");
    }

    @Test
    @DisplayName("Un fichier de plus de 5 Mo est rejeté.")
    void storeFile_fichierTropLourd_leveConflit() {
        byte[] contenu = new byte[16];
        long tailleExcessive = 5L * 1024 * 1024 + 1;

        assertThatThrownBy(() -> stockage.storeFile(new ByteArrayInputStream(contenu), "lourd.jpg",
                "image/jpeg", tailleExcessive, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("5 Mo");
    }

    @Test
    @DisplayName("Un type non image est rejeté.")
    void storeFile_typeInterdit_leveConflit() {
        byte[] contenu = new byte[]{1, 2};

        assertThatThrownBy(() -> stockage.storeFile(new ByteArrayInputStream(contenu), "doc.pdf",
                "application/pdf", contenu.length, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("JPG");
    }
}
