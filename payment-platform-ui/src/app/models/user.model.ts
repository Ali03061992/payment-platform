export interface User {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  phone: string;
  organizationId: number;
  roles: string[];
  status: 'ACTIVE' | 'DISABLED';
  createdAt: string;
  updatedAt: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone: string;
  role: string;
}
