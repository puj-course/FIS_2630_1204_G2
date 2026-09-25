# Alineación de paquetes Java — HU-078 (#175)

**Fecha:** 23 de septiembre de 2026  
**Participantes:** _(Samuel Malaver)_  
**Depende de:** HU-077 (arquitectura JavaFX + JDBC)  
**Rama de trabajo:** personal → Pull Request hacia `develop`

---

## Objetivo

Garantizar que **cada archivo `.java` declare un `package` coherente con su ubicación** dentro de `src/main/java`, de modo que Maven, `javac` y el IDE (IntelliJ) compilen de forma consistente.

---

## Regla adoptada

```text
Ruta del archivo                          →  package declarado
src/main/java/controller/Foo.java         →  package controller;
src/main/java/entity/Bar.java             →  package entity;
src/main/java/repository/Baz.java         →  package repository;
src/main/java/service/Qux.java            →  package service;
src/main/java/exceptions/Err.java         →  package exceptions;
src/main/java/ConexionDB/ConexionBD.java  →  package ConexionDB;