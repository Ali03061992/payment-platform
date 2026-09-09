import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { User } from '../models/user.model';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('register', () => {
    it('should POST to register', () => {
      const mock: User = {
        id: 1, username: 'newuser', email: 'n@b.com', firstName: 'New', lastName: 'User',
        phone: '123', organizationId: 1, roles: ['SHOP_AGENT'], status: 'ACTIVE', createdAt: '', updatedAt: ''
      };
      service.register({
        username: 'newuser', email: 'n@b.com', password: 'pass123',
        firstName: 'New', lastName: 'User', phone: '123', role: 'SHOP_AGENT'
      }).subscribe(data => {
        expect(data.username).toBe('newuser');
      });
      const req = httpMock.expectOne('/api/auth/register');
      expect(req.request.method).toBe('POST');
      req.flush(mock);
    });
  });
});
