plugins {
    id("java")
    id("idea")
    id("com.vanniktech.maven.publish") version "0.34.0"
}

group = "io.github.pjcdarker"
version = "1.0.0"

repositories {
    maven("https://maven.aliyun.com/repository/public/")
    mavenLocal()
    mavenCentral()
}

dependencies {

    testImplementation(libs.bundles.log4j)
    testImplementation(libs.slf4j.api)
    // testImplementation(libs.druid)
    testImplementation(libs.hikaricp)
    testImplementation(libs.h2)
    testImplementation(libs.mysql.connector)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)

    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.log4jdbc)
}

tasks.test {
    useJUnitPlatform()
}

// database test
listOf("h2", "mysql").forEach {
    tasks.register<Test>("${it}Test") {
        description = "Run ${it.uppercase()} tests"
        group = "database"
        useJUnitPlatform()
        systemProperty("test.db.type", it)
        include("**/*Test.class")
    }
}

mavenPublishing {

    coordinates(project.group.toString(), "sjsql", project.version.toString())

    pom {
        name.set("sjsql")
        description.set("A simple SQL builder for Java")
        inceptionYear.set("2025")
        url.set("https://github.com/pjcdarker/sjsql")
        licenses {
            license {
                name.set("MIT License")
                url.set("http://www.opensource.org/licenses/mit-license.php")
            }
        }
        developers {
            developer {
                id.set("pjcdarker")
                name.set("Reader")
                url.set("https://github.com/pjcdarker/")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/pjcdarker/sjsql.git")
            developerConnection.set("scm:git:ssh://github.com/pjcdarker/sjsql.git")
            url.set("https://github.com/pjcdarker/sjsql")
        }
    }

    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
}

tasks.javadoc {
    if (JavaVersion.current().isJava8Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}

tasks.withType<GenerateModuleMetadata>().configureEach {
    dependsOn("plainJavadocJar")
}