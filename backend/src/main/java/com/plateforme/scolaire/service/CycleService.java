package com.plateforme.scolaire.service;

import com.plateforme.scolaire.domain.CycleScolaire;
import com.plateforme.scolaire.repository.CycleScolaireRepository;
import com.plateforme.scolaire.web.dto.CycleDto;
import com.plateforme.scolaire.web.dto.CycleRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Référentiel des cycles d'enseignement : primaire, collège, lycée… (gérés par le ministère). */
@Service
@RequiredArgsConstructor
@Transactional
public class CycleService {

    private final CycleScolaireRepository cycleRepository;

    @Transactional(readOnly = true)
    public List<CycleDto> lister() {
        return cycleRepository.findAllByOrderByOrdre().stream().map(CycleDto::de).toList();
    }

    public CycleDto creer(CycleRequest r) {
        String code = normaliser(r.code());
        if (cycleRepository.findByCode(code).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un cycle porte déjà ce code");
        }
        return CycleDto.de(cycleRepository.save(appliquer(new CycleScolaire(), code, r)));
    }

    public CycleDto modifier(Long id, CycleRequest r) {
        CycleScolaire cycle = cycleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cycle introuvable"));
        String code = normaliser(r.code());
        cycleRepository.findByCode(code).filter(autre -> !autre.getId().equals(id)).ifPresent(autre -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un cycle porte déjà ce code");
        });
        return CycleDto.de(appliquer(cycle, code, r));
    }

    private static String normaliser(String code) {
        return code.trim().toUpperCase();
    }

    private static CycleScolaire appliquer(CycleScolaire c, String code, CycleRequest r) {
        c.setCode(code);
        c.setLibelle(r.libelle().trim());
        c.setOrdre(r.ordre());
        return c;
    }
}
