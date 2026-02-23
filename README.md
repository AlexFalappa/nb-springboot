# NB SpringBoot (Modernized Edition 🚀)

A plugin for Apache NetBeans that greatly facilitates the development of applications using the Spring Boot framework.

This repository is an updated fork of the excellent original work by [AlexFalappa](https://github.com/AlexFalappa/nb-springboot), extensively modernized to support the latest versions of the Java and Spring Boot ecosystems.

---

## ✨ What's New in this Version

The source code and dependencies have been heavily refactored to bring this plugin back to life in modern development environments:

* **JDK 17 to JDK 25 Support:** Compilation has been adapted to the new Java security restrictions, including the reactivation of annotation processors (`-proc:full`) required for the NetBeans internationalization (`Bundle`) system.
* **Apache NetBeans 27, 28, and 29 Ready:** NetBeans API dependencies updated to `RELEASE270`. Obsolete repositories were cleaned up, and transitive dependency conflicts (Guice, JSpecify) were resolved.
* **Spring Boot 4.x / Spring Framework 7.x Support:** Configuration metadata parsing libraries updated to the new standard. The Java source code was refactored to support modern Spring interfaces (`HttpStatusCode`, `UriComponentsBuilder`).
* **"Plug & Play" Installation:** The annotation scanning dependency (`Yenta`) is now repackaged internally during the build phase, eliminating the legacy "missing dependency" error when trying to install the plugin on a clean IDE.

---

## 🛠️ Key Features

1. **Spring Initializr Integration:** Bootstrap new projects directly from the IDE wizard via `File -> New Project -> Spring Boot Initializr Project`.
2. **Smart Autocompletion:** Fast, context-aware suggestions and official Spring documentation on the fly while editing your `application.properties` and `application.yml` files.
3. **Endpoint Navigator:** A dedicated side panel (Navigator) that scans your Java classes in real-time and lists all your REST endpoints (`@RequestMapping`, `@GetMapping`, etc.).

---

## 📦 How to Install in NetBeans

Currently, the fastest way to use the plugin is by manually installing the packaged `.nbm` file:

1. Go to the **[Releases](../../releases)** section of this repository and download the `nb-springboot-plugin-4.0-SNAPSHOT.nbm` file.
2. Open Apache NetBeans and go to **Tools -> Plugins**.
3. Select the **Downloaded** tab.
4. Click on **Add Plugins...** and select the `.nbm` file you just downloaded.
5. Click **Install** (you can safely accept any warnings about missing signatures) and restart the IDE. You are all set!

---

## 💻 Build from Source

If you want to tweak the code or build it yourself, make sure you have **JDK 25** (or a recent compatible version) and **Maven 3.8+** installed.

```bash
# 1. Clone the repository
git clone [https://github.com/YOUR_GITHUB_USERNAME/nb-springboot.git](https://github.com/YOUR_GITHUB_USERNAME/nb-springboot.git)

# 2. Navigate to the directory
cd nb-springboot

# 3. Build the project
mvn clean install
```

Once the process finishes (with a `BUILD SUCCESS`), the installable file will be generated at: `target/nbm/nb-springboot-plugin-4.0-SNAPSHOT.nbm`.

---

**Credits & Acknowledgments:** All credit for the original architecture, design, and core logic of this plugin goes to [Alessandro Falappa](https://github.com/AlexFalappa). This modified version is an independent effort to keep this fantastic tool alive and updated for the NetBeans community.