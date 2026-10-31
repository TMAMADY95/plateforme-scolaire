package com.plateforme.scolaire.repository;

import com.plateforme.scolaire.domain.PieceJointe;
import com.plateforme.scolaire.web.dto.PieceJointeDto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PieceJointeRepository extends JpaRepository<PieceJointe, Long> {

    /** Sans le contenu : on ne charge pas les fichiers pour afficher une liste. */
    @Query("select new com.plateforme.scolaire.web.dto.PieceJointeDto(p.id, p.nom, p.typeMime, p.taille, p.createdAt) "
            + "from PieceJointe p where p.inspection.id = :inspectionId order by p.id")
    List<PieceJointeDto> listerDeLInspection(@Param("inspectionId") Long inspectionId);

    long countByInspectionId(Long inspectionId);
}
