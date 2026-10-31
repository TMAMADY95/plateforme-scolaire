package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Inspection;
import com.plateforme.scolaire.domain.PieceJointe;
import com.plateforme.scolaire.repository.InspectionRepository;
import com.plateforme.scolaire.repository.PieceJointeRepository;
import com.plateforme.scolaire.repository.UtilisateurRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.web.dto.PieceJointeDto;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Pièces jointes d'une inspection : PDF et photos, 5 Mo au plus, 10 par inspection. */
@Service
@Transactional
public class PiecesJointesService {

    static final Set<String> TYPES = Set.of("application/pdf", "image/jpeg", "image/png");
    static final int MAX_PAR_INSPECTION = 10;

    private final PieceJointeRepository pieces;
    private final InspectionRepository inspections;
    private final UtilisateurRepository utilisateurs;
    private final PerimetreService perimetre;
    private final AuditService audit;
    private final Clock clock;
    private final long tailleMax;

    public PiecesJointesService(PieceJointeRepository pieces, InspectionRepository inspections,
                                UtilisateurRepository utilisateurs, PerimetreService perimetre, AuditService audit,
                                Clock clock,
                                @Value("${app.piece-jointe-max:5MB}") org.springframework.util.unit.DataSize tailleMax) {
        this.pieces = pieces;
        this.inspections = inspections;
        this.utilisateurs = utilisateurs;
        this.perimetre = perimetre;
        this.audit = audit;
        this.clock = clock;
        this.tailleMax = tailleMax.toBytes();
    }

    @Transactional(readOnly = true)
    public List<PieceJointeDto> lister(Long inspectionId) {
        Inspection i = inspection(inspectionId);
        verifierLecture(i);
        return pieces.listerDeLInspection(inspectionId);
    }

    public PieceJointeDto ajouter(Long inspectionId, MultipartFile fichier) throws IOException {
        Inspection i = inspection(inspectionId);
        if (!perimetre.peutAccederEtablissement(i.getEtablissement().getId())) {
            throw new AccessDeniedException("Inspection hors de votre périmètre");
        }
        if (fichier == null || fichier.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier vide");
        }
        if (fichier.getSize() > tailleMax) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Fichier trop volumineux (5 Mo au plus)");
        }
        byte[] contenu = fichier.getBytes();
        String type = typeReel(contenu);
        if (type == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seuls les PDF, JPEG et PNG sont acceptés");
        }
        if (pieces.countByInspectionId(inspectionId) >= MAX_PAR_INSPECTION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "10 pièces jointes au plus par inspection");
        }
        PieceJointe p = new PieceJointe();
        p.setInspection(i);
        p.setAjouteePar(utilisateurs.getReferenceById(perimetre.utilisateurCourant().getId()));
        p.setNom(nomSur(fichier.getOriginalFilename()));
        p.setTypeMime(type);
        p.setTaille(contenu.length);
        p.setContenu(contenu);
        p.setCreatedAt(Instant.now(clock));
        PieceJointe e = pieces.save(p);
        audit.enregistrer("PIECE_JOINTE_AJOUTEE", "PieceJointe", e.getId(),
                Map.of("inspection", inspectionId, "nom", e.getNom()));
        return new PieceJointeDto(e.getId(), e.getNom(), e.getTypeMime(), e.getTaille(), e.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public PieceJointe lire(Long pieceId) {
        PieceJointe p = pieces.findById(pieceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pièce jointe introuvable"));
        verifierLecture(p.getInspection());
        return p;
    }

    public void supprimer(Long pieceId) {
        PieceJointe p = pieces.findById(pieceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pièce jointe introuvable"));
        if (!perimetre.peutAccederEtablissement(p.getInspection().getEtablissement().getId())) {
            throw new AccessDeniedException("Inspection hors de votre périmètre");
        }
        pieces.delete(p);
        audit.enregistrer("PIECE_JOINTE_SUPPRIMEE", "PieceJointe", pieceId, Map.of("nom", p.getNom()));
    }

    private Inspection inspection(Long id) {
        return inspections.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inspection introuvable"));
    }

    private void verifierLecture(Inspection i) {
        Long etab = i.getEtablissement().getId();
        if (!perimetre.peutAccederEtablissement(etab) && !perimetre.peutGererEtablissement(etab)) {
            throw new AccessDeniedException("Inspection hors de votre périmètre");
        }
    }

    /** Le type est déduit du contenu (signature du fichier), jamais de ce que le client déclare. */
    static String typeReel(byte[] c) {
        if (c.length >= 5 && c[0] == '%' && c[1] == 'P' && c[2] == 'D' && c[3] == 'F' && c[4] == '-') {
            return "application/pdf";
        }
        if (c.length >= 3 && (c[0] & 0xFF) == 0xFF && (c[1] & 0xFF) == 0xD8 && (c[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (c.length >= 8 && (c[0] & 0xFF) == 0x89 && c[1] == 'P' && c[2] == 'N' && c[3] == 'G') {
            return "image/png";
        }
        return null;
    }

    static String nomSur(String original) {
        String nom = original == null ? "" : original.replace('\\', '/');
        nom = nom.substring(nom.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}\"<>:|?*]", "_").trim();
        if (nom.isEmpty()) {
            nom = "piece-jointe";
        }
        return nom.length() > 200 ? nom.substring(nom.length() - 200) : nom;
    }
}
