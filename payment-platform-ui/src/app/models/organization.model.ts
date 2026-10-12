export type FiscalRegime = 'ASSUJETTI' | 'FORFAITAIRE' | 'SUSPENDU' | 'EXPORT' | 'NON_ASSUJETTI' | 'ETAT_PUBLIC';

export const FISCAL_REGIMES: FiscalRegime[] = ['ASSUJETTI', 'FORFAITAIRE', 'SUSPENDU', 'EXPORT', 'NON_ASSUJETTI', 'ETAT_PUBLIC'];

export interface Organization {
  id: string;
  name: string;
  type: 'SUPPLIER' | 'SHOP';
  status: 'ACTIVE' | 'DISABLED';
  version: number;
  createdAt: string;
  updatedAt: string;
  relations: SupplierShopRelation[];
  matriculeFiscal?: string | null;
  patente?: string | null;
  tejIdentifier?: string | null;
  defaultWithholdingNature?: string | null;
  address?: string | null;
  fiscalRegime?: FiscalRegime | null;
  suspensionAttestationNumber?: string | null;
  suspensionAttestationDate?: string | null;
  suspensionExpiryDate?: string | null;
}

export interface SupplierShopRelation {
  id: string;
  supplierId: string;
  shopId: string;
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
}

export interface OrganizationStats {
  suppliers: number;
  shops: number;
}

export interface CreateOrganizationRequest {
  name: string;
  matriculeFiscal?: string | null;
  patente?: string | null;
  tejIdentifier?: string | null;
  defaultWithholdingNature?: string | null;
  address?: string | null;
  fiscalRegime?: FiscalRegime | null;
  suspensionAttestationNumber?: string | null;
  suspensionAttestationDate?: string | null;
  suspensionExpiryDate?: string | null;
}

export interface CreateRelationRequest {
  supplierId: string;
  shopId: string;
}
