import type { BaseDataApi } from '#/api/base-data';

export interface AreaSelectOption {
  label: string;
  value: string;
}

export function readAreaRows(data: any): BaseDataApi.CityQueryResult[] {
  const source = data?.data ?? data;
  if (Array.isArray(source)) {
    return source.filter(Boolean).map(normalizeAreaRow);
  }
  if (Array.isArray(source?.records)) {
    return source.records.filter(Boolean).map(normalizeAreaRow);
  }
  if (Array.isArray(source?.list)) {
    return source.list.filter(Boolean).map(normalizeAreaRow);
  }
  return [];
}

export function buildProvinceOptions(rows: BaseDataApi.CityQueryResult[]): AreaSelectOption[] {
  return [
    { label: '全部省份', value: '' },
    ...dedupeOptions(rows.filter((item) => String(item.type) === '1').map(toOption)),
  ];
}

export function buildCityOptions(
  rows: BaseDataApi.CityQueryResult[],
  provinceId?: string,
): AreaSelectOption[] {
  const cityRows = rows.filter((item) => String(item.type) === '2');
  const municipalityRows = rows.filter(
    (item) => String(item.type) === '1' && isMunicipality(item),
  );
  const selectedProvinceId = String(provinceId ?? '');
  const scopedMunicipalityFallbackRows = municipalityRows.filter(
    (item) => !cityRows.some((city) => String(city.parentId ?? '') === String(item.id)),
  );
  const scopedRows = selectedProvinceId
    ? buildScopedCityRows(rows, cityRows, selectedProvinceId)
    : [...cityRows, ...scopedMunicipalityFallbackRows];
  return [{ label: '全部城市', value: '' }, ...dedupeOptions(scopedRows.map(toOption))];
}

export function buildAreaIds(
  rows: BaseDataApi.CityQueryResult[],
  areaId?: string | number,
): string[] {
  const selectedId = String(areaId ?? '');
  if (!selectedId) {
    return [];
  }
  const selectedRow = rows.find((item) => String(item.id) === selectedId);
  const ids = new Set<string>([selectedId]);

  if (selectedRow && String(selectedRow.type) === '2') {
    const parentRow = rows.find((item) => String(item.id) === String(selectedRow.parentId ?? ''));
    if (parentRow && isMunicipality(parentRow)) {
      ids.add(String(parentRow.id));
    }
  }

  let changed = true;
  while (changed) {
    changed = false;
    rows.forEach((item) => {
      const parentId = String(item.parentId ?? '');
      const id = String(item.id ?? '');
      if (id && ids.has(parentId) && !ids.has(id)) {
        ids.add(id);
        changed = true;
      }
    });
  }

  return Array.from(ids).filter(Boolean);
}

function buildScopedCityRows(
  rows: BaseDataApi.CityQueryResult[],
  cityRows: BaseDataApi.CityQueryResult[],
  selectedProvinceId: string,
) {
  const selectedProvince = rows.find((item) => String(item.id) === selectedProvinceId);
  const cityList = cityRows.filter(
    (item) =>
      String(item.parentId ?? '') === selectedProvinceId ||
      String(item.id ?? '') === selectedProvinceId,
  );
  if (cityList.length > 0) {
    return cityList;
  }
  return selectedProvince && isMunicipality(selectedProvince) ? [selectedProvince] : [];
}

function isMunicipality(row: BaseDataApi.CityQueryResult): boolean {
  return String(row.municipality ?? '') === '1';
}

function normalizeAreaRow(row: any): BaseDataApi.CityQueryResult {
  return {
    id: String(row.id ?? ''),
    municipality: row.municipality,
    name: String(row.name ?? ''),
    parentId: String(row.parentId ?? ''),
    type: String(row.type ?? ''),
  };
}

function toOption(row: BaseDataApi.CityQueryResult): AreaSelectOption {
  return {
    label: row.name,
    value: String(row.id),
  };
}

function dedupeOptions(options: AreaSelectOption[]): AreaSelectOption[] {
  const seen = new Set<string>();
  return options.filter((item) => {
    if (!item.value || seen.has(item.value)) {
      return false;
    }
    seen.add(item.value);
    return true;
  });
}
