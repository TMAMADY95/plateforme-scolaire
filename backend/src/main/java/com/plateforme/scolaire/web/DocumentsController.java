package com.plateforme.scolaire.web;

import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.service.DocumentsPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Documents PDF : bulletin et reçu de paiement. */
@RestController
@RequiredArgsConstructor
public class DocumentsController {

    private final DocumentsPdfService documents;
    private final PerimetreService perimetreService;

    @GetMapping("/api/eleves/{id}/bulletin.pdf")
    @PreAuthorize("@perimetre.peutAccederEleve(#id)")
    public ResponseEntity<byte[]> bulletin(@PathVariable Long id, @RequestParam Long periodeId) {
        return pdf(documents.bulletin(id, periodeId), "bulletin-" + id + "-" + periodeId + ".pdf");
    }

    /** Le service décide des droits : direction/administration, élève concerné ou parents. */
    @GetMapping("/api/factures/{id}/recu.pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> recu(@PathVariable Long id) {
        return pdf(documents.recu(perimetreService.utilisateurCourant().getId(), id), "recu-facture-" + id + ".pdf");
    }

    private static ResponseEntity<byte[]> pdf(byte[] contenu, String nom) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nom).build().toString())
                .body(contenu);
    }
}
