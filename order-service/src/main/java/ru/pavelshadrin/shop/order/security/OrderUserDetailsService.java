package ru.pavelshadrin.shop.order.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pavelshadrin.shop.order.repository.UserRepository;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OrderUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public OrderUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(UserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));
    }

    public UserPrincipal loadUserById(UUID userId) throws UsernameNotFoundException {
        return userRepository.findById(userId)
                .map(UserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));
    }
}
