package com.ledgerbid.api.config;

import com.ledgerbid.api.entity.Role;
import com.ledgerbid.api.entity.UserAccount;
import com.ledgerbid.api.error.ApiException;
import com.ledgerbid.api.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {
    @Bean
    public FilterRegistrationBean<AuthFilter> authFilterRegistration(AuthService auth) {
        FilterRegistrationBean<AuthFilter> bean = new FilterRegistrationBean<>(new AuthFilter(auth));
        bean.addUrlPatterns("/api/*");
        bean.setOrder(1);
        return bean;
    }
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }

    public static UserAccount actor(HttpServletRequest request) {
        Object user = request.getAttribute("authUser");
        if (!(user instanceof UserAccount account)) {
            throw ApiException.unauthorized("Login required");
        }
        return account;
    }

    public static UserAccount admin(HttpServletRequest request) {
        UserAccount user = actor(request);
        if (user.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Admin only");
        }
        return user;
    }
}
