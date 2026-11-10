package com.jobtracker.service;

import com.jobtracker.dto.CompanyRequest;
import com.jobtracker.dto.CompanyResponse;
import com.jobtracker.entity.Company;
import com.jobtracker.entity.User;
import com.jobtracker.enums.CompanyApprovalStatus;
import com.jobtracker.enums.UserRole;
import com.jobtracker.repository.CompanyRepository;
import com.jobtracker.repository.UserRepository;
import java.text.Normalizer;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyService {
 private final CompanyRepository companies; private final UserRepository users;
 public CompanyService(CompanyRepository companies,UserRepository users){this.companies=companies;this.users=users;}
 private User current(String email){return users.findByEmailIgnoreCase(email).orElseThrow();}
 private String normalize(String s){return Normalizer.normalize(s.trim().toLowerCase(),Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[^a-z0-9]","");}
 private CompanyResponse dto(Company c){return new CompanyResponse(c.getId(),c.getName(),c.getCnpj(),c.getWebsite(),c.getDescription(),c.getApprovalStatus().name(),c.getCreatedBy().getId(),c.getCreatedBy().getName(),c.getReviewedBy()==null?null:c.getReviewedBy().getId(),c.getReviewedAt(),c.getReviewNotes());}
 @Transactional(readOnly=true) public List<CompanyResponse> list(String email){
  User u=current(email); return companies.findByApprovalStatusOrCreatedByIdOrderByName(CompanyApprovalStatus.APPROVED,u.getId()).stream().map(this::dto).toList();
 }
 @Transactional(readOnly=true) public CompanyResponse get(Long id,String email){
  User u=current(email); Company c=companies.findById(id).orElseThrow(()->new NoSuchElementException("Empresa não encontrada."));
  if(c.getApprovalStatus()!=CompanyApprovalStatus.APPROVED && !c.getCreatedBy().getId().equals(u.getId()) && u.getRole()!=UserRole.SUPER_ADMIN) throw new NoSuchElementException("Empresa não encontrada.");
  return dto(c);
 }
 @Transactional public CompanyResponse create(CompanyRequest r,String email){
  User u=current(email); String normalized=normalize(r.name()); String cnpj=r.cnpj()==null?null:r.cnpj().replaceAll("\\D","");
  if(companies.existsByNormalizedNameAndApprovalStatus(normalized,CompanyApprovalStatus.APPROVED) || (cnpj!=null&&!cnpj.isBlank()&&companies.existsByCnpjAndApprovalStatus(cnpj,CompanyApprovalStatus.APPROVED)))
   throw new IllegalStateException("Já existe uma empresa aprovada com esses dados. Utilize o cadastro existente.");
  return dto(companies.save(new Company(r.name().trim(),normalized,cnpj,r.website(),r.description(),u)));
 }
 @Transactional public CompanyResponse update(Long id,CompanyRequest r,String email){
  User u=current(email); Company c=companies.findById(id).orElseThrow(()->new NoSuchElementException("Empresa não encontrada."));
  if(!c.getCreatedBy().getId().equals(u.getId())) throw new org.springframework.security.access.AccessDeniedException("Somente o criador pode editar.");
  if(c.getApprovalStatus()==CompanyApprovalStatus.APPROVED) throw new IllegalStateException("Empresa aprovada não pode ser editada diretamente.");
  String normalized=normalize(r.name()); String cnpj=r.cnpj()==null?null:r.cnpj().replaceAll("\\D","");
  c.update(r.name().trim(),normalized,cnpj,r.website(),r.description());
  if(c.getApprovalStatus()==CompanyApprovalStatus.REJECTED)c.review(CompanyApprovalStatus.PENDING,null,null);
  return dto(c);
 }
 @Transactional public void delete(Long id,String email){
  User u=current(email); Company c=companies.findById(id).orElseThrow(()->new NoSuchElementException("Empresa não encontrada."));
  if(!c.getCreatedBy().getId().equals(u.getId())) throw new org.springframework.security.access.AccessDeniedException("Somente o criador pode excluir.");
  if(c.getApprovalStatus()==CompanyApprovalStatus.APPROVED) throw new IllegalStateException("Empresa aprovada não pode ser excluída.");
  companies.delete(c);
 }
 @Transactional(readOnly=true) public List<CompanyResponse> pending(){return companies.findByApprovalStatusOrderByName(CompanyApprovalStatus.PENDING).stream().map(this::dto).toList();}
 @Transactional(readOnly=true) public List<CompanyResponse> adminList(){return companies.findAll().stream().map(this::dto).toList();}
 @Transactional public CompanyResponse approve(Long id,String email){return review(id,email,CompanyApprovalStatus.APPROVED,null);}
 @Transactional public CompanyResponse reject(Long id,String email,String reason){return review(id,email,CompanyApprovalStatus.REJECTED,reason);}
 private CompanyResponse review(Long id,String email,CompanyApprovalStatus status,String reason){
  Company c=companies.findById(id).orElseThrow(()->new NoSuchElementException("Empresa não encontrada."));
  if(c.getApprovalStatus()!=CompanyApprovalStatus.PENDING)throw new IllegalStateException("Somente empresas pendentes podem ser analisadas.");
  User reviewer=current(email);
  if(status==CompanyApprovalStatus.APPROVED){
   boolean duplicate=companies.existsByNormalizedNameAndApprovalStatus(c.getNormalizedName(),CompanyApprovalStatus.APPROVED)
    ||(c.getCnpj()!=null&&!c.getCnpj().isBlank()&&companies.existsByCnpjAndApprovalStatus(c.getCnpj(),CompanyApprovalStatus.APPROVED));
   if(duplicate)throw new IllegalStateException("Já existe uma empresa aprovada equivalente.");
  }
  c.review(status,reviewer,reason);return dto(c);
 }
}
