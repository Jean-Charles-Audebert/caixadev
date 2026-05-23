export interface UserInfo {
  id: number;
  email: string;
  displayName: string | null;
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

