import { request } from './api';
import type { ApiKeyInfo, CreatedApiKey } from '../types';

const BASE = '/api/admin/api-keys';

/** Portées connues côté serveur (cf. ApiKeyScopes). */
export const API_KEY_SCOPES = ['cvc-gaines'] as const;

export interface CreateApiKeyInput {
  name: string;
  scopes: string[];
  /** ISO-8601 (UTC), ou absent pour une clé sans expiration. */
  expiresAt?: string;
}

export async function listApiKeys(): Promise<ApiKeyInfo[]> {
  return request<ApiKeyInfo[]>(BASE);
}

export async function createApiKey(input: CreateApiKeyInput): Promise<CreatedApiKey> {
  return request<CreatedApiKey>(BASE, {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export async function revokeApiKey(id: string): Promise<void> {
  await request<void>(`${BASE}/${encodeURIComponent(id)}`, { method: 'DELETE' });
}
