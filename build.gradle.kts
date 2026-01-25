plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "de.jotibi"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    
    processResources {
        filesMatching("plugin.yml") {
            expand("project" to project)
        }
    }
    
    shadowJar {
        archiveBaseName.set("OneWayElytra")
        archiveClassifier.set("")
    }
    
    build {
        dependsOn(shadowJar)
    }
}
