package com.example.toolshopapi.service.impl;

import com.example.toolshopapi.dto.UserDto;
import com.example.toolshopapi.dto.auth.JwtResponse;
import com.example.toolshopapi.dto.auth.SignUpDto;
import com.example.toolshopapi.dto.notification.NotificationDto;
import com.example.toolshopapi.exceptions.DuplicateKeyException;
import com.example.toolshopapi.mapping.UserMapper;
import com.example.toolshopapi.model.email.constants.NotificationType;
import com.example.toolshopapi.model.models.User;
import com.example.toolshopapi.repository.RoleRepository;
import com.example.toolshopapi.service.redis.JwtCacheService;
import com.example.toolshopapi.security.JwtTokenProvider;
import com.example.toolshopapi.service.redis.SessionRedisService;
import com.example.toolshopapi.security.UserDetailsServiceImpl;
import com.example.toolshopapi.service.iterfaces.AuthService;
import com.example.toolshopapi.service.iterfaces.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager manager;
    private final UserDetailsServiceImpl userDetails;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ApplicationContext applicationContext;
    private final RoleRepository roleRepository;
    private final UserService userService;
    private final UserMapper userMapper;
    private final JwtCacheService cacheService;
    private final SessionRedisService sessionRedisService;
    @Override
    @Transactional
    public UserDto register(SignUpDto signUpDto) {
        if (signUpDto == null) {
            throw new IllegalArgumentException("signUpDto is null check signUpDto value");
        }
        if (Boolean.TRUE.equals(userService.existsByEmail(signUpDto.getEmail()))) {
            throw new DuplicateKeyException("User with email " + signUpDto.getEmail() + " is exists");
        }
        User newUser = userMapper.signUpToUser(signUpDto);
        newUser.setRoles(Set.of(roleRepository.findAllByName("ROLE_USER").get()));
        newUser.setPassword(passwordEncoder.encode(signUpDto.getPassword()));
        applicationContext.publishEvent(getNotificationDto(signUpDto,NotificationType.REGISTRATION));

        return userService.save(userMapper.toUserDto(newUser));
    }

    @Override
    @Transactional
    public JwtResponse login(SignUpDto signUpDto) {
        if (signUpDto == null){
            throw new IllegalArgumentException("signUpDto is null check signUpDto value");
        }
        if (Boolean.FALSE.equals(userService.existsByEmail(signUpDto.getEmail()))) {
            throw new EntityNotFoundException("User with email " + signUpDto.getEmail() + " not found!");
        }
        if (!sessionRedisService.checkUserCreation(signUpDto)) {
            sessionRedisService.saveUserSession(signUpDto);
        }

        Optional<String> cachedToken = cacheService.getCachedTokenForEmail(signUpDto.getEmail());

        if (cachedToken.isPresent()){
            return new JwtResponse(cachedToken.get());
        }
        else {
            manager.authenticate(new UsernamePasswordAuthenticationToken(signUpDto.getEmail(), signUpDto.getPassword()));
            UserDetails loadUserByUsername = userDetails.loadUserByUsername(signUpDto.getEmail());
            String token = tokenProvider.generateToken(loadUserByUsername);
            cacheService.cacheToken(token, signUpDto.getEmail());
            return new JwtResponse(token);
        }
    }

    private NotificationDto getNotificationDto(SignUpDto signUpDto,NotificationType notificationType) {
        NotificationDto notificationDto = new NotificationDto();
        notificationDto.setNotificationType(notificationType);
        notificationDto.setEmail(signUpDto.getEmail());
        notificationDto.setFirstName(signUpDto.getEmail());
        return notificationDto;
    }

}
