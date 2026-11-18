import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';

const ACCESS_KEY = 'library_access_token';
const REFRESH_KEY = 'library_refresh_token';

export type ApiResponse<T> = {
  success: boolean;
  data: T;
  message?: string;
  code?: string;
  errors?: unknown;
  meta?: unknown;
};

export type AuthUser = {
  id: number;
  email: string;
  phone?: string | null;
  fullName: string;
  avatarUrl?: string | null;
  role: 'ADMIN' | 'STAFF' | 'USER';
  status: string;
  emailVerifiedAt?: string | null;
  lastLoginAt?: string | null;
};

export type TokenPayload = {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
  user: AuthUser;
};

export const tokenStore = {
  getAccess: () => localStorage.getItem(ACCESS_KEY),
  getRefresh: () => localStorage.getItem(REFRESH_KEY),
  set: (access: string, refresh: string) => {
    localStorage.setItem(ACCESS_KEY, access);
    localStorage.setItem(REFRESH_KEY, refresh);
  },
  clear: () => {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
  },
};

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 20000,
});

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenStore.getAccess();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let refreshing: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = tokenStore.getRefresh();
  if (!refreshToken) return null;
  try {
    const { data } = await axios.post<ApiResponse<TokenPayload>>(
      `${import.meta.env.VITE_API_BASE_URL || '/api/v1'}/auth/refresh`,
      { refreshToken },
    );
    if (!data.success || !data.data) return null;
    tokenStore.set(data.data.accessToken, data.data.refreshToken);
    return data.data.accessToken;
  } catch {
    tokenStore.clear();
    return null;
  }
}

api.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const original = error.config as InternalAxiosRequestConfig & { _retry?: boolean };
    if (error.response?.status === 401 && original && !original._retry) {
      original._retry = true;
      refreshing = refreshing ?? refreshAccessToken().finally(() => {
        refreshing = null;
      });
      const newToken = await refreshing;
      if (newToken) {
        original.headers.Authorization = `Bearer ${newToken}`;
        return api(original);
      }
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  },
);

export const authApi = {
  login: (username: string, password: string) =>
    api.post<ApiResponse<TokenPayload>>('/auth/login', { username, password }),
  me: () => api.get<ApiResponse<AuthUser>>('/auth/me'),
  logout: (refreshToken?: string | null) =>
    api.post<ApiResponse<null>>('/auth/logout', { refreshToken: refreshToken ?? undefined }),
};
