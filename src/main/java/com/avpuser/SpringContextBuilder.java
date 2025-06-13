package com.avpuser;

import com.avpuser.env.EnvironmentEnum;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

public class SpringContextBuilder {

    /**
     * Builds and runs a Spring Boot application context.
     * If the current OS is macOS, the "dev" profile is activated automatically.
     *
     * @param applicationClass the main application class annotated with {@code @SpringBootApplication}
     * @param args the application arguments
     * @return the initialized {@link ApplicationContext}
     */
    public static <T> ApplicationContext run(Class<T> applicationClass, String[] args) {
        SpringApplication app = new SpringApplication(applicationClass);

        if (isMacos()) {
            app.setAdditionalProfiles(EnvironmentEnum.DEV.getEnvironment());
        }

        return app.run(args);
    }

    public static boolean isMacos() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("mac");
    }
}