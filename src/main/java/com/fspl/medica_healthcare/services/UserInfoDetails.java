//package com.fspl.medica_healthcare.services;
//
//import com.fspl.medica_healthcare.models.User;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.UserDetails;
//
//
//import java.util.Collection;
//import java.util.List;
//import java.util.stream.Collectors;
//
//public class UserInfoDetails implements UserDetails {
//
//    private final String username;
//    private final String password;
//    private final List<GrantedAuthority> authorities;
//
//    public UserInfoDetails(User userInfo) {
//        this.username = userInfo.getUsername();
//        this.password = userInfo.getPassword();
//        this.authorities = List.of(userInfo.getRoles().split(","))
//                .stream()
//                .map(SimpleGrantedAuthority::new)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public Collection<? extends GrantedAuthority> getAuthorities() {
//        return authorities;
//    }
//
//    @Override
//    public String getPassword() {
//        return password;
//    }
//
//    @Override
//    public String getUsername() {
//        return username;
//    }
//
//}
//
