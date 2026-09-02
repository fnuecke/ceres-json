plugins {
    `java-library`
    alias(libs.plugins.mavenPublish)
}

fun getGitRef(): String = try {
    providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim().ifEmpty { "unknown" }
} catch (ignored: Throwable) {
    "unknown"
}

val semver: String by project

version = "$semver+${getGitRef()}"
group = "li.cil.ceres"

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.jsr305)

    api(libs.ceres)
    api(libs.gson)

    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
    testRuntimeOnly(libs.junit.launcher)
}

tasks.withType<AbstractArchiveTask>().configureEach {
    archiveVersion = semver
}

mavenPublishing {
    publishToMavenCentral()
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }

    coordinates(group.toString(), project.name, semver)

    pom {
        name = "Ceres JSON"
        description = "Gson-backed JSON serialization backend for Ceres."
        url = "https://github.com/fnuecke/ceres-json"
        licenses {
            license {
                name = "MIT License"
                url = "https://github.com/fnuecke/ceres-json/blob/main/LICENSE"
            }
        }
        developers {
            developer {
                id = "fnuecke"
                name = "Florian Nücke"
            }
        }
        scm {
            connection = "scm:git:https://github.com/fnuecke/ceres-json.git"
            developerConnection = "scm:git:ssh://git@github.com/fnuecke/ceres-json.git"
            url = "https://github.com/fnuecke/ceres-json"
        }
    }
}

tasks.test {
    useJUnitPlatform()
    systemProperty("li.cil.ceres.disableCodeGen", "false")
}

val testReflection = tasks.register<Test>("testReflection") {
    description = "reflection-based serializer tests"
    group = "verification"

    useJUnitPlatform()
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    systemProperty("li.cil.ceres.disableCodeGen", "true")
}

tasks.check {
    dependsOn(testReflection)
}
