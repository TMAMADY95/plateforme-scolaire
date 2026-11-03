package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.Eleve;
import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.web.dto.EtatCivilRequest;
import com.plateforme.scolaire.web.dto.IdentiteNationaleDto;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Identifiant national de l'élève (INE) et pièce d'état civil. L'INE lui-même est généré par la base (V66). */
@Service
@RequiredArgsConstructor
@Transactional
public class IdentifiantNationalService {

    /** Longueur de l'INE : GN + 12 chiffres. */
    public static final int LONGUEUR_INF = 14;

    private final EleveRepository eleveRepository;

    /** Vrai si l'INE a le bon format et une clé modulo 97 correcte (détecte les fautes de frappe). */
    public static boolean ineValide(String ine) {
        if (ine == null || !ine.matches("GN\\d{12}")) {
            return false;
        }
        long corps = Long.parseLong(ine.substring(2, 12));
        int cle = Integer.parseInt(ine.substring(12));
        return cle == 97 - (int) ((corps * 100) % 97);
    }

    /** Normalise une saisie d'INE : espaces et tirets retirés, majuscules. */
    public static String normaliser(String saisi) {
        return saisi == null ? "" : saisi.replace(" ", "").replace("-", "").trim().toUpperCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public IdentiteNationaleDto parIne(String saisi) {
        String ine = normaliser(saisi);
        if (!ineValide(ine)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INE invalide (format ou clé de contrôle)");
        }
        return eleveRepository.findByIne(ine).map(IdentiteNationaleDto::de)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun élève avec cet INE"));
    }

    @Transactional(readOnly = true)
    public IdentiteNationaleDto identite(Long eleveId) {
        return IdentiteNationaleDto.de(charger(eleveId));
    }

    public IdentiteNationaleDto modifierEtatCivil(Long eleveId, EtatCivilRequest r) {
        Eleve eleve = charger(eleveId);
        String numero = r.numeroActeNaissance() == null ? null : r.numeroActeNaissance().trim();
        if ("AUCUN".equals(r.typeActeNaissance())) {
            numero = null;
        } else if (numero == null || numero.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le numéro de l'acte est obligatoire");
        }
        eleve.setTypeActeNaissance(r.typeActeNaissance());
        eleve.setNumeroActeNaissance(numero);
        return IdentiteNationaleDto.de(eleve);
    }

    /** Élèves sans acte de naissance : à régulariser avant l'inscription aux examens (CEE, BEPC, BAC). */
    @Transactional(readOnly = true)
    public List<IdentiteNationaleDto> sansActe(Long etablissementId) {
        return eleveRepository.sansActeNaissance(etablissementId).stream().map(IdentiteNationaleDto::de).toList();
    }

    private Eleve charger(Long id) {
        return eleveRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Élève introuvable"));
    }
}
