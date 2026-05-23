class ApiException extends Error {
  constructor(
    public readonly status: number,
    message: string
  ) {
    super(message);
  }
}

let isRefreshing = false;

async function tryRefresh(): Promise<boolean> {
  if (isRefreshing) return false;
  isRefreshing = true;
  try {
    const res = await fetch('/api/auth/refresh', { method: 'POST', credentials: 'include' });
    return res.ok;
  } catch {
    return false;
  } finally {
    isRefreshing = false;
  }
}

export async function request<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const res = await fetch(path, {
    ...options,
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...options.headers },
  });

  if (res.status === 401 && !path.includes('/auth/')) {
    const refreshed = await tryRefresh();
    if (refreshed) {
      const retryRes = await fetch(path, {
        ...options,
        credentials: 'include',
        headers: { 'Content-Type': 'application/json', ...options.headers },
      });
      if (retryRes.ok) {
        if (retryRes.status === 204) return null as T;
        return retryRes.json();
      }
    }
    throw new ApiException(401, 'Session expirée');
  }

  if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new ApiException(res.status, body?.message ?? 'Erreur serveur');
  }

  if (res.status === 204) return null as T;
  return res.json();
}

