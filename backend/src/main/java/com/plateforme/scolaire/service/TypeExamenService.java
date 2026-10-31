package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.TypeExamen;
import com.plateforme.scolaire.repository.CycleScolaireRepository;
import com.plateforme.scolaire.repository.NiveauRepository;
import com.plateforme.scolaire.repository.TypeExamenRepository;
import com.plateforme.scolaire.web.dto.TypeExamenDto;
import com.plateforme.scolaire.web.dto.TypeExamenRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Les modèles d'examens (CEE, BEPC, BAC…) et leur niveau d'organisation, modifiables par le ministère. */
@Service
@RequiredArgsConstructor
@Transactional
public class TypeExamenService {

    private final TypeExamenRepository types;
    private final NiveauRepository niveaux;
    private final CycleScolaireRepository cycles;
    private final ExamenService examens;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<TypeExamenDto> lister() {
        return types.findAllByOrderByCode().stream().map(TypeExamenDto::de).toList();
    }

    public TypeExamenDto creer(TypeExamenRequest r) {
        if (types.existsByCodeIgnoreCase(r.code().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce code existe déjà");
        }
        TypeExamen t = new TypeExamen();
        appliquer(t, r);
        TypeExamen enregistre = types.save(t);
        audit.enregistrer("TYPE_EXAMEN_CREE", "TypeExamen", enregistre.getId(), java.util.Map.of("code", enregistre.getCode()));
        return TypeExamenDto.de(enregistre);
    }

    public TypeExamenDto modifier(Long id, TypeExamenRequest r) {
        TypeExamen t = types.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modèle introuvable"));
        if (!t.getCode().equalsIgnoreCase(r.code().trim()) && types.existsByCodeIgnoreCase(r.code().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce code existe déjà");
        }
        appliquer(t, r);
        audit.enregistrer("TYPE_EXAMEN_MODIFIE", "TypeExamen", t.getId(), java.util.Map.of("code", t.getCode()));
        return TypeExamenDto.de(t);
    }

    public void supprimer(Long id) {
        TypeExamen t = types.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modèle introuvable"));
        types.delete(t);
        audit.enregistrer("TYPE_EXAMEN_SUPPRIME", "TypeExamen", id, java.util.Map.of("code", t.getCode()));
    }

    private void appliquer(TypeExamen t, TypeExamenRequest r) {
        String organisation = r.niveauOrganisation().trim();
        String retenu = examens.niveauxOrganisation().stream().filter(n -> n.equalsIgnoreCase(organisation)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Niveau d'organisation inconnu : NATIONAL ou un type de zone existant"));
        t.setCode(r.code().trim().toUpperCase());
        t.setLibelle(r.libelle().trim());
        t.setNiveauOrganisation(retenu);
        t.setNiveau(r.niveauId() == null ? null : niveaux.findById(r.niveauId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niveau inconnu")));
        t.setCycle(r.cycleId() == null ? null : cycles.findById(r.cycleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cycle inconnu")));
    }
}
