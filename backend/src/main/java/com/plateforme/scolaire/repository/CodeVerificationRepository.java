package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.CodeVerification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeVerificationRepository extends JpaRepository<CodeVerification, Long> {
}
