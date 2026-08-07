import java.nio.charset.StandardCharsets

plugins {
  id("java")
  id("net.fabricmc.fabric-loom") version ("1.17.19")
}

group = "pet.liawr"
version = "1.3.0-release"

repositories {
  mavenLocal()
  mavenCentral()
}

dependencies {
  annotationProcessor(libs.annotations.get())
  implementation(libs.annotations.get())

  minecraft("com.mojang:minecraft:${property("minecraft_version")}")
  implementation("net.fabricmc:fabric-loader:${property("loader_version")}")

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
  ).forEach { implementation(it); /*include(it)*/ }
}

tasks.withType<JavaCompile> {
  options.release.set(26)
  options.compilerArgs.addAll(listOf("-Xlint:all", "-g", "-parameters", "--enable-preview"))

  options.encoding = StandardCharsets.UTF_8.toString()
}

tasks.withType<AbstractArchiveTask> {
  isReproducibleFileOrder = true
  isPreserveFileTimestamps = false
}
