import { describe, it, expect } from "vitest";
import i18n from "./config";
import { normalizeLocale } from "./locales";

// Known catalog values:
//   login.button  en = "Log in", es-419 = "Iniciar sesión"  (es-AR has no override)
//   login.title   es-419 (tuteo) and es-AR (voseo) differ
const T = (lng) => i18n.getFixedT(lng);

describe("Spanish locale resolution (es -> es-419 rename)", () => {
  it("es-419 renders Latin American Spanish", () => {
    expect(T("es-419")("login.button")).toBe("Iniciar sesión");
  });

  it("plain es aliases to es-419", () => {
    expect(T("es")("login.button")).toBe("Iniciar sesión");
  });

  it("es-XX browser regions fall back to es-419, not English", () => {
    expect(T("es-MX")("login.button")).toBe("Iniciar sesión");
    expect(T("es-CL")("login.button")).toBe("Iniciar sesión");
  });

  it("es-AR falls back to es-419 for keys it does not override", () => {
    expect(T("es-AR")("login.button")).toBe("Iniciar sesión");
  });

  it("es-AR uses its own voseo override where present", () => {
    expect(T("es-AR")("login.title")).toBe(
      "Iniciá sesión y registrá tu próximo entrenamiento"
    );
    expect(T("es-419")("login.title")).toBe(
      "Inicia sesión y registra tu próximo entrenamiento"
    );
  });

  it("English is unchanged", () => {
    expect(T("en")("login.button")).toBe("Log in");
  });
});

describe("normalizeLocale", () => {
  it("maps legacy es and es-XX regions to es-419", () => {
    expect(normalizeLocale("es")).toBe("es-419");
    expect(normalizeLocale("es-MX")).toBe("es-419");
    expect(normalizeLocale("es-CL")).toBe("es-419");
    expect(normalizeLocale("es-ES")).toBe("es-419");
  });
  it("keeps es-AR and es-419 as themselves", () => {
    expect(normalizeLocale("es-AR")).toBe("es-AR");
    expect(normalizeLocale("es-419")).toBe("es-419");
  });
  it("passes other locales through and defaults empty to en", () => {
    expect(normalizeLocale("en")).toBe("en");
    expect(normalizeLocale("fr")).toBe("fr");
    expect(normalizeLocale("")).toBe("en");
    expect(normalizeLocale(undefined)).toBe("en");
  });
});
