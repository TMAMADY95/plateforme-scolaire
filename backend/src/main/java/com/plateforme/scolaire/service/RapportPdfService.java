package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.AnneeScolaire;
import com.plateforme.scolaire.domain.ZoneGeographique;
import com.plateforme.scolaire.repository.AnneeScolaireRepository;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.ZoneGeographiqueRepository;
import com.plateforme.scolaire.service.GenerateurPdf.DonneesRapport;
import com.plateforme.scolaire.service.GenerateurPdf.SectionRapport;
import com.plateforme.scolaire.web.dto.ConformiteDto;
import com.plateforme.scolaire.web.dto.LigneEtablissementStatDto;
import com.plateforme.scolaire.web.dto.LigneZoneStatDto;
import com.plateforme.scolaire.web.dto.StatistiquesDto;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Rapports officiels en PDF : national (ou d'une zone), régional et d'un établissement. */
@Service
@Transactional(readOnly = true)
public class RapportPdfService {

    private final StatistiquesService statistiques;
    private final ConformiteService conformite;
    private final AnneeScolaireRepository anneeRepository;
    private final ZoneGeographiqueRepository zoneRepository;
    private final EtablissementRepository etablissementRepository;
    private final GenerateurPdf generateur;
    private final Clock clock;
    private final String entete;
    private final String sousEntete;

    public RapportPdfService(StatistiquesService statistiques, ConformiteService conformite,
                             AnneeScolaireRepository anneeRepository, ZoneGeographiqueRepository zoneRepository,
                             EtablissementRepository etablissementRepository, GenerateurPdf generateur, Clock clock,
                             @Value("${app.rapports.entete:Ministère de l'Éducation}") String entete,
                             @Value("${app.rapports.sous-entete:République de Guinée}") String sousEntete) {
        this.statistiques = statistiques;
        this.conformite = conformite;
        this.anneeRepository = anneeRepository;
        this.zoneRepository = zoneRepository;
        this.etablissementRepository = etablissementRepository;
        this.generateur = generateur;
        this.clock = clock;
        this.entete = entete;
        this.sousEntete = sousEntete;
    }

    /** Ministère : tout le pays, ou une zone et ses sous-zones. */
    public byte[] nationale(Long anneeId, Long zoneId) {
        String perimetre = "Tout le pays";
        if (zoneId != null) {
            ZoneGeographique z = zoneRepository.findById(zoneId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zone introuvable"));
            perimetre = z.getNom() + " (" + z.getTypeZone() + ")";
        }
        StatistiquesDto s = zoneId == null ? statistiques.nationales(anneeId) : statistiques.zone(zoneId, anneeId);
        List<String[]> chiffres = chiffresReseau(s);
        ConformiteDto c = conformite.nationale(zoneId, null, null, null);
        chiffres.add(new String[] {"Établissements autorisés", GenerateurPdf.nombre(c.indicateurs().autorises())});
        chiffres.add(new String[] {"Établissements non autorisés", GenerateurPdf.nombre(c.indicateurs().nonAutorises())});
        chiffres.add(new String[] {"Non conformes", GenerateurPdf.nombre(c.indicateurs().nonConformes())});
        chiffres.add(new String[] {"Inspectés (moins d'un an)", GenerateurPdf.nombre(c.indicateurs().inspectes())});
        chiffres.add(new String[] {"Avec un problème de sécurité", GenerateurPdf.nombre(c.indicateurs().avecProblemeSecurite())});
        chiffres.add(new String[] {"Données non transmises", GenerateurPdf.nombre(c.indicateurs().sansDonnees())});
        chiffres.add(new String[] {"Inspections urgentes", GenerateurPdf.nombre(c.indicateurs().urgents())});

        List<SectionRapport> sections = new ArrayList<>();
        sections.add(sectionRepartition("Élèves par cycle", "Cycle", s.effectifsParCycle()));
        sections.add(sectionRepartition("Élèves par niveau", "Niveau", s.effectifsParNiveau()));
        if (zoneId == null) {
            sections.add(sectionZones(statistiques.lignesParZone(anneeId)));
        }
        sections.add(sectionEtablissements(statistiques.lignesNationales(anneeId, zoneId)));
        return produire("Rapport sur le réseau scolaire", perimetre, anneeId, chiffres, sections);
    }

    /** Direction régionale : ses zones. */
    public byte[] regionale(Long anneeId) {
        StatistiquesDto s = statistiques.regionales(anneeId);
        List<SectionRapport> sections = new ArrayList<>();
        sections.add(sectionRepartition("Élèves par cycle", "Cycle", s.effectifsParCycle()));
        sections.add(sectionRepartition("Élèves par niveau", "Niveau", s.effectifsParNiveau()));
        sections.add(sectionEtablissements(statistiques.lignesRegionales(anneeId)));
        return produire("Rapport de la direction régionale", "Établissements de la direction régionale", anneeId,
                chiffresReseau(s), sections);
    }

    /** Un établissement (l'accès est contrôlé par le contrôleur). */
    public byte[] etablissement(Long etablissementId, Long anneeId) {
        var e = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable"));
        StatistiquesDto s = statistiques.etablissement(etablissementId, anneeId);
        List<String[]> chiffres = new ArrayList<>();
        chiffres.add(new String[] {"Élèves inscrits", GenerateurPdf.nombre(s.elevesInscrits())});
        chiffres.add(new String[] {"Enseignants", GenerateurPdf.nombre(s.enseignants())});
        chiffres.add(new String[] {"Personnel de direction", GenerateurPdf.nombre(s.personnelDirection())});
        chiffres.add(new String[] {"Classes", GenerateurPdf.nombre(s.classes())});
        List<SectionRapport> sections = List.of(
                sectionRepartition("Élèves par cycle", "Cycle", s.effectifsParCycle()),
                sectionRepartition("Élèves par niveau", "Niveau", s.effectifsParNiveau()),
                sectionRepartition("Filles et garçons", "Sexe", s.effectifsParSexe()));
        return produire("Rapport de l'établissement", e.getNom() + " (" + e.getCodeOfficiel() + ")", anneeId, chiffres, sections);
    }

    private byte[] produire(String titre, String perimetre, Long anneeId, List<String[]> chiffres, List<SectionRapport> sections) {
        String annee = anneeRepository.findById(anneeId).map(AnneeScolaire::getLibelle)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Année scolaire introuvable"));
        String edite = LocalDate.now(clock).format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRANCE));
        return generateur.rapport(new DonneesRapport(entete, sousEntete, titre, perimetre, annee, edite, chiffres, sections));
    }

    private static List<String[]> chiffresReseau(StatistiquesDto s) {
        List<String[]> c = new ArrayList<>();
        c.add(new String[] {"Établissements", GenerateurPdf.nombre(s.etablissements())});
        c.add(new String[] {"Élèves inscrits", GenerateurPdf.nombre(s.elevesInscrits())});
        c.add(new String[] {"Enseignants", GenerateurPdf.nombre(s.enseignants())});
        c.add(new String[] {"Personnel de direction", GenerateurPdf.nombre(s.personnelDirection())});
        c.add(new String[] {"Classes", GenerateurPdf.nombre(s.classes())});
        return c;
    }

    private static SectionRapport sectionRepartition(String titre, String colonne, List<StatistiquesDto.Repartition> r) {
        return new SectionRapport(titre, List.of(colonne, "Élèves"),
                r.stream().map(x -> List.of(x.libelle(), GenerateurPdf.nombre(x.valeur()))).toList());
    }

    private static SectionRapport sectionZones(List<LigneZoneStatDto> zones) {
        return new SectionRapport("Effectifs par zone",
                List.of("Zone", "Type", "Établissements", "Élèves", "Enseignants", "Direction", "Classes"),
                zones.stream().map(z -> List.of(z.nom(), z.typeZone(), GenerateurPdf.nombre(z.etablissements()),
                        GenerateurPdf.nombre(z.eleves()), GenerateurPdf.nombre(z.enseignants()),
                        GenerateurPdf.nombre(z.personnelDirection()), GenerateurPdf.nombre(z.classes()))).toList());
    }

    private static SectionRapport sectionEtablissements(List<LigneEtablissementStatDto> l) {
        return new SectionRapport("Effectifs par établissement",
                List.of("Établissement", "Zone", "Secteur", "Élèves", "Enseignants", "Direction", "Classes"),
                l.stream().map(e -> List.of(e.nom() + " (" + e.codeOfficiel() + ")", e.zone() == null ? "—" : e.zone(),
                        "PRIVE".equals(e.secteur()) ? "Privé" : "Public", GenerateurPdf.nombre(e.eleves()),
                        GenerateurPdf.nombre(e.enseignants()), GenerateurPdf.nombre(e.personnelDirection()),
                        GenerateurPdf.nombre(e.classes()))).toList());
    }
}
