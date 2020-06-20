plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.3.70"

    id("org.openjfx.javafxplugin") version "0.0.8"

    id("com.github.johnrengelman.shadow") version "5.2.0"

    // Apply the application plugin to add support for building a CLI application.
    application
}

repositories {
    // Use jcenter for resolving dependencies.
    // You can declare any Maven/Ivy/file repository here.
//    jcenter()
    maven(url = "https://maven.aliyun.com/repository/public")
    maven(url = "https://mirrors.huaweicloud.com/repository/maven/")
    maven(url = "https://maven.aliyun.com/repository/gradle-plugin")
}

val tornadoVersion = "1.7.20"
val vertxVersion = "3.8.5"
val coroutineVersion = "1.3.5"
val mysqlVersion = "8.0.19"
val exposedVersion = "0.17.7"

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("org.jetbrains.exposed:exposed:$exposedVersion")

    implementation("no.tornado:tornadofx:$tornadoVersion")

    implementation("io.vertx:vertx-core:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutineVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:$coroutineVersion")
    implementation("mysql:mysql-connector-java:$mysqlVersion")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

application {
    // Define the main class for the application.
    mainClassName = "dbs.lab3.wechat.AppKt"
}

javafx {
    modules("javafx.controls")
}

val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "1.8"

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to application.mainClassName))
        }
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}
