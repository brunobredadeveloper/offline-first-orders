plugins { kotlin("jvm") }
kotlin { jvmToolchain(17) }
dependencies {
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
 testImplementation(kotlin("test-junit5"))
 testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
 testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
 testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}
tasks.test { useJUnitPlatform() }
