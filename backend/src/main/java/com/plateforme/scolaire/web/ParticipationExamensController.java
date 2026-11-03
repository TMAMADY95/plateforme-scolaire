package com.plateforme.scolaire.web;

import com.plateforme.scolaire.service.ParticipationExamensService;
import com.plateforme.scolaire.web.dto.ParticipationDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Participation aux épreuves (d'après les rapports de surveillance) : DGECS et ministère (consultation). */
@RestController
@RequiredArgsConstructor
public class ParticipationExamensController {

    private final ParticipationExamensService service;

    @GetMapping("/api/dgecs/participation")
    @PreAuthorize("hasAnyRole('MINISTERE','DGECS')")
    public ParticipationDto participation(@RequestParam Long anneeScolaireId) {
        return service.participation(anneeScolaireId);
    }

    @GetMapping("/api/dgecs/participation/epreuves/{epreuveId}/centres")
    @PreAuthorize("hasAnyRole('MINISTERE','DGECS')")
    public List<ParticipationDto.Centre> centres(@PathVariable Long epreuveId) {
        return service.centres(epreuveId);
    }
}
