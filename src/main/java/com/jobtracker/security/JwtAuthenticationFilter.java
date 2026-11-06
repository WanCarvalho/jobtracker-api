package com.jobtracker.security;

import com.jobtracker.service.CustomUserDetailsService;
import com.jobtracker.service.JwtService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
 private final JwtService jwt; private final CustomUserDetailsService users;
 public JwtAuthenticationFilter(JwtService jwt, CustomUserDetailsService users){this.jwt=jwt;this.users=users;}
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException {
  String header=request.getHeader("Authorization");
  if(header!=null && header.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication()==null){
   String token=header.substring(7);
   if(jwt.isValid(token)){
    String email=jwt.extractEmail(token); UserDetails details=users.loadUserByUsername(email);
    var auth=new UsernamePasswordAuthenticationToken(details,null,details.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(auth);
   }
  }
  chain.doFilter(request,response);
 }
}
