package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.CandidatExamen;
import com.plateforme.scolaire.domain.ExamenNational;
import com.plateforme.scolaire.repository.CandidatExamenRepository;
import com.plateforme.scolaire.repository.EpreuveExamenRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.ExamenNationalRepository;
import com.plateforme.scolaire.repository.ListeCandidatsRepository;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Cartes spéciales d'examen : après la validation de la liste des candidats, l'établissement imprime la carte de chacun
 * (identité, numéro de PV, centre, salle et table), à présenter à l'entrée du centre et à chaque épreuve.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartesExamenService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ExamenNationalRepository examens;
    private final EtablissementRepository etablissements;
    private final CandidatExamenRepository candidats;
    private final ListeCandidatsRepository listes;
    private final EpreuveExamenRepository epreuves;
    private final GenerateurCartesPdf pdf;

    /** Pur : seule une liste définitivement validée donne lieu à des cartes. */
    static boolean imprimable(String statutListe) {
        return "VALIDEE".equals(statutListe);
    }

    public byte[] cartes(Long etablissementId, Long examenId) {
        ExamenNational examen = examens.findById(examenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Examen introuvable"));
        var etab = etablissements.findById(etablissementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable"));
        String statut = listes.findByExamenIdAndEtablissementId(examenId, etablissementId).map(l -> l.getStatut()).orElse("BROUILLON");
        if (!imprimable(statut)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Les cartes s'impriment une fois la liste des candidats validée par la DGECS");
        }
        List<GenerateurCartesPdf.Carte> cartes = candidats.deLEtablissement(examenId, etablissementId).stream().map(this::carte).toList();
        String periode = examen.getDateDebut() == null ? null
                : DATE.format(examen.getDateDebut()) + (examen.getDateFin() == null ? "" : " au " + DATE.format(examen.getDateFin()));
        if (periode == null) {
            periode = epreuves.findByExamenIdOrderByDateEpreuveAscHeureDebutAsc(examenId).stream().findFirst()
                    .map(e -> "à partir du " + DATE.format(e.getDateEpreuve())).orElse(null);
        }
        return pdf.cartes(new GenerateurCartesPdf.DonneesCartes(etab.getNom(), examen.getLibelle(),
                examen.getAnneeScolaire().getLibelle(), periode, cartes));
    }

    private GenerateurCartesPdf.Carte carte(CandidatExamen c) {
        var e = c.getEleve();
        return new GenerateurCartesPdf.Carte((e.getPrenom() + " " + e.getNom()).trim(), e.getMatricule(),
                e.getDateNaissance() == null ? null : DATE.format(e.getDateNaissance()),
                c.getClasse() == null ? null : c.getClasse().getNom(), c.getNumero(), c.getNumeroPv(),
                c.getCentre() == null ? null : c.getCentre().getNom(),
                c.getSalle() == null ? null : String.valueOf(c.getSalle()),
                c.getTableNumero() == null ? null : String.valueOf(c.getTableNumero()));
    }
}
