import { test, expect, APIRequestContext } from '@playwright/test';
import { randomUUID } from 'crypto';

// ─── helpers ────────────────────────────────────────────────────────────────

function uniqueEmail() {
  return `test-${randomUUID()}@covoit.test`;
}

async function register(request: APIRequestContext, email: string, password: string) {
  return request.post('/api/auth/register', {
    data: {
      email,
      password,
      displayName: 'Test User',
      phoneDialCode: '+33',
      phoneNumber: '612345678',
    },
  });
}

async function login(request: APIRequestContext, email: string, password: string) {
  return request.post('/api/auth/login', {
    data: { email, password },
  });
}

// ─── register ───────────────────────────────────────────────────────────────

test.describe('POST /api/auth/register', () => {

  test('inscrit un nouvel utilisateur et retourne un token', async ({ request }) => {
    const email = uniqueEmail();
    const res = await register(request, email, 'MotDePasseSecure123!');

    expect(res.status()).toBe(201);
    const body = await res.json();
    expect(body).toHaveProperty('accessToken');
    expect(body).toHaveProperty('refreshToken');
    expect(typeof body.accessToken).toBe('string');
    expect(body.accessToken.length).toBeGreaterThan(0);
  });

  test('rejette un email invalide', async ({ request }) => {
    const res = await register(request, 'pas-un-email', 'MotDePasseSecure123!');
    expect(res.status()).toBe(400);
  });

  test('rejette un mot de passe trop court (< 12 caractères)', async ({ request }) => {
    const res = await register(request, uniqueEmail(), 'court');
    expect(res.status()).toBe(400);
  });

  test('rejette un email déjà utilisé', async ({ request }) => {
    const email = uniqueEmail();
    await register(request, email, 'MotDePasseSecure123!');
    const res = await register(request, email, 'MotDePasseSecure123!');
    expect(res.status()).toBe(409);
  });

  test('rejette un body vide', async ({ request }) => {
    const res = await request.post('/api/auth/register', { data: {} });
    expect(res.status()).toBe(400);
  });

});

// ─── login ──────────────────────────────────────────────────────────────────

test.describe('POST /api/auth/login', () => {

  test('connecte un utilisateur existant et retourne un token', async ({ request }) => {
    const email = uniqueEmail();
    const password = 'MotDePasseSecure123!';
    await register(request, email, password);

    const res = await login(request, email, password);
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body).toHaveProperty('accessToken');
    expect(body).toHaveProperty('refreshToken');
  });

  test('rejette un mauvais mot de passe', async ({ request }) => {
    const email = uniqueEmail();
    await register(request, email, 'MotDePasseSecure123!');

    const res = await login(request, email, 'mauvaisMotDePasse!');
    expect(res.status()).toBe(401);
  });

  test('rejette un email inconnu', async ({ request }) => {
    const res = await login(request, 'inconnu@covoit.test', 'MotDePasseSecure123!');
    expect(res.status()).toBe(401);
  });

  test('rejette un body vide', async ({ request }) => {
    const res = await request.post('/api/auth/login', { data: {} });
    expect(res.status()).toBe(400);
  });

});

// ─── refresh ─────────────────────────────────────────────────────────────────

test.describe('POST /api/auth/refresh', () => {

  test('retourne un nouveau token à partir du refreshToken', async ({ request }) => {
    const email = uniqueEmail();
    const password = 'MotDePasseSecure123!';
    const registerRes = await register(request, email, password);
    const { refreshToken } = await registerRes.json();

    const res = await request.post('/api/auth/refresh', {
      data: { refreshToken },
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body).toHaveProperty('accessToken');
    expect(body).toHaveProperty('refreshToken');
    // le nouveau refreshToken doit être différent (rotation)
    expect(body.refreshToken).not.toBe(refreshToken);
  });

  test('rejette un refreshToken invalide', async ({ request }) => {
    const res = await request.post('/api/auth/refresh', {
      data: { refreshToken: 'token-bidon' },
    });
    expect(res.status()).toBe(401);
  });

});

// ─── logout ──────────────────────────────────────────────────────────────────

test.describe('POST /api/auth/logout', () => {

  test('déconnecte et invalide le refreshToken', async ({ request }) => {
    const email = uniqueEmail();
    const password = 'MotDePasseSecure123!';
    const registerRes = await register(request, email, password);
    const { accessToken, refreshToken } = await registerRes.json();

    const logoutRes = await request.post('/api/auth/logout', {
      headers: { Authorization: `Bearer ${accessToken}` },
      data: { refreshToken },
    });
    expect(logoutRes.status()).toBe(204);

    // le refreshToken ne doit plus fonctionner
    const refreshRes = await request.post('/api/auth/refresh', {
      data: { refreshToken },
    });
    expect(refreshRes.status()).toBe(401);
  });

});

// ─── /me ─────────────────────────────────────────────────────────────────────

test.describe('GET /api/me', () => {

  test('retourne le profil avec un accessToken valide', async ({ request }) => {
    const email = uniqueEmail();
    const password = 'MotDePasseSecure123!';
    const registerRes = await register(request, email, password);
    const { accessToken } = await registerRes.json();

    const res = await request.get('/api/me', {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.email).toBe(email);
  });

  test('rejette une requête sans token', async ({ request }) => {
    const res = await request.get('/api/me');
    expect(res.status()).toBe(401);
  });

  test('rejette un token invalide', async ({ request }) => {
    const res = await request.get('/api/me', {
      headers: { Authorization: 'Bearer token-bidon' },
    });
    expect(res.status()).toBe(401);
  });

});
