plugins {
    java
}

group = "de.example"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

// Funktioniert mit normaler Ordnerstruktur (src/main/...) UND wenn alle Dateien lose im Hauptordner liegen.
sourceSets {
    main {
        java {
            setSrcDirs(listOf(".", "src/main/java"))
            include("*.java", "de/**/*.java")
        }
        resources {
            setSrcDirs(listOf(".", "src/main/resources"))
            include("plugin.yml", "config.yml")
        }
    }
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("plugin.yml") { expand(props) }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
