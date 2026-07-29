import java.util.Properties

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}
val pzInstallPath: String? = localProperties.getProperty("pz.install.path")?.trim()?.ifBlank { null }

plugins {
    java
}

group = "glas.voip"
version = "0.1.0-spike"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.ow2.asm:asm:9.10.1")
    implementation("org.ow2.asm:asm-util:9.10.1")

    if (pzInstallPath != null) {
        compileOnly(files(pzInstallPath))
    }

    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes(
            "Premain-Class" to "glas.voip.spike.Agent",
            "Can-Retransform-Classes" to "true"
        )
    }
    // The agent jar must be self-contained (fat jar): it's loaded via -javaagent before the
    // game's own classpath is set up, so ASM can't be resolved externally at that point.
    // DIY merge (not the shadow plugin) is fine while the only dependency is ASM, which ships
    // no ServiceLoader provider files or signed entries. Revisit with a proper shadow-jar
    // plugin before adding dependencies that do (EXCLUDE would then silently drop entries).
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
