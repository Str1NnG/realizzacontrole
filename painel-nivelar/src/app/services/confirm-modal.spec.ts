import { TestBed } from '@angular/core/testing';

import { ConfirmModal } from './confirm-modal';

describe('ConfirmModal', () => {
  let service: ConfirmModal;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ConfirmModal);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
