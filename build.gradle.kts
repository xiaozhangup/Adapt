import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/*
 * Adapt is Copyright (c) 2021 Arcane Arts (Volmit Software)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

plugins {
    java
    `java-library`
    id("org.jetbrains.kotlin.jvm") version "2.3.20"
    id("io.freefair.lombok") version "9.5.0"
    id("com.gradleup.shadow") version "9.4.3"
    id("org.jetbrains.kotlin.plugin.lombok") version "2.3.20"
    id("me.xiaozhangup.sftp-uploader") version "0.1.0"
//    id("com.diffplug.spotless") version "7.0.4"
}

version = "1.16.12"
val apiVersion = "1.21"
val pluginName = rootProject.name
val main = "com.volmit.adapt.Adapt"
val outputJar = layout.buildDirectory.file("libs/Adapt-$version-all.jar")
val adaptJar = layout.buildDirectory.file("Adapt-$version.jar")

/**
 * Gradle is weird sometimes, we need to delete the plugin yml from the build folder to actually filter properly.
 */
layout.buildDirectory.file("resources/main/plugin.yml").get().asFile.delete()

/**
 * Expand properties into plugin yml
 */

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

tasks.processResources {
    filesMatching("**/plugin.yml") {
        expand(
            "name" to pluginName,
            "version" to version,
            "main" to main,
            "apiversion" to apiVersion,
        )
    }
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.org/repository/maven-public")
    maven("https://mvn.lumine.io/repository/maven-public/")
    maven("https://nexus.frengor.com/repository/public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.glaremasters.me/repository/bloodshot/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.momirealms.net/releases/")
    maven("https://repo.oraxen.com/releases")
    maven("https://repo.alessiodp.com/releases")
    maven("https://jitpack.io")
}

//spotless {
//    java {
//        eclipse()
//        target("src/**/*.java")
//        leadingTabsToSpaces(4)
//    }
//}

dependencies {
    // Provided or Classpath
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")

    compileOnly("me.clip:placeholderapi:2.11.6")
    implementation("com.frengor:ultimateadvancementapi-shadeable:3.0.0-beta-2")
    implementation("com.jeff-media:custom-block-data:2.2.3")

    // Dynamically Loaded
    compileOnly("me.xiaozhangup.octopus:octopus-api:26.2-R0.1-SNAPSHOT")
    compileOnly("me.xiaozhangup:SlimeCargoNext:1.0.2")
    compileOnly("me.xiaozhangup:OrangDomain:1.0.2")
    compileOnly("me.xiaozhangup:WhaleMechanism:1.0.1")
    compileOnly("com.github.retrooper:packetevents-spigot:2.11.1")
    compileOnly("com.googlecode.concurrentlinkedhashmap:concurrentlinkedhashmap-lru:1.4.2")
    compileOnly("org.apache.commons:commons-lang3:3.12.0")
    compileOnly("com.google.code.gson:gson:2.10")
    compileOnly("com.elmakers.mine.bukkit:EffectLib:10.10")
    compileOnly("com.google.guava:guava:30.1-jre")
}

configurations.configureEach {
    resolutionStrategy.cacheChangingModulesFor(60, "minutes")
    resolutionStrategy.cacheDynamicVersionsFor(60, "minutes")
}

/**
 * We need parameter meta for the decree command system
 */
tasks.compileJava {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

/**
 * Configure Adapt for shading
 */
tasks.shadowJar {
    append("plugin.yml")
    relocate("com.fren_gor.ultimateAdvancementAPI", "com.volmit.adapt.util.advancements")
    relocate("com.jeff_media.customblockdata", "com.volmit.adapt.util.customblocks")
    dependencies {
        include(dependency("com.frengor:ultimateadvancementapi-shadeable:"))
        include(dependency("net.byteflux:"))
        include(dependency("com.jeff-media:custom-block-data:"))
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
    finalizedBy(tasks.named("adapt"))
}

tasks.register<Copy>("adapt") {
    group = "adapt"
    from(outputJar)
    into(layout.buildDirectory)
    rename { fileName ->
        fileName.replace(outputJar.get().asFile.name, adaptJar.get().asFile.name)
    }
    dependsOn(tasks.shadowJar)
}

tasks.named("uploadSftp") {
    mustRunAfter(tasks.named("adapt"))
}

sftpUploader {
    host.set("xiaozhangup@s1.dimc.cloud")
    target.set("Minecraft")
    jars.set(
        listOf(
            adaptJar.get().asFile.absolutePath,
        )
    )
}
