package com.unbound.backend.service;

import com.unbound.backend.dto.RegisterRequest;
import com.unbound.backend.dto.LoginRequest;
import com.unbound.backend.dto.AuthResponse;
import com.unbound.backend.entity.User;
import com.unbound.backend.entity.Student;
import com.unbound.backend.entity.College;
import com.unbound.backend.repository.UserRepository;
import com.unbound.backend.repository.StudentRepository;
import com.unbound.backend.repository.CollegeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private CollegeRepository collegeRepository;
    @Autowired
    private PasswordService passwordService;
    @Autowired
    private JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }
        User.Role role = User.Role.valueOf(request.getRole());
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordService.hashPassword(request.getPassword()))
                .role(role)
                .createdAt(new java.sql.Timestamp(System.currentTimeMillis()))
                .build();
        user = userRepository.save(user);
        String sname = null, cname = null;
        if (role == User.Role.Student) {
            if (request.getCollegeId() == null) {
                throw new RuntimeException("College ID is required for student registration");
            }
            College assignedCollege = collegeRepository.findById(request.getCollegeId())
                .orElseThrow(() -> new RuntimeException("College not found for given collegeId"));
            Student student = Student.builder()
                    .user(user)
                    .sname(request.getSname())
                    .college(assignedCollege)
                    .build();
            studentRepository.save(student);
            sname = student.getSname();
        } else if (role == User.Role.College) {
            College college = College.builder()
                    .user(user)
                    .cname(request.getCname())
                    .cdescription(request.getCdescription())
                    .address(request.getAddress())
                    .contactEmail(request.getContactEmail())
                    .build();
            collegeRepository.save(college);
            cname = college.getCname();
        }
        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getRole().name(), user.getEmail(), sname, cname);
    }

    public AuthResponse login(LoginRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Invalid credentials");
        }
        User user = userOpt.get();
        if (!passwordService.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        String sname = null, cname = null;
        if (user.getRole() == User.Role.Student) {
            Student student = studentRepository.findAll().stream()
                    .filter(s -> s.getUser().getUid().equals(user.getUid()))
                    .findFirst().orElse(null);
            if (student != null) sname = student.getSname();
        } else if (user.getRole() == User.Role.College) {
            College college = collegeRepository.findAll().stream()
                    .filter(c -> c.getUser().getUid().equals(user.getUid()))
                    .findFirst().orElse(null);
            if (college != null) cname = college.getCname();
        }
        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getRole().name(), user.getEmail(), sname, cname);
    }
} 