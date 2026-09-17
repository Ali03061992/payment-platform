// @ts-nocheck
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SupplierAgentService, SupplierAgent } from './supplier-agent.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

const mockAgent: SupplierAgent = {
  id: 1, username: 'agent1', firstName: 'Agent', lastName: 'One',
  email: 'agent@sup.com', phone: '123', status: 'ACTIVE', organizationId: 1
};

describe('SupplierAgentService', () => {
  let service: SupplierAgentService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
    imports: [],
    providers: [SupplierAgentService, provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
});
    service = TestBed.inject(SupplierAgentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('listAgents', () => {
    it('should GET agents for supplier', () => {
      service.listAgents(1).subscribe(data => {
        expect(data.length).toBe(1);
      });
      const req = httpMock.expectOne('/api/suppliers/1/agents');
      expect(req.request.method).toBe('GET');
      req.flush([mockAgent]);
    });
  });

  describe('createAgent', () => {
    it('should POST to create agent', () => {
      service.createAgent(1, { username: 'agent1', firstName: 'Agent', lastName: 'One', email: 'a@b.com' }).subscribe(data => {
        expect(data.username).toBe('agent1');
      });
      const req = httpMock.expectOne('/api/suppliers/1/agents');
      expect(req.request.method).toBe('POST');
      req.flush(mockAgent);
    });
  });

  describe('updateAgent', () => {
    it('should PATCH to update agent', () => {
      service.updateAgent(1, 1, { firstName: 'Updated' }).subscribe(data => {
        expect(data.id).toBe(1);
      });
      const req = httpMock.expectOne('/api/suppliers/1/agents/1');
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual({ firstName: 'Updated' });
      req.flush(mockAgent);
    });
  });

  describe('activateAgent', () => {
    it('should PATCH to activate agent', () => {
      service.activateAgent(1, 1).subscribe(data => {
        expect(data.status).toBe('ACTIVE');
      });
      const req = httpMock.expectOne('/api/suppliers/1/agents/1/activate');
      expect(req.request.method).toBe('PATCH');
      req.flush(mockAgent);
    });
  });

  describe('disableAgent', () => {
    it('should PATCH to disable agent', () => {
      service.disableAgent(1, 1).subscribe(data => {
        expect(data.id).toBe(1);
      });
      const req = httpMock.expectOne('/api/suppliers/1/agents/1/disable');
      expect(req.request.method).toBe('PATCH');
      req.flush(mockAgent);
    });
  });
});
