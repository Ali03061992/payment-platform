// @ts-nocheck
/**
 * Tests du composant PwaUpdateComponent.
 * Perimetre : instanciation et blocs ngOnInit, dismiss, updateApp (voir blocs describe/it).
 * Moyens : TestBed + fixture, stubs jasmine.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PwaUpdateComponent } from './pwa-update.component';
import { SwUpdate, VersionReadyEvent } from '@angular/service-worker';
import { Subject } from 'rxjs';

describe('PwaUpdateComponent', () => {
  let component: PwaUpdateComponent;
  let fixture: ComponentFixture<PwaUpdateComponent>;
  let swUpdate: jasmine.SpyObj<SwUpdate>;
  let versionUpdatesSubject: Subject<VersionReadyEvent>;

  beforeEach(() => {
    versionUpdatesSubject = new Subject<VersionReadyEvent>();
    const swSpy = jasmine.createSpyObj('SwUpdate', ['activateUpdate'], {
      isEnabled: true,
      versionUpdates: versionUpdatesSubject.asObservable()
    });
    swSpy.activateUpdate.and.returnValue(Promise.resolve(true));

    TestBed.configureTestingModule({
      declarations: [PwaUpdateComponent],
      providers: [
        { provide: SwUpdate, useValue: swSpy }
      ]
    });
    fixture = TestBed.createComponent(PwaUpdateComponent);
    component = fixture.componentInstance;
    swUpdate = TestBed.inject(SwUpdate) as jasmine.SpyObj<SwUpdate>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should start with showUpdate false', () => {
    expect(component.showUpdate).toBeFalse();
  });

  describe('ngOnInit', () => {
    it('should show banner on VERSION_READY', () => {
      component.ngOnInit();
      versionUpdatesSubject.next({ type: 'VERSION_READY' } as VersionReadyEvent);
      expect(component.showUpdate).toBeTrue();
    });

    it('should not show banner for other event types', () => {
      component.ngOnInit();
      versionUpdatesSubject.next({ type: 'NO_UPDATE_FOUND' } as any);
      expect(component.showUpdate).toBeFalse();
    });
  });

  describe('dismiss', () => {
    it('should hide update banner', () => {
      component.showUpdate = true;
      component.dismiss();
      expect(component.showUpdate).toBeFalse();
    });
  });

  describe('updateApp', () => {
    it('should call activateUpdate', () => {
      swUpdate.activateUpdate.and.returnValue(new Promise(() => {}));
      component.updateApp();
      expect(swUpdate.activateUpdate).toHaveBeenCalled();
    });
  });
});
