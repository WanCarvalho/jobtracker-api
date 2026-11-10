package com.jobtracker.repository;

import com.jobtracker.entity.Company;
import com.jobtracker.enums.CompanyApprovalStatus;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company,Long> {
 List<Company> findByApprovalStatusOrderByName(CompanyApprovalStatus status);
 List<Company> findByCreatedByIdOrderByName(Long userId);
 List<Company> findByApprovalStatusOrCreatedByIdOrderByName(CompanyApprovalStatus status,Long userId);
 boolean existsByCnpjAndApprovalStatus(String cnpj,CompanyApprovalStatus status);
 boolean existsByNormalizedNameAndApprovalStatus(String name,CompanyApprovalStatus status);
}
