// @ts-nocheck
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { UserService } from './user.service';
import { User } from '../models/user.model';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
    imports: [],
    providers: [UserService, provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
});
    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('list', () => {
    it('should GET users without params', () => {
      const mock: User[] = [
        { id: 1, username: 'u1', email: 'a@b.com', firstName: 'A', lastName: 'B', phone: '123', organizationId: 1, roles: ['SYSTEM_ADMIN'], status: 'ACTIVE', createdAt: '', updatedAt: '' }
      ];
      service.list().subscribe(data => {
        expect(data.length).toBe(1);
      });
      const req = httpMock.expectOne('/api/users');
      expect(req.request.method).toBe('GET');
      req.flush(mock);
    });

    it('should GET users with all params', () => {
      service.list(5, 'SHOP_AGENT', 'ACTIVE').subscribe();
      const req = httpMock.expectOne(r => r.url === '/api/users');
      expect(req.request.params.get('organizationId')).toBe('5');
      expect(req.request.params.get('role')).toBe('SHOP_AGENT');
      expect(req.request.params.get('statusFilter')).toBe('ACTIVE');
      req.flush([]);
    });

    it('should omit undefined params', () => {
      service.list(undefined, 'SYSTEM_ADMIN').subscribe();
      const req = httpMock.expectOne(r => r.url === '/api/users');
      expect(req.request.params.has('organizationId')).toBeFalse();
      expect(req.request.params.get('role')).toBe('SYSTEM_ADMIN');
      req.flush([]);
    });
  });

  describe('getById', () => {
    it('should GET user by id', () => {
      const mock: User = { id: 1, username: 'u1', email: 'a@b.com', firstName: 'A', lastName: 'B', phone: '123', organizationId: 1, roles: ['SYSTEM_ADMIN'], status: 'ACTIVE', createdAt: '', updatedAt: '' };
      service.getById(1).subscribe(data => {
        expect(data.id).toBe(1);
      });
      const req = httpMock.expectOne('/api/users/1');
      expect(req.request.method).toBe('GET');
      req.flush(mock);
    });
  });

  describe('create', () => {
    it('should POST to create user', () => {
      const mock: User = { id: 2, username: 'new', email: 'n@b.com', firstName: 'N', lastName: 'W', phone: '456', organizationId: 2, roles: ['SHOP_AGENT'], status: 'ACTIVE', createdAt: '', updatedAt: '' };
      service.create({ username: 'new', email: 'n@b.com', role: 'SHOP_AGENT' }).subscribe(data => {
        expect(data.username).toBe('new');
      });
      const req = httpMock.expectOne('/api/users');
      expect(req.request.method).toBe('POST');
      req.flush(mock);
    });
  });

  describe('activate', () => {
    it('should PATCH to activate user', () => {
      const mock: User = { id: 1, username: 'u1', email: 'a@b.com', firstName: 'A', lastName: 'B', phone: '123', organizationId: 1, roles: ['SHOP_AGENT'], status: 'ACTIVE', createdAt: '', updatedAt: '' };
      service.activate(1).subscribe(data => {
        expect(data.status).toBe('ACTIVE');
      });
      const req = httpMock.expectOne('/api/users/1/activate');
      expect(req.request.method).toBe('PATCH');
      req.flush(mock);
    });
  });

  describe('disable', () => {
    it('should PATCH to disable user', () => {
      const mock: User = { id: 1, username: 'u1', email: 'a@b.com', firstName: 'A', lastName: 'B', phone: '123', organizationId: 1, roles: ['SHOP_AGENT'], status: 'DISABLED', createdAt: '', updatedAt: '' };
      service.disable(1).subscribe(data => {
        expect(data.status).toBe('DISABLED');
      });
      const req = httpMock.expectOne('/api/users/1/disable');
      expect(req.request.method).toBe('PATCH');
      req.flush(mock);
    });
  });
});
