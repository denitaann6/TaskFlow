package com.taskflow.taskflow_backend.service;

import com.taskflow.taskflow_backend.entity.User;
import com.taskflow.taskflow_backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Safe, idempotent account initialization.
 * Only creates demo accounts if they do not already exist in the database.
 * Does not overwrite existing accounts, alter passwords on restart, or modify existing tasks.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Predefined Administrator account
        initAccount("admin", "Admin@123", "System Administrator", "ADMIN", "Administrator");

        // Predefined Member accounts corresponding to the existing 12 tasks in taskflow_db
        record MemberSeed(String username, String rawPassword, String displayName, String assignedMemberName) {}

        List<MemberSeed> memberSeeds = List.of(
                new MemberSeed("john", "Member@123", "John", "John"),
                new MemberSeed("subu", "Member@123", "Subu", "subu"),
                new MemberSeed("sheena", "Member@123", "Sheena", "Sheena"),
                new MemberSeed("sebastian", "Member@123", "Sebastian", "Sebastian"),
                new MemberSeed("mary", "Member@123", "Mary", "Mary"),
                new MemberSeed("mathew", "Member@123", "Mathew", "Mathew"),
                new MemberSeed("amrith", "Member@123", "Amrith", "Amrith"),
                new MemberSeed("testuser", "Member@123", "Test User", "Test user"),
                new MemberSeed("usera", "Member@123", "User A", "UserA"),
                new MemberSeed("userb", "Member@123", "User B", "UserB"),
                new MemberSeed("member_a", "Member@123", "Member A", "a"),
                new MemberSeed("member_b", "Member@123", "Member B", "b"),
                new MemberSeed("arjun", "Member@123", "Arjun", "Arjun"),
                new MemberSeed("priya", "Member@123", "Priya", "Priya"),
                new MemberSeed("rahul", "Member@123", "Rahul", "Rahul"),
                new MemberSeed("sneha", "Member@123", "Sneha", "Sneha")
        );

        for (MemberSeed seed : memberSeeds) {
            initAccount(seed.username(), seed.rawPassword(), seed.displayName(), "MEMBER", seed.assignedMemberName());
        }
    }

    private void initAccount(String username, String rawPassword, String displayName, String role, String assignedMemberName) {
        if (userRepository.findByUsernameIgnoreCase(username).isEmpty()) {
            String hashed = passwordEncoder.encode(rawPassword);
            User user = new User(username, hashed, displayName, role, true, assignedMemberName);
            userRepository.save(user);
        }
    }
}
