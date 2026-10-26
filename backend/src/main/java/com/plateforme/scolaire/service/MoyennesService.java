package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.Inscription;
import com.plateforme.scolaire.domain.Matiere;
import com.plateforme.scolaire.domain.Note;
import com.plateforme.scolaire.domain.Periode;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.InscriptionRepository;
import com.plateforme.scolaire.repository.NoteRepository;
import com.plateforme.scolaire.repository.PeriodeRepository;
import com.plateforme.scolaire.service.CalculMoyennes.NoteCalcul;
import com.plateforme.scolaire.service.CalculMoyennes.Resultat;
import com.plateforme.scolaire.web.dto.BulletinDto;
import com.plateforme.scolaire.web.dto.EleveDto;
import com.plateforme.scolaire.web.dto.LigneBulletinDto;
import com.plateforme.scolaire.web.dto.LigneResultatDto;
import com.plateforme.scolaire.web.dto.MatiereDto;
import com.plateforme.scolaire.web.dto.ResultatsClasseDto;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MoyennesService {

    private final NoteRepository noteRepository;
    private final ClasseRepository classeRepository;
    private final PeriodeRepository periodeRepository;
    private final InscriptionRepository inscriptionRepository;

    /** Moyennes de tous les élèves de la classe pour une période, avec le rang de chacun. */
    public ResultatsClasseDto resultatsClasse(Long classeId, Long periodeId) {
        Classe classe = classeRepository.findById(classeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe introuvable"));
        Periode periode = periodeRepository.findById(periodeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Période introuvable"));
        if (!periode.getAnneeScolaire().getId().equals(classe.getAnneeScolaire().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La période n'appartient pas à l'année scolaire de la classe");
        }

        List<Inscription> inscrits = inscriptionRepository
                .findByClasseIdAndStatutOrderByEleveNomAscElevePrenomAsc(classeId, "ACCEPTEE");
        List<Note> notes = noteRepository.findPourCalcul(classeId, periodeId);

        Map<Long, Matiere> matieres = new LinkedHashMap<>();
        notes.forEach(n -> matieres.put(n.getEvaluation().getMatiere().getId(), n.getEvaluation().getMatiere()));

        Resultat resultat = CalculMoyennes.calculer(notes.stream()
                .map(n -> new NoteCalcul(n.getInscription().getId(), n.getEvaluation().getMatiere().getId(),
                        n.getValeur(), n.getEvaluation().getBareme(), n.getEvaluation().getCoefficient()))
                .toList());

        List<LigneResultatDto> eleves = inscrits.stream().map(i -> {
            Map<Long, BigDecimal> moyennes = new LinkedHashMap<>();
            resultat.moyennesParEleve().getOrDefault(i.getId(), Map.of())
                    .forEach((matiereId, valeur) -> moyennes.put(matiereId, CalculMoyennes.arrondir(valeur)));
            return new LigneResultatDto(i.getId(), EleveDto.de(i.getEleve()), moyennes,
                    CalculMoyennes.arrondir(resultat.moyennesGenerales().get(i.getId())),
                    resultat.rangs().get(i.getId()));
        }).toList();

        Map<Long, BigDecimal> moyennesClasse = new LinkedHashMap<>();
        resultat.moyennesClasseParMatiere().forEach((id, valeur) -> moyennesClasse.put(id, CalculMoyennes.arrondir(valeur)));

        List<MatiereDto> listeMatieres = matieres.values().stream()
                .sorted(Comparator.comparing(Matiere::getLibelle))
                .map(MatiereDto::de)
                .toList();
        return new ResultatsClasseDto(classeId, periodeId, listeMatieres, eleves, moyennesClasse,
                CalculMoyennes.arrondir(resultat.moyenneGeneraleClasse()));
    }

    /** Bulletin d'un élève pour une période : ses moyennes, celles de la classe et son rang. */
    public BulletinDto bulletin(Long eleveId, Long periodeId) {
        Periode periode = periodeRepository.findById(periodeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Période introuvable"));
        Inscription inscription = inscriptionRepository.findByEleveIdAndAnneeScolaireIdAndStatutIn(
                        eleveId, periode.getAnneeScolaire().getId(), List.of("ACCEPTEE"))
                .filter(i -> i.getClasse() != null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune inscription acceptée pour cette année scolaire"));

        ResultatsClasseDto resultats = resultatsClasse(inscription.getClasse().getId(), periodeId);
        LigneResultatDto ligne = resultats.eleves().stream()
                .filter(l -> l.inscriptionId().equals(inscription.getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Élève introuvable dans la classe"));

        List<LigneBulletinDto> matieres = resultats.matieres().stream()
                .map(m -> new LigneBulletinDto(m.id(), m.libelle(), ligne.moyennesParMatiere().get(m.id()),
                        resultats.moyennesClasseParMatiere().get(m.id())))
                .toList();
        int effectif = (int) resultats.eleves().stream().filter(l -> l.rang() != null).count();

        return new BulletinDto(ligne.eleve(), inscription.getClasse().getId(), inscription.getClasse().getNom(),
                periodeId, periode.getLibelle(), matieres, ligne.moyenneGenerale(),
                resultats.moyenneGeneraleClasse(), ligne.rang(), effectif);
    }
}
