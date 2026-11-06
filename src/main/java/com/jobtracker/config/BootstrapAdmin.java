package com.jobtracker.config;

import com.jobtracker.entity.User;
import com.jobtracker.enums.UserRole;
import com.jobtracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BootstrapAdmin {
 @Bean CommandLineRunner createSuperAdmin(UserRepository users,PasswordEncoder encoder,
  @Value("${app.bootstrap.superadmin-email}") String email,
  @Value("${app.bootstrap.superadmin-password}") String password){
  return args->{if(users.findByEmailIgnoreCase(email).isEmpty())users.save(new User("Superadmin",email.toLowerCase(),encoder.encode(password),UserRole.SUPER_ADMIN));};
 }
}
