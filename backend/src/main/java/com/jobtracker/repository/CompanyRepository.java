package com.jobtracker.repository;

import com.jobtracker.entity.Company;
import com.jobtracker.enums.CompanyApprovalStatus;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company,Long> {
 List<Company> findByApprovalStatusAndDeletedAtIsNullOrderByName(CompanyApprovalStatus status);
 List<Company> findByDeletedAtIsNullOrderByName();
 @org.springframework.data.jpa.repository.Query("select c from Company c where c.deletedAt is null and (c.approvalStatus = :status or c.createdBy.id = :userId) order by c.name")
 List<Company> findVisible(CompanyApprovalStatus status,Long userId);
 boolean existsByCnpjAndApprovalStatusAndDeletedAtIsNull(String cnpj,CompanyApprovalStatus status);
 boolean existsByNormalizedNameAndApprovalStatusAndDeletedAtIsNull(String name,CompanyApprovalStatus status);
}
