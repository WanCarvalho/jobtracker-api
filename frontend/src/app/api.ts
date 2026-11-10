import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, firstValueFrom, throwError } from 'rxjs';

export interface Session { token: string; userId: number; name: string; email: string; role: string }
export interface Company { id: number; name: string; approvalStatus: string }
export interface Stage { id: number; name: string; color: string; isFinal: boolean; isActive: boolean }
export interface Application { id: number; jobTitle: string; companyName: string; stageName: string; stageId: number; appliedAt: string }
export interface Summary { totalApplications: number; activeApplications: number; finalApplications: number }

@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);
  readonly session = signal<Session | null>(this.restore());
  private restore(): Session | null {
    try {
      const value = JSON.parse(sessionStorage.getItem('jobtracker.session') ?? 'null');
      return value && typeof value.token === 'string' ? value : null;
    } catch { return null; }
  }
  async authenticate(register: boolean, credentials: { name: string; email: string; password: string }) {
    const session = await this.post<Session>(`/auth/${register ? 'register' : 'login'}`, credentials);
    sessionStorage.setItem('jobtracker.session', JSON.stringify(session));
    this.session.set(session);
  }
  logout() { sessionStorage.removeItem('jobtracker.session'); this.session.set(null); }
  get<T>(path: string) { return firstValueFrom(this.http.get<T>(`/api${path}`)); }
  post<T>(path: string, body: unknown) { return firstValueFrom(this.http.post<T>(`/api${path}`, body)); }
  patch<T>(path: string, body: unknown) { return firstValueFrom(this.http.patch<T>(`/api${path}`, body)); }
}

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const api = inject(Api);
  const isApi = request.url.startsWith('/api/');
  const isAuth = request.url === '/api/auth/login' || request.url === '/api/auth/register';
  const token = api.session()?.token;
  return next(isApi && !isAuth && token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request)
    .pipe(catchError((error: HttpErrorResponse) => {
      if (isApi && !isAuth && error.status === 401) api.logout();
      return throwError(() => error);
    }));
};
