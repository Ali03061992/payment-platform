import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { AccountActivationComponent } from './account-activation.component';
import { UserService } from '../../services/user.service';
import { ToastService } from '../../services/toast.service';
import { User } from '../../models/user.model';

describe('AccountActivationComponent', () => {
  let component: AccountActivationComponent;
  let fixture: ComponentFixture<AccountActivationComponent>;
  let userService: jasmine.SpyObj<UserService>;
  let toast: jasmine.SpyObj<ToastService>;

  const mockUsers: User[] = [
    { id: 1, username: 'admin', email: 'a@b.com', firstName: 'Admin', lastName: 'User', phone: '123', organizationId: 1, roles: ['SYSTEM_ADMIN'], status: 'ACTIVE', createdAt: '', updatedAt: '' },
    { id: 2, username: 'agent1', email: 'b@b.com', firstName: 'Agent', lastName: 'One', phone: '456', organizationId: 2, roles: ['SUPPLIER_AGENT'], status: 'DISABLED', createdAt: '', updatedAt: '' }
  ];

  beforeEach(() => {
    const userSpy = jasmine.createSpyObj('UserService', ['list', 'activate', 'disable']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    userSpy.list.and.returnValue(of(mockUsers));

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, FormsModule],
      declarations: [AccountActivationComponent],
      providers: [
        { provide: UserService, useValue: userSpy },
        { provide: ToastService, useValue: toastSpy }
      ]
    });
    fixture = TestBed.createComponent(AccountActivationComponent);
    component = fixture.componentInstance;
    userService = TestBed.inject(UserService) as jasmine.SpyObj<UserService>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should call loadUsers on init', () => {
      component.ngOnInit();
      expect(userService.list).toHaveBeenCalled();
      expect(component.users.length).toBe(2);
      expect(component.loading).toBeFalse();
    });
  });

  describe('loadUsers', () => {
    it('should load users successfully', () => {
      component.loadUsers();
      expect(component.users.length).toBe(2);
      expect(component.loading).toBeFalse();
    });

    it('should handle error when loading users', () => {
      userService.list.and.returnValue(throwError(() => ({ error: { message: 'Load failed' } })));
      component.loadUsers();
      expect(toast.error).toHaveBeenCalledWith('Load failed');
      expect(component.loading).toBeFalse();
    });

    it('should handle error without message', () => {
      userService.list.and.returnValue(throwError(() => ({ error: {} })));
      component.loadUsers();
      expect(toast.error).toHaveBeenCalledWith('Erreur de chargement');
      expect(component.loading).toBeFalse();
    });
  });

  describe('filteredUsers', () => {
    it('should return all users when no filters', () => {
      component.users = mockUsers;
      component.searchQuery = '';
      component.filterStatus = '';
      expect(component.filteredUsers.length).toBe(2);
    });

    it('should filter by search query on username', () => {
      component.users = mockUsers;
      component.searchQuery = 'admin';
      expect(component.filteredUsers.length).toBe(1);
      expect(component.filteredUsers[0].username).toBe('admin');
    });

    it('should filter by search query on firstName', () => {
      component.users = mockUsers;
      component.searchQuery = 'Agent';
      expect(component.filteredUsers.length).toBe(1);
      expect(component.filteredUsers[0].firstName).toBe('Agent');
    });

    it('should filter by search query on lastName', () => {
      component.users = mockUsers;
      component.searchQuery = 'One';
      expect(component.filteredUsers.length).toBe(1);
    });

    it('should filter by search query on email', () => {
      component.users = mockUsers;
      component.searchQuery = 'b@b.com';
      expect(component.filteredUsers.length).toBe(1);
    });

    it('should filter by status', () => {
      component.users = mockUsers;
      component.searchQuery = '';
      component.filterStatus = 'ACTIVE';
      expect(component.filteredUsers.length).toBe(1);
      expect(component.filteredUsers[0].status).toBe('ACTIVE');
    });

    it('should combine search and status filters', () => {
      component.users = mockUsers;
      component.searchQuery = 'admin';
      component.filterStatus = 'ACTIVE';
      expect(component.filteredUsers.length).toBe(1);
    });

    it('should return empty when no match', () => {
      component.users = mockUsers;
      component.searchQuery = 'nonexistent';
      expect(component.filteredUsers.length).toBe(0);
    });

    it('should be case insensitive', () => {
      component.users = mockUsers;
      component.searchQuery = 'ADMIN';
      expect(component.filteredUsers.length).toBe(1);
    });
  });

  describe('activate', () => {
    it('should activate user successfully', () => {
      const user = { ...mockUsers[1] };
      userService.activate.and.returnValue(of({ ...user, status: 'ACTIVE' } as any));
      component.activate(user);
      expect(user.status).toBe('ACTIVE');
      expect(toast.success).toHaveBeenCalledWith('agent1 activé avec succès');
    });

    it('should handle activation error', () => {
      const user = { ...mockUsers[1] };
      userService.activate.and.returnValue(throwError(() => ({ error: { message: 'Activation failed' } })));
      component.activate(user);
      expect(toast.error).toHaveBeenCalledWith('Activation failed');
    });

    it('should handle activation error without message', () => {
      const user = { ...mockUsers[1] };
      userService.activate.and.returnValue(throwError(() => ({ error: {} })));
      component.activate(user);
      expect(toast.error).toHaveBeenCalledWith('Erreur');
    });
  });

  describe('disable', () => {
    it('should disable user successfully', () => {
      const user = { ...mockUsers[0] };
      userService.disable.and.returnValue(of({ ...user, status: 'DISABLED' } as any));
      component.disable(user);
      expect(user.status).toBe('DISABLED');
      expect(toast.success).toHaveBeenCalledWith('admin désactivé avec succès');
    });

    it('should handle disable error', () => {
      const user = { ...mockUsers[0] };
      userService.disable.and.returnValue(throwError(() => ({ error: { message: 'Disable failed' } })));
      component.disable(user);
      expect(toast.error).toHaveBeenCalledWith('Disable failed');
    });

    it('should handle disable error without message', () => {
      const user = { ...mockUsers[0] };
      userService.disable.and.returnValue(throwError(() => ({ error: {} })));
      component.disable(user);
      expect(toast.error).toHaveBeenCalledWith('Erreur');
    });
  });
});
