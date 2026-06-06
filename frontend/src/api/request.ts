import type { Result } from '@/types';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  });
  const result = (await response.json()) as Result<T>;
  if (!result.success) {
    throw new Error(result.message || '请求失败');
  }
  return result.data;
}

export async function post<T>(path: string, body: unknown): Promise<T> {
  return request<T>(path, {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export async function get<T>(path: string): Promise<T> {
  return request<T>(path, { method: 'GET' });
}
