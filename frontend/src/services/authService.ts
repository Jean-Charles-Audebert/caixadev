import { request } from './api';
import type { UserInfo } from '../types';

export async function login(email: string, password: string): Promise<UserInfo> {
  return request<UserInfo>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
}

export async function logout(): Promise<void> {
  await request<void>('/api/auth/logout', { method: 'POST' });
}

export async function getMe(): Promise<UserInfo> {
  return request<UserInfo>('/api/users/me');
}

