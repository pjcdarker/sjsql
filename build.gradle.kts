plugins {
    id("java")
    id("idea")
}

group = "com.reader.sjsql"
version = "0.1.0"

repositories {
    maven("https://maven.aliyun.com/repository/public/")
    mavenLocal()
    mavenCentral()
}



dependencies {

    testImplementation(libs.bundles.log4j)
    testImplementation(libs.slf4j.api)
    testImplementation(libs.druid)
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
// H2
tasks.register<Test>("h2Test") {
    description = "Run H2 tests"
    group = "database"
    useJUnitPlatform()
    systemProperty("test.db.type", "h2")
    include("**/*Test.class")
    // filter {
    //     includeTestsMatching("H2TestSuite")
    // }
}

// MySQL
tasks.register<Test>("mysqlTest") {
    description = "Run MySQL tests"
    group = "database"
    useJUnitPlatform()
    systemProperty("test.db.type", "mysql")
    include("**/*Test.class")
    // filter {
    //     includeTestsMatching("MysqlTestSuite")
    // }
}