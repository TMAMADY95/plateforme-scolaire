package com.plateforme.scolaire.web;

import com.plateforme.scolaire.domain.PieceJointe;
import com.plateforme.scolaire.service.PiecesJointesService;
import com.plateforme.scolaire.web.dto.PieceJointeDto;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Pièces jointes des inspections. Les contrôles de périmètre sont faits par le service. */
@RestController
@RequiredArgsConstructor
public class PiecesJointesController {

    private final PiecesJointesService service;

    @GetMapping("/api/inspections/{id}/pieces")
    @PreAuthorize("isAuthenticated()")
    public List<PieceJointeDto> lister(@PathVariable Long id) {
        return service.lister(id);
    }

    @PostMapping(value = "/api/inspections/{id}/pieces", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MINISTERE','DIRECTION_REGIONALE')")
    public PieceJointeDto ajouter(@PathVariable Long id, @RequestPart("fichier") MultipartFile fichier) throws IOException {
        return service.ajouter(id, fichier);
    }

    @GetMapping("/api/pieces/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> telecharger(@PathVariable Long id) {
        PieceJointe p = service.lire(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(p.getNom(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(p.getTypeMime()))
                .body(p.getContenu());
    }

    @DeleteMapping("/api/pieces/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('MINISTERE','DIRECTION_REGIONALE')")
    public void supprimer(@PathVariable Long id) {
        service.supprimer(id);
    }
}
