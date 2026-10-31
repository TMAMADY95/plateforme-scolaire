package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.CandidatExamen;
import com.plateforme.scolaire.domain.ExamenNational;
import com.plateforme.scolaire.domain.PieceCandidature;
import com.plateforme.scolaire.repository.CandidatExamenRepository;
import com.plateforme.scolaire.repository.ExamenNationalRepository;
import com.plateforme.scolaire.repository.NoteRepository;
import com.plateforme.scolaire.repository.PieceCandidatureRepository;
import com.plateforme.scolaire.security.PerimetreService;
import com.plateforme.scolaire.web.dto.DossiersCandidaturesDto;
import com.plateforme.scolaire.web.dto.PieceCandidatureRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Dossier de candidature (règlement général de 2022) : demande manuscrite adressée au ministre par voie hiérarchique, extrait
 * d'acte de naissance ou jugement supplétif, copie légalisée du diplôme précédent lorsqu'il est exigé (BEPC : le CEE ; BAC : le
 * BEPC) et attestation d'inscription de l'établissement d'origine. L'établissement enregistre les pièces reçues ; le dossier
 * est complet quand toutes les pièces requises sont là.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DossierCandidatureService {

    static final Map<String, String> PIECES = new LinkedHashMap<>();

    static {
        PIECES.put("DEMANDE", "Demande manuscrite adressée au ministre");
        PIECES.put("ACTE_NAISSANCE", "Extrait d'acte de naissance ou jugement supplétif");
        PIECES.put("DIPLOME", "Copie légalisée du diplôme précédent");
        PIECES.put("ATTESTATION", "Attestation d'inscription de l'établissement d'origine");
    }

    private final ExamenNationalRepository examens;
    private final CandidatExamenRepository candidats;
    private final PieceCandidatureRepository pieces;
    private final NoteRepository notes;
    private final PerimetreService perimetre;
    private final AuditService audit;
    private final Clock clock;

    /** Le diplôme précédent n'est exigé que pour les examens régionaux (BEPC) et nationaux (BAC), pas pour le CEE. */
    static boolean diplomeRequis(String niveauOrganisation) {
        return niveauOrganisation != null && Set.of("NATIONAL", "REGION").contains(niveauOrganisation.toUpperCase());
    }

    static boolean requise(String type, String niveauOrganisation) {
        return requise(type, niveauOrganisation, false);
    }

    /** Un candidat libre n'a pas d'établissement d'origine : l'attestation d'inscription ne lui est pas demandée. */
    static boolean requise(String type, String niveauOrganisation, boolean libre) {
        if ("ATTESTATION".equals(type) && libre) {
            return false;
        }
        return !"DIPLOME".equals(type) || diplomeRequis(niveauOrganisation);
    }

    /** Libellés des pièces requises qui ne sont pas encore fournies. */
    static List<String> manques(String niveauOrganisation, Set<String> fournies) {
        return manques(niveauOrganisation, fournies, false);
    }

    static List<String> manques(String niveauOrganisation, Set<String> fournies, boolean libre) {
        List<String> manques = new ArrayList<>();
        PIECES.forEach((type, libelle) -> {
            if (requise(type, niveauOrganisation, libre) && !fournies.contains(type)) {
                manques.add(libelle);
            }
        });
        return manques;
    }

    @Transactional(readOnly = true)
    public DossiersCandidaturesDto dossiers(Long etablissementId, Long examenId) {
        ExamenNational examen = examen(examenId);
        List<CandidatExamen> liste = candidats.deLEtablissement(examenId, etablissementId);
        return dto(examen, liste);
    }

    public DossiersCandidaturesDto marquer(Long etablissementId, Long examenId, Long candidatId, String type, PieceCandidatureRequest r) {
        ExamenNational examen = examen(examenId);
        if (!PIECES.containsKey(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pièce inconnue : " + type);
        }
        CandidatExamen c = candidats.findById(candidatId)
                .filter(x -> x.getExamen().getId().equals(examenId) && x.getEtablissement().getId().equals(etablissementId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidat introuvable dans cet établissement"));
        if (!perimetre.peutGererEtablissement(etablissementId)) {
            throw new AccessDeniedException("Candidat hors de votre établissement");
        }
        if ("PROCLAME".equals(examen.getStatutResultats())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Les résultats sont proclamés : les dossiers sont clos");
        }
        var existante = pieces.findByCandidatIdAndType(candidatId, type);
        if (r.fournie()) {
            PieceCandidature p = existante.orElseGet(() -> {
                PieceCandidature n = new PieceCandidature();
                n.setCandidat(c);
                n.setType(type);
                n.setFournieLe(LocalDate.now(clock));
                n.setCreatedAt(Instant.now(clock));
                return n;
            });
            p.setReference(r.reference() == null || r.reference().isBlank() ? null : r.reference().trim());
            pieces.save(p);
        } else {
            existante.ifPresent(pieces::delete);
            pieces.flush();
        }
        audit.enregistrer("PIECE_CANDIDATURE", "ExamenNational", examenId, Map.of("candidat", candidatId, "piece", type, "fournie", r.fournie()));
        return dto(examen, candidats.deLEtablissement(examenId, etablissementId));
    }

    private DossiersCandidaturesDto dto(ExamenNational examen, List<CandidatExamen> liste) {
        String niveau = examen.getNiveauOrganisation();
        List<Long> ids = liste.stream().map(CandidatExamen::getId).toList();
        Map<Long, List<PieceCandidature>> parCandidat = ids.isEmpty() ? Map.of()
                : pieces.findByCandidatIdIn(ids).stream().collect(Collectors.groupingBy(p -> p.getCandidat().getId()));
        Map<Long, Integer> manquees = new HashMap<>();
        if (!liste.isEmpty()) {
            notes.evaluationsManqueesParEleve(examen.getAnneeScolaire().getId(), liste.stream().map(c -> c.getEleve().getId()).toList())
                    .forEach(l -> manquees.put((Long) l[0], ((Number) l[1]).intValue()));
        }
        List<DossiersCandidaturesDto.Candidat> candidatsDto = liste.stream().map(c -> {
            Map<String, DossiersCandidaturesDto.Piece> fournies = new LinkedHashMap<>();
            parCandidat.getOrDefault(c.getId(), List.of()).forEach(p -> fournies.put(p.getType(), new DossiersCandidaturesDto.Piece(p.getReference(), p.getFournieLe())));
            List<String> manques = manques(niveau, fournies.keySet(), c.isLibre());
            return new DossiersCandidaturesDto.Candidat(c.getId(), (c.getEleve().getPrenom() + " " + c.getEleve().getNom()).trim(), c.getNumero(),
                    c.getClasse() == null ? null : c.getClasse().getNom(), fournies, manques.isEmpty(), manques,
                    c.isLibre() ? 0 : manquees.getOrDefault(c.getEleve().getId(), 0), c.isLibre());
        }).toList();
        List<DossiersCandidaturesDto.TypePiece> types = PIECES.entrySet().stream()
                .map(e -> new DossiersCandidaturesDto.TypePiece(e.getKey(), e.getValue(), requise(e.getKey(), niveau))).toList();
        return new DossiersCandidaturesDto(examen.getId(), examen.getLibelle(), types, candidatsDto,
                (int) candidatsDto.stream().filter(DossiersCandidaturesDto.Candidat::complet).count());
    }

    private ExamenNational examen(Long id) {
        return examens.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Examen introuvable"));
    }
}
