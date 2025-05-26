import java.nio.charset.StandardCharsets

plugins {
  id("java")
  id("fabric-loom") version ("1.11.0-alpha.10")
}

group = "cc.luciel"
version = "0.0.0-develop"

repositories {
  mavenLocal()
  mavenCentral()
  maven(url = "https://repo.viaversion.com")
  maven(url = "https://maven.lenni0451.net/everything")
  maven(url = "https://jitpack.io") { content { includeGroup("com.github.Oryxel") } }
}

dependencies {
  annotationProcessor(libs.annotations.get())
  implementation(libs.annotations.get())

  minecraft("com.mojang:minecraft:${property("minecraft_version")}")
  mappings(loom.officialMojangMappings())
  modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")

  // soft dependency
  modCompileOnly("com.viaversion:viafabricplus-api:4.1.1")

  val clazzLinux = arrayOf("linux-aarch_64", "linux-riscv64", "linux-x86_64")
  val clazzMac = arrayOf("osx-aarch_64", "osx-x86_64")

  val nativeLinuxEpollDep = libs.netty.transport.native.epoll.get()
  val nativeLinuxIoUringDep = libs.netty.transport.native.io.uring.get()
  val nativeMacKqueueDep = libs.netty.transport.native.kqueue.get()

  nativeLinuxEpollDep.artifacts.addAll(clazzLinux.map { nativeLinuxEpollDep.artifact { classifier = it } })
  nativeMacKqueueDep.artifacts.addAll(clazzMac.map { nativeMacKqueueDep.artifact { classifier = it } })
  nativeLinuxIoUringDep.artifacts
    .addAll(clazzLinux.map { nativeLinuxIoUringDep.artifact { classifier = it } })

  arrayOf(
    libs.bundles.network.all,
    nativeLinuxEpollDep,
    nativeLinuxIoUringDep,
    nativeMacKqueueDep,
  ).forEach { implementation(it); include(it) }
}

tasks.withType<JavaCompile> {
  sourceCompatibility = JavaVersion.VERSION_21.toString()
  targetCompatibility = JavaVersion.VERSION_21.toString()
  options.encoding = StandardCharsets.UTF_8.toString()
}

tasks.withType<AbstractArchiveTask> {
  isReproducibleFileOrder = true
  isPreserveFileTimestamps = false
}
