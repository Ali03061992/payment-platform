export interface User {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  phone: string;
  organizationId: string;
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
  refreshToken?: string;
  refreshExpiresIn?: number;
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

/** Page d'utilisateurs telle que retournée par GET /api/users (page/size + filtres). */
export interface UserPage {
  items: User[];
  totalElements: number;
  totalPages: number;
  number: number;
}
