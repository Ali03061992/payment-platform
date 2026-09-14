import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { UserManagementComponent } from './user-management.component';
import { UserService } from '../../services/user.service';
import { ToastService } from '../../services/toast.service';
import { User } from '../../models/user.model';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('UserManagementComponent', () => {
  let component: UserManagementComponent;
  let fixture: ComponentFixture<UserManagementComponent>;
  let userService: jasmine.SpyObj<UserService>;
  let toast: jasmine.SpyObj<ToastService>;

  beforeEach(() => {
    const userSpy = jasmine.createSpyObj('UserService', ['list', 'activate', 'disable']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    userSpy.list.and.returnValue(of([]));

    TestBed.configureTestingModule({
    declarations: [UserManagementComponent],
    imports: [FormsModule],
    providers: [
        { provide: UserService, useValue: userSpy },
        { provide: ToastService, useValue: toastSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
});
    fixture = TestBed.createComponent(UserManagementComponent);
    component = fixture.componentInstance;
    userService = TestBed.inject(UserService) as jasmine.SpyObj<UserService>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have initial state', () => {
    expect(component.users).toEqual([]);
    expect(component.loading).toBeTrue();
  });

  describe('loadUsers', () => {
    it('should load users', () => {
      const mockUsers: User[] = [
        { id: 1, username: 'u1', email: 'a@b.com', firstName: 'A', lastName: 'B', phone: '123', organizationId: 1, roles: ['SYSTEM_ADMIN'], status: 'ACTIVE', createdAt: '', updatedAt: '' }
      ];
      userService.list.and.returnValue(of(mockUsers));
      component.loadUsers();
      expect(component.users.length).toBe(1);
      expect(component.loading).toBeFalse();
    });

    it('should handle error', () => {
      userService.list.and.returnValue(throwError(() => ({ error: { message: 'Load failed' } })));
      component.loadUsers();
      expect(component.error).toBe('Load failed');
      expect(component.loading).toBeFalse();
    });
  });

  describe('activate', () => {
    it('should activate user', () => {
      const user: User = { id: 1, username: 'u1', email: '', firstName: '', lastName: '', phone: '', organizationId: 1, roles: [], status: 'DISABLED', createdAt: '', updatedAt: '' };
      userService.activate.and.returnValue(of({ ...user, status: 'ACTIVE' } as any));
      component.activate(user);
      expect(user.status).toBe('ACTIVE');
      expect(toast.success).toHaveBeenCalled();
    });

    it('should handle activation error', () => {
      const user: User = { id: 1, username: 'u1', email: '', firstName: '', lastName: '', phone: '', organizationId: 1, roles: [], status: 'DISABLED', createdAt: '', updatedAt: '' };
      userService.activate.and.returnValue(throwError(() => ({ error: { message: 'Fail' } })));
      component.activate(user);
      expect(toast.error).toHaveBeenCalled();
    });
  });

  describe('disable', () => {
    it('should disable user', () => {
      const user: User = { id: 1, username: 'u1', email: '', firstName: '', lastName: '', phone: '', organizationId: 1, roles: [], status: 'ACTIVE', createdAt: '', updatedAt: '' };
      userService.disable.and.returnValue(of({ ...user, status: 'DISABLED' } as any));
      component.disable(user);
      expect(user.status).toBe('DISABLED');
      expect(toast.success).toHaveBeenCalled();
    });

    it('should handle disable error', () => {
      const user: User = { id: 1, username: 'u1', email: '', firstName: '', lastName: '', phone: '', organizationId: 1, roles: [], status: 'ACTIVE', createdAt: '', updatedAt: '' };
      userService.disable.and.returnValue(throwError(() => ({ error: { message: 'Fail' } })));
      component.disable(user);
      expect(toast.error).toHaveBeenCalled();
    });
  });

  describe('applyFilter', () => {
    it('should reload users', () => {
      component.applyFilter();
      expect(userService.list).toHaveBeenCalled();
    });
  });
});
