plugins {
    java
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("com.diffplug.spotless") version "6.25.0"
    id("net.ltgt.errorprone") version "3.1.0"
}

group = "com.github.cybellereaper"
version = "0.0.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    implementation("com.google.dagger:dagger:2.51.1")
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")
    annotationProcessor("com.google.dagger:dagger-compiler:2.51.1")
    implementation("org.jooq:jool:0.9.15")
    implementation("io.vavr:vavr:0.10.4")
    implementation("com.google.guava:guava:33.2.1-jre")
    implementation("com.google.code.gson:gson:2.11.0")
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    errorprone("com.google.errorprone:error_prone_core:2.29.2")
}

tasks {
    runServer {
        minecraftVersion("1.21")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.test {
    useJUnitPlatform()
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat("1.17.0")
    }
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
