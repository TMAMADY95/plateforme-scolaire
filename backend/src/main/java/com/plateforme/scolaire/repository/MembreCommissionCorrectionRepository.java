package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.MembreCommissionCorrection;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembreCommissionCorrectionRepository extends JpaRepository<MembreCommissionCorrection, Long> {

    List<MembreCommissionCorrection> findByCommissionIdInOrderByRoleAscNomAsc(Collection<Long> commissionIds);

    List<MembreCommissionCorrection> findByCommissionId(Long commissionId);
}
