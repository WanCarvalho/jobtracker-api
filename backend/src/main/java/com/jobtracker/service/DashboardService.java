package com.jobtracker.service;

import com.jobtracker.repository.ApplicationRepository;
import com.jobtracker.repository.StageRepository;
import com.jobtracker.repository.UserRepository;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
 private final ApplicationRepository apps;private final UserRepository users;private final StageRepository stages;
 public DashboardService(ApplicationRepository a,UserRepository u,StageRepository s){apps=a;users=u;stages=s;}
 public Map<String,Long> summary(String email){
  var u=users.findByEmailIgnoreCase(email).orElseThrow();long total=apps.countByUserIdAndDeletedAtIsNull(u.getId());
  long closed=apps.countByUserIdAndDeletedAtIsNullAndStageIsFinal(u.getId(),true);
  return Map.of("totalApplications",total,"activeApplications",total-closed,"finalApplications",closed);
 }
 public List<Map<String,Object>> distribution(String email){
  var u=users.findByEmailIgnoreCase(email).orElseThrow();var all=apps.findByUserIdAndDeletedAtIsNullOrderByAppliedAtDesc(u.getId());
  Map<Long,Long> counts=new HashMap<>();all.forEach(a->counts.merge(a.getStage().getId(),1L,Long::sum));
  return stages.findByUserIdAndIsActiveTrueOrderByPosition(u.getId()).stream().map(s->Map.<String,Object>of("stageId",s.getId(),"stageName",s.getName(),"color",s.getColor(),"count",counts.getOrDefault(s.getId(),0L))).toList();
 }
}
