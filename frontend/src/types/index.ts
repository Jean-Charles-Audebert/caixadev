export interface UserInfo {
  id: number;
  email: string;
  displayName: string | null;
  /** Affichage uniquement : les droits sont vérifiés côté serveur. */
  superAdmin: boolean;
}

export type ApiKeyStatus = 'ACTIVE' | 'EXPIRED' | 'REVOKED';

export interface ApiKeyInfo {
  id: string;
  name: string;
  keyPrefix: string;
  scopes: string[];
  createdBy: string;
  createdAt: string;
  expiresAt: string | null;
  lastUsedAt: string | null;
  revokedAt: string | null;
  status: ApiKeyStatus;
}

/** Réponse de création : `key` n'est transmise qu'une seule fois. */
export interface CreatedApiKey {
  apiKey: ApiKeyInfo;
  key: string;
}

export interface ProjectInfo {
  id: number;
  key: string;
  name: string;
  description: string | null;
  isActive: boolean;
  isPublic: boolean;
  roles: string[];
}

export interface ApiError {
  error: string;
  message: string;
  timestamp: string;
}

