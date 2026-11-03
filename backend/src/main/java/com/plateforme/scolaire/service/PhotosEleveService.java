package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.PhotoEleve;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.PhotoEleveRepository;
import com.plateforme.scolaire.security.PerimetreService;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Photo d'identité des élèves : JPEG ou PNG, 1 Mo au plus. Le contrôle d'accès est fait par le contrôleur (périmètre de l'élève). */
@Service
@Transactional
public class PhotosEleveService {

    private final PhotoEleveRepository photos;
    private final EleveRepository eleves;
    private final PerimetreService perimetre;
    private final AuditService audit;
    private final Clock clock;
    private final long tailleMax;

    public PhotosEleveService(PhotoEleveRepository photos, EleveRepository eleves, PerimetreService perimetre, AuditService audit, Clock clock,
                              @Value("${app.photo-eleve-max:1MB}") org.springframework.util.unit.DataSize tailleMax) {
        this.photos = photos;
        this.eleves = eleves;
        this.perimetre = perimetre;
        this.audit = audit;
        this.clock = clock;
        this.tailleMax = tailleMax.toBytes();
    }

    /** Pur : type réel d'une photo (JPEG ou PNG, d'après la signature du fichier) ; null sinon. Les PDF sont refusés. */
    static String typePhoto(byte[] contenu) {
        String type = PiecesJointesService.typeReel(contenu);
        return "image/jpeg".equals(type) || "image/png".equals(type) ? type : null;
    }

    public void enregistrer(Long eleveId, MultipartFile fichier) throws IOException {
        if (!eleves.existsById(eleveId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Élève introuvable");
        }
        if (fichier == null || fichier.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier vide");
        }
        if (fichier.getSize() > tailleMax) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Photo trop volumineuse (1 Mo au plus)");
        }
        byte[] contenu = fichier.getBytes();
        String type = typePhoto(contenu);
        if (type == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seules les photos JPEG et PNG sont acceptées");
        }
        PhotoEleve p = photos.findById(eleveId).orElseGet(PhotoEleve::new);
        p.setEleveId(eleveId);
        p.setTypeMime(type);
        p.setTaille(contenu.length);
        p.setContenu(contenu);
        p.setUpdatedAt(Instant.now(clock));
        p.setAjouteeParId(perimetre.utilisateurCourant() == null ? null : perimetre.utilisateurCourant().getId());
        photos.save(p);
        audit.enregistrer("PHOTO_ELEVE_ENREGISTREE", "Eleve", eleveId, Map.of("taille", contenu.length));
    }

    @Transactional(readOnly = true)
    public PhotoEleve lire(Long eleveId) {
        return photos.findById(eleveId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune photo"));
    }

    /** Taille de la photo en octets, ou null s'il n'y en a pas. */
    @Transactional(readOnly = true)
    public Long taille(Long eleveId) {
        return photos.tailleDe(eleveId).orElse(null);
    }

    public void supprimer(Long eleveId) {
        if (photos.existsById(eleveId)) {
            photos.deleteById(eleveId);
            audit.enregistrer("PHOTO_ELEVE_SUPPRIMEE", "Eleve", eleveId, Map.of());
        }
    }
}
