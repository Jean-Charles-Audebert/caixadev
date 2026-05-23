import { request } from './api';
import type { ProjectInfo } from '../types';

export async function getPublicProjects(): Promise<ProjectInfo[]> {
  return request<ProjectInfo[]>('/api/projects/public');
}

export async function getMyProjects(): Promise<ProjectInfo[]> {
  return request<ProjectInfo[]>('/api/users/me/projects');
}

