package com.example.demo.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Lấy đường dẫn tuyệt đối của thư mục upload
        String absolutePath = Paths.get(uploadDir).toAbsolutePath().toUri().toString();

        // Ánh xạ URL /uploads/** vào thư mục vật lý trên ổ cứng
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(absolutePath);
    }
}