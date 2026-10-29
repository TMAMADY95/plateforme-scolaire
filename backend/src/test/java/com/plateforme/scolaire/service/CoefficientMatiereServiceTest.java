package com.plateforme.scolaire.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateforme.scolaire.domain.AffectationEnseignant;
import com.plateforme.scolaire.domain.Classe;
import com.plateforme.scolaire.domain.CoefficientMatiere;
import com.plateforme.scolaire.domain.Matiere;
import com.plateforme.scolaire.repository.AffectationEnseignantRepository;
import com.plateforme.scolaire.repository.ClasseRepository;
import com.plateforme.scolaire.repository.CoefficientMatiereRepository;
import com.plateforme.scolaire.repository.MatiereRepository;
import com.plateforme.scolaire.web.dto.CoefficientsRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CoefficientMatiereServiceTest {

    private static final long CLASSE = 3L;

    private final CoefficientMatiereRepository coefficients = mock(CoefficientMatiereRepository.class);
    private final AffectationEnseignantRepository affectations = mock(AffectationEnseignantRepository.class);
    private final ClasseRepository classes = mock(ClasseRepository.class);
    private final MatiereRepository matieres = mock(MatiereRepository.class);
    private final AuditService audit = mock(AuditService.class);
    private final CoefficientMatiereService service =
            new CoefficientMatiereService(coefficients, affectations, classes, matieres, audit);

    private final Matiere maths = matiere(1L, "MATHS", "Mathématiques");
    private final Matiere francais = matiere(2L, "FR", "Français");
    private final List<CoefficientMatiere> enregistres = new ArrayList<>();

    private static Matiere matiere(long id, String code, String libelle) {
        Matiere m = new Matiere();
        m.setId(id);
        m.setCode(code);
        m.setLibelle(libelle);
        return m;
    }

    @BeforeEach
    void preparer() {
        Classe classe = new Classe();
        classe.setId(CLASSE);
        when(classes.findById(CLASSE)).thenReturn(Optional.of(classe));
        when(matieres.findById(1L)).thenReturn(Optional.of(maths));
        when(matieres.findById(2L)).thenReturn(Optional.of(francais));
        AffectationEnseignant a = new AffectationEnseignant();
        a.setClasse(classe);
        a.setMatiere(francais);
        when(affectations.findByClasseId(CLASSE)).thenReturn(List.of(a));
        when(coefficients.findByClasseId(CLASSE)).thenAnswer(i -> List.copyOf(enregistres));
        when(coefficients.save(any(CoefficientMatiere.class))).thenAnswer(i -> {
            CoefficientMatiere c = i.getArgument(0);
            if (!enregistres.contains(c)) {
                enregistres.add(c);
            }
            return c;
        });
    }

    @Test
    void sansCoefficientDefiniLaMatiereEnseigneeVautUn() {
        var lignes = service.lister(CLASSE);

        assertThat(lignes).hasSize(1);
        assertThat(lignes.get(0).libelle()).isEqualTo("Français");
        assertThat(lignes.get(0).coefficient()).isEqualByComparingTo("1");
        assertThat(lignes.get(0).defini()).isFalse();
    }

    @Test
    void enregistrerCreeOuModifieEtJournaliseLesChangements() {
        var lignes = service.enregistrer(CLASSE, new CoefficientsRequest(List.of(
                new CoefficientsRequest.Ligne(1L, new BigDecimal("4")),
                new CoefficientsRequest.Ligne(2L, new BigDecimal("2.5")))));

        assertThat(lignes).hasSize(2);
        assertThat(service.coefficientsDe(CLASSE).get(1L)).isEqualByComparingTo("4");
        verify(audit).enregistrer(eq("MODIFICATION_COEFFICIENTS"), eq("classe"), eq(CLASSE), any());
    }

    @Test
    void rienNEstJournaliseSiRienNeChange() {
        service.enregistrer(CLASSE, new CoefficientsRequest(List.of(new CoefficientsRequest.Ligne(1L, new BigDecimal("4")))));
        org.mockito.Mockito.clearInvocations(audit);

        service.enregistrer(CLASSE, new CoefficientsRequest(List.of(new CoefficientsRequest.Ligne(1L, new BigDecimal("4.00")))));

        verify(audit, never()).enregistrer(any(), any(), any(), any());
    }

    @Test
    void uneMatiereEnDoubleOuInconnueEstRefusee() {
        assertThatThrownBy(() -> service.enregistrer(CLASSE, new CoefficientsRequest(List.of(
                new CoefficientsRequest.Ligne(1L, BigDecimal.ONE), new CoefficientsRequest.Ligne(1L, BigDecimal.TWO)))))
                .hasMessageContaining("double");
        assertThatThrownBy(() -> service.enregistrer(CLASSE,
                new CoefficientsRequest(List.of(new CoefficientsRequest.Ligne(99L, BigDecimal.ONE)))))
                .hasMessageContaining("introuvable");
    }
}
