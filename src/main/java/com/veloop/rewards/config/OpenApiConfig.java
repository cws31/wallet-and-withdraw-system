package com.veloop.rewards.config;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "VELoop Rewards Wallet & Withdrawal API", version = "1.0.0", description = """
        **🚧 DEVELOPMENT STATUS: IN PROGRESS**

        Backend API for the VELoop Rewards Wallet & Withdrawal System.

        | **API Owner** | **Developed By** |
        |---|---|
        | **[VELoop Rewards](https://velooprewards.in)** | **Sonu Kumar** |
        | [Official Website](https://velooprewards.in) | [GitHub — cws31](https://github.com/cws31) |
        | | [LinkedIn — Sonu Kumar](https://www.linkedin.com/in/sonu-kumar-9a59b52a4/) |

        ---

        **Core Services**

        `Authentication` · `JWT Authorization` · `Wallet` · `Rewards` · `Payout` · `Withdrawal`

        **Current Scope:** Authentication, JWT security, validation and exception handling are implemented.
        Wallet, payout, withdrawal and additional production features are under active development.
        """, contact = @Contact(name = "VELoop Rewards", url = "https://velooprewards.in"), license = @License(name = "VELoop Rewards")), externalDocs = @ExternalDocumentation(description = "Developer Profile — Sonu Kumar", url = "https://github.com/cws31"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}