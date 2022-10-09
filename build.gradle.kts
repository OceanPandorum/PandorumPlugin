plugins {
    java
}

repositories {
    mavenCentral()
    maven(url = "https://www.jitpack.io")
}

val mindustryVersion = "v139"

dependencies {
    compileOnly("com.github.Anuken.Arc:arc-core:$mindustryVersion")
    compileOnly("com.github.Anuken.Mindustry:core:$mindustryVersion")
}

tasks.jar {
    from (configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
