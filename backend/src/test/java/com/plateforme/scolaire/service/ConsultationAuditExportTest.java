package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.repository.EleveRepository;
import com.plateforme.scolaire.repository.JournalAuditRepository;
import com.plateforme.scolaire.web.dto.JournalAuditDto;
import com.plateforme.scolaire.web.dto.PageAuditDto;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ConsultationAuditExportTest {

    @Test
    void lExportNeutraliseLesFormulesEtEchappeLesGuillemets() {
        ConsultationAuditService service = Mockito.spy(new ConsultationAuditService(
                mock(JournalAuditRepository.class), mock(ResolveurContexteAudit.class), mock(EleveRepository.class)));
        JournalAuditDto ligne = new JournalAuditDto(1L, Instant.parse("2026-10-30T07:00:00Z"), "a@b.org", "=Pirate \"X\"",
                "CREATION", "résumé", "Plan", 3L, List.of(), null, "1.2.3.4");
        Mockito.doReturn(new PageAuditDto(List.of(ligne), 1, 0, 100)).when(service).rechercher(any(), any(), any(), Mockito.anyInt(), Mockito.anyInt());

        String csv = service.exporterCsv("", "", "");

        assertThat(csv).startsWith("﻿Date;Utilisateur");
        assertThat(csv).contains("\"'=Pirate \"\"X\"\"\"");
    }
}
