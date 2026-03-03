plugins {
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"
    java
}

group = "com.example"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

extra["resilience4jVersion"] = "2.3.0"

dependencies {
    // Spring WebFlux reactive stack
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Observability
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Caching -- direct Caffeine (not Spring Cache abstraction, incompatible with Mono<T>)
    implementation("com.github.ben-manes.caffeine:caffeine")

    // Resilience
    implementation("io.github.resilience4j:resilience4j-spring-boot3:${property("resilience4jVersion")}")
    implementation("io.github.resilience4j:resilience4j-reactor:${property("resilience4jVersion")}")

    // Structured JSON logging
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.wiremock.integrations:wiremock-spring-boot:3.6.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
