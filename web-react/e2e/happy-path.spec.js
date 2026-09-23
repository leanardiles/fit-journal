import { test, expect, request } from '@playwright/test';

// Backend API base (dev). Override with E2E_API_URL if yours differs.
const API_URL = process.env.E2E_API_URL || 'http://localhost:8000/v1';

// A throwaway user, unique per run, keeps the test self-contained.
// example.com is a real, validator-safe domain (unlike reserved .test).
const TEST_EMAIL = `e2e.${Date.now()}@example.com`;
const TEST_PASSWORD = 'E2ePassw0rd!';

let authToken = '';
let authUserId = '';

// Seed the user straight through the API so the UI login test has an account,
// and capture a token to preset the session for the page-render walk.
test.beforeAll(async () => {
  const api = await request.newContext();

  const reg = await api.post(`${API_URL}/register`, {
    data: { user_email: TEST_EMAIL, user_password: TEST_PASSWORD },
  });
  expect(reg.ok(), `register failed (${reg.status()}): ${await reg.text()}`).toBeTruthy();

  const login = await api.post(`${API_URL}/login`, {
    data: { user_email: TEST_EMAIL, user_password: TEST_PASSWORD },
  });
  expect(login.ok(), `login failed (${login.status()}): ${await login.text()}`).toBeTruthy();

  const data = await login.json();
  authToken = data.access_token;
  authUserId = data.user_id;

  await api.dispose();
});

test('logs in through the UI and reaches the dashboard', async ({ page }) => {
  await page.goto('/login');
  await page.locator('input[type="email"]').fill(TEST_EMAIL);
  await page.locator('input[type="password"]').first().fill(TEST_PASSWORD);
  await page.locator('.auth-line--action button').click();

  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.locator('.app-shell')).toBeVisible();
  await expect(page.locator('.dash-title')).toBeVisible();
});

test('every protected page renders inside the app shell', async ({ page, context }) => {
  // Preset the session so the route guard passes, then walk the app.
  await context.addInitScript(
    ([token, userId]) => {
      window.localStorage.setItem('token', token);
      window.localStorage.setItem('user_id', String(userId));
    },
    [authToken, authUserId],
  );

  const routes = ['/dashboard', '/profile', '/routine', '/exercises', '/calendar', '/workout'];
  for (const route of routes) {
    await page.goto(route);
    await expect(page).toHaveURL(new RegExp(`${route}$`));
    await expect(page.locator('.app-shell')).toBeVisible();
  }
});
