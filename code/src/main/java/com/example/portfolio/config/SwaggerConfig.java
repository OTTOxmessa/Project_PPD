package com.example.portfolio.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Swagger UI: http://localhost:8080/swagger-ui.html  (OpenAPI JSON: /api-docs)
// วิธีลองเรียก API ที่ต้อง login: POST /api/v1/auth/login -> คัดลอก token -> กดปุ่ม Authorize แล้ววาง token
@Configuration
public class SwaggerConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI portfolioSystemOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Portfolio Management System API")
                        .description("CP353002 Principles of Software Design and Development — Investment Portfolio Management System")
                        .version("v1.0"))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT จาก /api/v1/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
