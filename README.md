# Java Foundry

A hub for small, self-contained Java and Spring Boot projects built to showcase backend engineering concepts.

> Every project follows a consistent Maven project structure and is independently runnable.

## Project structure

```text
JavaFoundry/
├── index.html
├── style.css
├── script.js
├── projects/
│   ├── _template/
│   │   ├── pom.xml
│   │   ├── README.md
│   │   └── src/
│   │       ├── main/java/com/buildtheportfolio/app/Application.java
│   │       └── main/resources/application.yml
│   └── ...
└── Ideas.md
```

The hub discovers project folders under `projects/` automatically. A directory is included in the project catalog only when it contains a `pom.xml`; `_template` is always excluded.

## Add a project

1. Copy `projects/_template/` to a new folder.
2. Update the Maven coordinates and application package.
3. Implement the project from `Ideas.md`.
4. Run `mvn spring-boot:run` from the project directory.
5. Push the folder to GitHub.
6. The project is automatically discovered by the hub without editing a registry.

## Project rules

- One project = one folder.
- Maven is the standard build system.
- Java 21 and Spring Boot are the baseline for backend projects.
- Keep projects independently runnable.
- Keep source code free of comments.
- Deployment is manual.
- No GitHub Actions are required.
