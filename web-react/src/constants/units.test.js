import { describe, it, expect } from 'vitest';
import {
  KG_PER_LB,
  CM_PER_IN,
  kgToDisplay,
  displayToKg,
  kgToLb,
  lbToKg,
  cmToFeetInches,
  feetInchesToCm,
} from './units.js';

describe('conversion factors', () => {
  it('are the canonical values', () => {
    expect(KG_PER_LB).toBeCloseTo(0.45359237, 8);
    expect(CM_PER_IN).toBe(2.54);
  });
});

describe('kgToDisplay', () => {
  it('returns "" for unset or zero weight', () => {
    expect(kgToDisplay(null, false)).toBe('');
    expect(kgToDisplay(undefined, false)).toBe('');
    expect(kgToDisplay(0, false)).toBe('');
    expect(kgToDisplay(0, true)).toBe('');
  });

  it('shows the exact stored value in metric', () => {
    expect(kgToDisplay(100, false)).toBe('100');
    expect(kgToDisplay(72.5, false)).toBe('72.5');
  });

  it('converts to pounds rounded to one decimal in imperial', () => {
    // 100 kg -> 220.462... lb -> 220.5
    expect(kgToDisplay(100, true)).toBe('220.5');
  });

  it('lets half-pound values survive the kg round-trip', () => {
    const kg = displayToKg('17.5', true);
    expect(kgToDisplay(kg, true)).toBe('17.5');
  });
});

describe('displayToKg', () => {
  it('treats blank input as zero', () => {
    expect(displayToKg('', false)).toBe(0);
    expect(displayToKg('   ', true)).toBe(0);
  });

  it('returns 0 for non-numeric input', () => {
    expect(displayToKg('abc', false)).toBe(0);
  });

  it('passes metric values straight through', () => {
    expect(displayToKg('100', false)).toBe(100);
    expect(displayToKg('72.5', false)).toBe(72.5);
  });

  it('converts pounds to kg with 2-decimal precision in imperial', () => {
    expect(displayToKg('220.5', true)).toBe(100.02);
    expect(displayToKg('45', true)).toBe(20.41);
  });
});

describe('body weight helpers (whole numbers)', () => {
  it('kgToLb rounds to whole pounds and returns "" for unset', () => {
    expect(kgToLb('')).toBe('');
    expect(kgToLb(null)).toBe('');
    expect(kgToLb(100)).toBe(220);
    expect(kgToLb(70)).toBe(154);
  });

  it('lbToKg rounds to whole kilograms', () => {
    expect(lbToKg(220)).toBe(100);
    expect(lbToKg(154)).toBe(70);
    expect(lbToKg(0)).toBe(0);
  });
});

describe('height helpers', () => {
  it('returns empty parts for unset height', () => {
    expect(cmToFeetInches(null)).toEqual({ feet: '', inches: '' });
    expect(cmToFeetInches('')).toEqual({ feet: '', inches: '' });
  });

  it('rounds to whole inches first so 30cm is 1ft 0in, not 0ft 12in', () => {
    expect(cmToFeetInches(30)).toEqual({ feet: 1, inches: 0 });
  });

  it('converts common heights', () => {
    expect(cmToFeetInches(180)).toEqual({ feet: 5, inches: 11 });
    expect(cmToFeetInches(165)).toEqual({ feet: 5, inches: 5 });
  });

  it('feetInchesToCm returns null for 0ft 0in', () => {
    expect(feetInchesToCm(0, 0)).toBeNull();
    expect(feetInchesToCm('', '')).toBeNull();
  });

  it('feetInchesToCm converts common heights', () => {
    expect(feetInchesToCm(5, 11)).toBe(180);
    expect(feetInchesToCm(6, 0)).toBe(183);
    expect(feetInchesToCm(5, 5)).toBe(165);
  });
});
