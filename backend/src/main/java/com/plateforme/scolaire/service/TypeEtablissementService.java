package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.TypeEtablissement;
import com.plateforme.scolaire.repository.EtablissementRepository;
import com.plateforme.scolaire.repository.TypeEtablissementRepository;
import com.plateforme.scolaire.web.dto.TypeEtablissementDto;
import com.plateforme.scolaire.web.dto.TypeEtablissementRequest;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Les types d'établissement du référentiel, modifiables par le ministère. */
@Service
@RequiredArgsConstructor
@Transactional
public class TypeEtablissementService {

    private final TypeEtablissementRepository types;
    private final EtablissementRepository etablissements;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<TypeEtablissementDto> lister() {
        return types.findAllByOrderByLibelle().stream().map(TypeEtablissementDto::de).toList();
    }

    public TypeEtablissementDto creer(TypeEtablissementRequest r) {
        String code = code(r);
        if (types.existsByCodeIgnoreCase(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce code existe déjà");
        }
        TypeEtablissement t = new TypeEtablissement();
        t.setCode(code);
        t.setLibelle(r.libelle().trim());
        TypeEtablissement enregistre = types.save(t);
        audit.enregistrer("TYPE_ETABLISSEMENT_CREE", "TypeEtablissement", enregistre.getId(), Map.of("code", code));
        return TypeEtablissementDto.de(enregistre);
    }

    public TypeEtablissementDto modifier(Long id, TypeEtablissementRequest r) {
        TypeEtablissement t = types.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Type introuvable"));
        String code = code(r);
        if (!t.getCode().equalsIgnoreCase(code) && types.existsByCodeIgnoreCase(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce code existe déjà");
        }
        t.setCode(code);
        t.setLibelle(r.libelle().trim());
        audit.enregistrer("TYPE_ETABLISSEMENT_MODIFIE", "TypeEtablissement", id, Map.of("code", code));
        return TypeEtablissementDto.de(t);
    }

    public void supprimer(Long id) {
        TypeEtablissement t = types.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Type introuvable"));
        if (etablissements.existsByTypeEtablissementId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce type est utilisé par des établissements");
        }
        types.delete(t);
        audit.enregistrer("TYPE_ETABLISSEMENT_SUPPRIME", "TypeEtablissement", id, Map.of("code", t.getCode()));
    }

    private static String code(TypeEtablissementRequest r) {
        return r.code().trim().toUpperCase();
    }
}
