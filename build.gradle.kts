plugins {
    kotlin("jvm") version "2.0.21"
}

group = "com.example"
version = "2.3.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

kotlin {
    jvmToolchain(21)
}
