package com.plateforme.scolaire.web;

import com.plateforme.scolaire.domain.PhotoEleve;
import com.plateforme.scolaire.service.PhotosEleveService;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Photo d'identité d'un élève : lecture dans son périmètre, modification par la gestion de son établissement. */
@RestController
@RequiredArgsConstructor
public class PhotoEleveController {

    private final PhotosEleveService service;

    @GetMapping("/api/eleves/{id}/photo")
    @PreAuthorize("@perimetre.peutAccederEleve(#id)")
    public ResponseEntity<byte[]> photo(@PathVariable Long id) {
        PhotoEleve p = service.lire(id);
        return ResponseEntity.ok().header("X-Content-Type-Options", "nosniff").contentType(MediaType.parseMediaType(p.getTypeMime())).body(p.getContenu());
    }

    @GetMapping("/api/eleves/{id}/photo/info")
    @PreAuthorize("@perimetre.peutAccederEleve(#id)")
    public Map<String, Object> info(@PathVariable Long id) {
        Long taille = service.taille(id);
        return Map.of("presente", taille != null, "taille", taille == null ? 0L : taille);
    }

    @PostMapping(value = "/api/eleves/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@perimetre.peutGererEleve(#id)")
    public void enregistrer(@PathVariable Long id, @RequestPart("fichier") MultipartFile fichier) throws IOException {
        service.enregistrer(id, fichier);
    }

    @DeleteMapping("/api/eleves/{id}/photo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@perimetre.peutGererEleve(#id)")
    public void supprimer(@PathVariable Long id) {
        service.supprimer(id);
    }
}
