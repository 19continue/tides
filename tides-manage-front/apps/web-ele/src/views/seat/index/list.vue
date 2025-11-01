<script setup lang="ts">
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { ProgramApi } from '#/api/program';
import { computed, h, nextTick, onMounted, ref } from 'vue';
import { Page } from '@vben/common-ui';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { programDetailApi, seatPageQueryApi, ticketCategoryListQueryApi } from '#/api/program';
import { movieScreeningPriceListApi, movieScreeningSeatPageApi } from '#/api/movie';
import { dbTicketCategoryListQueryApi } from '#/api/program';
import { $t } from '#/locales';
import ProgramBusinessPicker from '#/views/_components/program-business-picker.vue';
import { useColumns } from './data';

// 节目飘荡响应式数据存储下拉选项
const ticketCategoryOptions = ref<Array<{ label: string; value: string }>>([]);
const seatViewMode = ref<'map' | 'sales' | 'table'>('table');
const seatMapTitle = ref('未选择节目');
const seatMapRows = ref<any[]>([]);
const salesRows = ref<any[]>([]);
const salesLoading = ref(false);
const seatMapLoading = ref(false);
const seatMapPreparing = ref(false);
const seatMapAvailable = ref(false);
const currentSeatMapQuery = ref<SeatMapLoadOverrides | null>(null);
const programPermitChooseSeat = ref<boolean | null>(null);

type SeatMapLoadOverrides = {
  programId?: number | string;
  screeningId?: number | string;
  sourceType?: string;
  ticketCategoryId?: number | string;
  title?: string;
};

const canOpenSeatMap = computed(() => seatMapPreparing.value || seatMapLoading.value || seatMapAvailable.value);
const canOpenSalesView = computed(() => salesLoading.value || salesRows.value.length > 0);
const currentSeatViewName = computed(() => {
  if (seatViewMode.value === 'sales') return '销售视图';
  if (seatViewMode.value === 'map') return '座位图';
  return '座位列表';
});

// 表单组件，显示列表
const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    wrapperClass: 'grid-cols-2',
    commonConfig: {
      controlClass: 'max-w-[260px] w-full',
      formItemClass: 'col-span-1',
    },
    schema: [
      {
        component: 'Input',
        fieldName: 'programTitle',
        label: '节目',
        componentProps: {
          readonly: true,
          placeholder: '请选择节目',
          onClick: () => openProgramPicker(),
        },
        renderComponentContent: () => ({
          suffix: () => h('span', { class: 'text-blue-600 cursor-pointer', onClick: () => openProgramPicker() }, '选择'),
        }),
        //formItemClass: 'col-span-2',
      },
      // 隐藏字段：存储节目ID用于查询
      {
        component: 'Input',
        fieldName: 'programId',
        label: '',
        componentProps: {
          readonly: true,
        },
        formItemClass: 'hidden',
      },
      {
        component: 'Input',
        fieldName: 'sourceType',
        label: '',
        componentProps: {
          readonly: true,
        },
        formItemClass: 'hidden',
      },
      {
        component: 'Input',
        fieldName: 'screeningId',
        label: '',
        componentProps: {
          readonly: true,
        },
        formItemClass: 'hidden',
      },
      // 节目票档选择
      {
        component: 'Select',
        componentProps: () => ({
          allowClear: true,
          filterOption: true,
          onChange: (value: string | undefined) => {
            void loadSeatMapRows({ ticketCategoryId: value });
          },
          options: ticketCategoryOptions.value,
          showSearch: true,
        }),
        fieldName: 'ticketCategoryId',
        label: '节目票档',
        // 独占第一行：Tailwind 栅格应使用 col-span-2
        //formItemClass: 'col-span-2',
      },
    ],
    showCollapseButton: false,
    submitOnChange: true,
  },
  gridOptions: {
    columns: useColumns(),
    height: '100%',
    keepSource: true,
    pagerConfig: {
      enabled: true,
      pageSize: 20,
      pageSizes: [10, 20, 50, 100],
      currentPage: 1,
    },
    proxyConfig: {
      autoLoad: false,
      ajax: {
        query: async ({ page }, formValues) => {
          const programId = (formValues as any)?.programId as string | undefined;
          const ticketCategoryId = (formValues as any)?.ticketCategoryId as string | undefined;
          const sourceType = (formValues as any)?.sourceType as string | undefined;
          const screeningId = (formValues as any)?.screeningId as string | undefined;
          if (sourceType === 'movie') {
            if (!screeningId) {
              return { items: [], total: 0 } as any;
            }
            const query: any = {
              screeningId: String(screeningId),
              pageNumber: String(page.currentPage),
              pageSize: String(page.pageSize),
            };
            if (ticketCategoryId) {
              query.ticketCategoryId = String(ticketCategoryId);
            }
            const res: any = await movieScreeningSeatPageApi(query);
            const data = res?.data ?? res;
            const items = Array.isArray(data?.records)
              ? data.records
              : Array.isArray(data?.list)
              ? data.list
              : Array.isArray(data?.items)
              ? data.items
              : [];
            const total = Number(
              data?.total ?? data?.totalSize ?? (Array.isArray(items) ? items.length : 0),
            );
            return { items, total } as any;
          }
          if (!programId || !ticketCategoryId) {
            return { items: [], total: 0 } as any;
          }
          const res: any = await seatPageQueryApi({
            programId: String(programId),
            ticketCategoryId: String(ticketCategoryId),
            pageNumber: String(page.currentPage),
            pageSize: String(page.pageSize),
          } as any);
          const data = res?.data ?? res;
          const items = Array.isArray(data?.records)
            ? data.records
            : Array.isArray(data?.list)
            ? data.list
            : Array.isArray(data?.items)
            ? data.items
            : [];
          const total = Number(
            data?.total ?? data?.totalSize ?? (Array.isArray(items) ? items.length : 0),
          );
          return { items, total } as any;
        },
      },
    },
    rowConfig: {
      keyField: 'id',
    },
    toolbarConfig: {
      custom: true,
      export: false,
      refresh: { code: 'query' },
      search: true,
      zoom: true,
    },
  } as VxeTableGridOptions<ProgramApi.SeatPageQueryResult>,
});

const programPickerRef = ref<InstanceType<typeof ProgramBusinessPicker> | null>(null);

const seatMapColumnRange = computed(() => {
  const cols = seatMapRows.value
    .map((seat) => Number(seat?.colCode || 0))
    .filter((col) => col > 0)
    .sort((left, right) => left - right);
  const minCol = cols[0] ?? 1;
  const maxCol = cols[cols.length - 1] ?? 0;
  return {
    count: maxCol >= minCol ? maxCol - minCol + 1 : 0,
    maxCol,
    minCol,
  };
});

const seatMapGridStyle = computed(() => ({
  gridTemplateColumns: `repeat(${seatMapColumnRange.value.count || 1}, 30px)`,
}));

const seatMapGridWidthStyle = computed(() => {
  const count = seatMapColumnRange.value.count || 1;
  const width = count * 30 + Math.max(0, count - 1) * 6;
  return { width: `${width}px` };
});

const seatMapGroups = computed(() => {
  const rowMap = new Map<number, Map<number, any>>();
  seatMapRows.value.forEach((seat) => {
    const rowCode = Number(seat?.rowCode || 0);
    const colCode = Number(seat?.colCode || 0);
    if (!rowCode || !colCode) return;
    if (!rowMap.has(rowCode)) {
      rowMap.set(rowCode, new Map<number, any>());
    }
    rowMap.get(rowCode)?.set(colCode, seat);
  });
  return [...rowMap.entries()]
    .sort(([left], [right]) => left - right)
    .map(([rowCode, colMap]) => {
      const { maxCol, minCol } = seatMapColumnRange.value;
      const seats = [];
      for (let col = minCol; col <= maxCol; col += 1) {
        seats.push(colMap.get(col) ?? { colCode: col, placeholder: true, rowCode });
      }
      return {
        rowCode,
        seats,
      };
    });
});

const seatMapStats = computed(() => {
  return seatMapRows.value.reduce(
    (stats, seat) => {
      const kind = seatStatusKind(seat);
      stats.total += 1;
      if (kind === 'available') stats.available += 1;
      if (kind === 'locked') stats.locked += 1;
      if (kind === 'sold') stats.sold += 1;
      if (kind === 'disabled') stats.disabled += 1;
      return stats;
    },
    { available: 0, disabled: 0, locked: 0, sold: 0, total: 0 },
  );
});

const salesStats = computed(() => {
  return salesRows.value.reduce(
    (stats, row) => {
      stats.total += salesTotal(row);
      stats.remain += salesRemain(row);
      stats.sold += salesSold(row);
      return stats;
    },
    { remain: 0, sold: 0, total: 0 },
  );
});

const salesOverallPercent = computed(() => {
  if (salesStats.value.total <= 0) return 0;
  return Math.min(100, Math.round((salesStats.value.sold / salesStats.value.total) * 100));
});

const seatPriceGroups = computed(() => {
  const groupMap = new Map<string, { count: number; index: number; label: string }>();
  seatMapRows.value.forEach((seat) => {
    if (seat?.placeholder) return;
    const key = seatPriceKey(seat);
    if (!key) return;
    const current = groupMap.get(key);
    if (current) {
      current.count += 1;
      return;
    }
    groupMap.set(key, {
      count: 1,
      index: groupMap.size,
      label: seatPriceLabel(seat),
    });
  });
  return [...groupMap.entries()].map(([key, value]) => ({ key, ...value }));
});

function openProgramPicker() {
  programPickerRef.value?.open();
}

async function chooseProgramFromPicker(payload: any) {
  const form = (gridApi as any)?.formApi;
  const allowSeatMap = await resolveProgramPermitChooseSeat(payload);
  ticketCategoryOptions.value = [];
  seatMapRows.value = [];
  salesRows.value = [];
  seatMapPreparing.value = false;
  seatMapAvailable.value = false;
  currentSeatMapQuery.value = null;
  programPermitChooseSeat.value = allowSeatMap;
  seatViewMode.value = 'table';
  form?.setFieldValue?.('ticketCategoryId', '');
  form?.setFieldValue?.('sourceType', 'program');
  form?.setFieldValue?.('programId', payload.programId);
  form?.setFieldValue?.('screeningId', '');
  form?.setFieldValue?.('programTitle', payload.title);
  seatMapTitle.value = payload.title || '未选择节目';
  await loadDbTicketCategories(payload.programId, { loadSeatMap: allowSeatMap, title: payload.title });
}

async function chooseMovieScreeningFromPicker(payload: any) {
  const form = (gridApi as any)?.formApi;
  const screeningId = payload.screeningId ? String(payload.screeningId) : '';
  ticketCategoryOptions.value = [];
  seatMapRows.value = [];
  salesRows.value = [];
  seatMapPreparing.value = true;
  seatMapAvailable.value = false;
  programPermitChooseSeat.value = true;
  seatViewMode.value = 'map';
  currentSeatMapQuery.value = {
    programId: payload.programId,
    screeningId,
    sourceType: 'movie',
    ticketCategoryId: '',
    title: payload.title,
  };
  form?.setFieldValue?.('ticketCategoryId', '');
  form?.setFieldValue?.('screeningId', screeningId);
  form?.setFieldValue?.('sourceType', 'movie');
  form?.setFieldValue?.('programId', payload.programId);
  form?.setFieldValue?.('programTitle', payload.title);
  seatMapTitle.value = payload.title || '未选择节目';
  if (screeningId) {
    await loadMovieTicketCategories(screeningId, payload);
  }
}

async function loadMovieTicketCategories(screeningId: string, context: any = {}) {
  if (!screeningId) {
    ticketCategoryOptions.value = [];
    seatMapPreparing.value = false;
    return;
  }
  try {
    const resp: any = await movieScreeningPriceListApi({ screeningId });
    const list = readRecords(resp);
    ticketCategoryOptions.value = list.map((item: any) => ({
      label: `${item?.priceName ?? '电影票'} ${item?.price ?? ''}`,
      value: String(item?.ticketCategoryId ?? ''),
    }));
  } catch (error) {
    ticketCategoryOptions.value = [];
  }
  const form = (gridApi as any)?.formApi;
  form?.setFieldValue?.('ticketCategoryId', '');
  await nextTick();
  form?.submit?.();
  await loadSeatMapRows({
    programId: context?.programId,
    screeningId,
    sourceType: 'movie',
    ticketCategoryId: '',
    title: context?.title,
  });
}

async function loadDbTicketCategories(
  programId: string,
  options: { loadSeatMap?: boolean; title?: string } = {},
) {
  if (!programId) {
    ticketCategoryOptions.value = [];
    return;
  }
  try {
    const resp: any = await dbTicketCategoryListQueryApi({ programId } as any);
    const list = Array.isArray(resp)
      ? resp
      : Array.isArray(resp?.data)
      ? resp.data
      : Array.isArray(resp?.list)
      ? resp.list
      : [];
    ticketCategoryOptions.value = list.map((item: any) => ({
      label: String(item?.introduce ?? ''),
      value: String(item?.id ?? ''),
    }));

    if (options.loadSeatMap === false) {
      const form = (gridApi as any)?.formApi;
      const firstTicketCategory = ticketCategoryOptions.value[0];
      if (firstTicketCategory?.value) {
        form?.setFieldValue?.('ticketCategoryId', firstTicketCategory.value);
        await nextTick();
      }
      form?.submit?.();
      seatMapRows.value = [];
      seatMapPreparing.value = false;
      seatMapAvailable.value = false;
      currentSeatMapQuery.value = null;
      await loadSalesRows(programId);
      seatViewMode.value = salesRows.value.length > 0 ? 'sales' : 'table';
      return;
    }

    // 自动选择第一个票档并触发查询
    if (ticketCategoryOptions.value.length > 0) {
      const form = (gridApi as any)?.formApi;
      const firstTicketCategory = ticketCategoryOptions.value[0];
      if (firstTicketCategory?.value) {
        form?.setFieldValue?.('ticketCategoryId', firstTicketCategory.value);
        await nextTick();
        form?.submit?.();
        const rowCount = await loadSeatMapRows({
          programId,
          sourceType: 'program',
          ticketCategoryId: firstTicketCategory.value,
          title: options.title,
        });
        seatViewMode.value = rowCount > 0 ? 'map' : 'table';
      }
    }
  } catch (error) {
    ticketCategoryOptions.value = [];
  }
}

async function resolveProgramPermitChooseSeat(payload: any) {
  const value = payload?.permitChooseSeat ?? payload?.row?.permitChooseSeat;
  if (value !== undefined && value !== null && value !== '') {
    return Number(value) === 1;
  }
  const programId = payload?.programId ?? payload?.row?.id;
  if (!programId) return false;
  try {
    const detail: any = await programDetailApi({ id: programId });
    const data = detail?.data ?? detail;
    return Number(data?.permitChooseSeat ?? 0) === 1;
  } catch (error) {
    return false;
  }
}

async function loadSalesRows(programId: number | string) {
  if (!programId) {
    salesRows.value = [];
    return;
  }
  salesLoading.value = true;
  try {
    const res: any = await ticketCategoryListQueryApi({ programId: String(programId) });
    salesRows.value = readRecords(res).map((item: any) => ({
      ...item,
      soldNumber: salesSold(item),
      soldPercent: salesPercent(item),
    }));
  } catch (error) {
    salesRows.value = [];
  } finally {
    salesLoading.value = false;
  }
}

function readRecords(data: any) {
  const source = data?.data ?? data;
  if (Array.isArray(source)) return source;
  if (Array.isArray(source?.records)) return source.records;
  if (Array.isArray(source?.list)) return source.list;
  if (Array.isArray(source?.items)) return source.items;
  return [];
}

function readSeatMapQuery(overrides: SeatMapLoadOverrides = {}) {
  const form = (gridApi as any)?.formApi;
  const values = form?.getFieldsValue?.() ?? {};
  const cached = currentSeatMapQuery.value ?? {};
  const sourceType = String(overrides.sourceType ?? cached.sourceType ?? values?.sourceType ?? 'program');
  const programId =
    overrides.programId != null
      ? String(overrides.programId)
      : cached.programId != null
        ? String(cached.programId)
        : values?.programId
          ? String(values.programId)
          : '';
  const screeningId =
    overrides.screeningId != null
      ? String(overrides.screeningId)
      : cached.screeningId != null
        ? String(cached.screeningId)
        : values?.screeningId
          ? String(values.screeningId)
          : '';
  const ticketCategoryId =
    overrides.ticketCategoryId != null
      ? String(overrides.ticketCategoryId)
      : cached.ticketCategoryId != null
        ? String(cached.ticketCategoryId)
        : values?.ticketCategoryId
          ? String(values.ticketCategoryId)
          : '';
  const title = String(overrides.title ?? cached.title ?? values?.programTitle ?? seatMapTitle.value ?? '未选择节目');
  return { programId, screeningId, sourceType, ticketCategoryId, title };
}

async function loadSeatMapRows(overrides: SeatMapLoadOverrides = {}) {
  const { programId, screeningId, sourceType, ticketCategoryId, title } = readSeatMapQuery(overrides);
  seatMapTitle.value = title;

  if (sourceType === 'movie' && !screeningId) {
    seatMapRows.value = [];
    seatMapPreparing.value = false;
    seatMapAvailable.value = false;
    seatViewMode.value = 'table';
    return 0;
  }
  if (sourceType !== 'movie' && programPermitChooseSeat.value === false) {
    seatMapRows.value = [];
    seatMapPreparing.value = false;
    seatMapAvailable.value = false;
    await loadSalesRows(programId);
    seatViewMode.value = salesRows.value.length > 0 ? 'sales' : 'table';
    return 0;
  }
  if (sourceType !== 'movie' && (!programId || !ticketCategoryId)) {
    seatMapRows.value = [];
    seatMapPreparing.value = false;
    seatMapAvailable.value = false;
    seatViewMode.value = 'table';
    return 0;
  }

  seatMapLoading.value = true;
  try {
    const query: any = {
      pageNumber: '1',
      pageSize: '5000',
    };
    if (ticketCategoryId) {
      query.ticketCategoryId = ticketCategoryId;
    }
    if (sourceType === 'movie') {
      const res: any = await movieScreeningSeatPageApi({
        ...query,
        screeningId,
      });
      const rows = readRecords(res).sort(compareSeatPosition);
      applySeatMapRows(rows, { programId, screeningId, sourceType, ticketCategoryId, title });
      return rows.length;
    }
    const res: any = await seatPageQueryApi({
      ...query,
      programId,
    } as any);
    const rows = readRecords(res).sort(compareSeatPosition);
    applySeatMapRows(rows, { programId, screeningId, sourceType, ticketCategoryId, title });
    return rows.length;
  } catch (error) {
    seatMapRows.value = [];
    seatMapPreparing.value = false;
    seatMapAvailable.value = false;
    if (seatViewMode.value === 'map') {
      seatViewMode.value = 'table';
    }
    return 0;
  } finally {
    seatMapPreparing.value = false;
    seatMapLoading.value = false;
  }
}

function applySeatMapRows(rows: any[], query: Required<Pick<SeatMapLoadOverrides, 'sourceType' | 'title'>> & SeatMapLoadOverrides) {
  seatMapRows.value = rows;
  seatMapPreparing.value = false;
  seatMapAvailable.value = rows.length > 0;
  currentSeatMapQuery.value = query;
  if (rows.length === 0 && seatViewMode.value === 'map') {
    seatViewMode.value = 'table';
  }
}

function setSeatViewMode(mode: 'map' | 'sales' | 'table') {
  if (mode === 'map' && !canOpenSeatMap.value) {
    seatViewMode.value = 'table';
    return;
  }
  if (mode === 'sales' && !canOpenSalesView.value) {
    seatViewMode.value = 'table';
    return;
  }
  seatViewMode.value = mode;
  if (mode === 'map') {
    void loadSeatMapRows();
  }
}

function compareSeatPosition(left: any, right: any) {
  const leftRow = Number(left?.rowCode || 0);
  const rightRow = Number(right?.rowCode || 0);
  if (leftRow !== rightRow) return leftRow - rightRow;
  return Number(left?.colCode || 0) - Number(right?.colCode || 0);
}

function seatStatusKind(seat: any) {
  if (seat?.placeholder) return 'placeholder';
  if (Number(seat?.repairFlag || 0) === 1 || Number(seat?.sellableFlag ?? 1) === 0 || Number(seat?.aisleFlag || 0) === 1) {
    return 'disabled';
  }
  const status = Number(seat?.dbSellStatus ?? seat?.sellStatus ?? 1);
  const statusName = String(seat?.dbSellStatusName ?? '');
  if (status === 3 || statusName.includes('已售')) return 'sold';
  if (status === 2 || statusName.includes('锁定')) return 'locked';
  return 'available';
}

function seatClass(seat: any) {
  const classes = [`seat-cell--${seatStatusKind(seat)}`];
  const priceClass = seatPriceClass(seat);
  if (priceClass) {
    classes.push(priceClass);
  }
  return classes;
}

function seatText(seat: any) {
  if (seat?.placeholder) return '';
  return String(seat?.colCode || seat?.seatNo || '');
}

function seatTooltip(seat: any) {
  if (seat?.placeholder) return '';
  const attrs = [
    `${seat?.rowCode ?? '-'}排${seat?.colCode ?? '-'}座`,
    seat?.seatTypeName,
    seat?.zoneName,
    seat?.priceLevel,
    `DB：${seat?.dbSellStatusName ?? '-'}`,
    `Redis：${seat?.redisSellStatusName ?? '-'}`,
    seat?.price ? `￥${seat.price}` : '',
  ].filter(Boolean);
  return attrs.join(' / ');
}

function seatPriceKey(seat: any) {
  if (seat?.placeholder) return '';
  return String(seat?.price ?? seat?.priceLevel ?? seat?.ticketCategoryId ?? '');
}

function seatPriceLabel(seat: any) {
  const prefix = seat?.priceLevel || seat?.zoneName || (seat?.ticketCategoryId ? `票档${seat.ticketCategoryId}` : '默认票档');
  return seat?.price ? `${prefix} ￥${seat.price}` : String(prefix);
}

function seatPriceClass(seat: any) {
  const key = seatPriceKey(seat);
  if (!key) return '';
  const group = seatPriceGroups.value.find((item) => item.key === key);
  if (!group) return '';
  return `seat-price-${group.index % 6}`;
}

function salesTotal(row: any) {
  return Math.max(0, Number(row?.totalNumber ?? 0));
}

function salesRemain(row: any) {
  const remain = row?.dbRemainNumber ?? row?.redisRemainNumber ?? row?.remainNumber ?? row?.totalNumber ?? 0;
  return Math.max(0, Number(remain));
}

function salesSold(row: any) {
  return Math.max(0, salesTotal(row) - salesRemain(row));
}

function salesPercent(row: any) {
  const total = salesTotal(row);
  if (total <= 0) return 0;
  return Math.min(100, Math.round((salesSold(row) / total) * 100));
}

function salesStatusText(row: any) {
  if (salesTotal(row) <= 0) return '未配置';
  if (salesRemain(row) <= 0) return '售罄';
  return '在售';
}

function formatPercent(value: number) {
  return `${Math.max(0, Math.min(100, Number(value || 0)))}%`;
}

onMounted(() => {
  nextTick(() => {
    const values = (gridApi as any)?.formApi?.getFieldsValue?.();
    const programId = values?.programId as string | undefined;
    if (values?.programTitle) {
      seatMapTitle.value = values.programTitle;
    }
    if (programId) {
      loadDbTicketCategories(String(programId));
    }
  });
});

</script>
<template>
  <Page auto-content-height>
    <div class="seat-list-page">
      <div class="seat-view-toolbar">
        <div class="seat-view-title">
          <span>{{ currentSeatViewName }}</span>
          <div class="seat-title-line">
            <strong>{{ seatMapTitle }}</strong>
            <button class="seat-title-switch" type="button" @click="openProgramPicker">切换</button>
          </div>
        </div>
        <div class="seat-view-actions">
          <button :class="{ active: seatViewMode === 'table' }" @click="setSeatViewMode('table')">表格</button>
          <button v-if="canOpenSalesView" :class="{ active: seatViewMode === 'sales' }" @click="setSeatViewMode('sales')">销售图</button>
          <button v-if="canOpenSeatMap" :class="{ active: seatViewMode === 'map' }" @click="setSeatViewMode('map')">座位图</button>
          <button v-if="canOpenSeatMap" :disabled="seatMapLoading || seatMapPreparing" @click="() => loadSeatMapRows()">刷新座位图</button>
        </div>
      </div>

      <div v-show="seatViewMode === 'table'" class="seat-table-panel">
        <Grid :table-title="$t('tides.index.seatListTitle')">
          <template #orderNumberCell="{ row }">
            <span class="text-gray-800">{{ row.id }}</span>
          </template>
        </Grid>
      </div>

      <div v-show="seatViewMode === 'sales' && canOpenSalesView" v-loading="salesLoading" class="sales-panel">
        <div class="sales-summary">
          <div>
            <span>总票数</span>
            <strong>{{ salesStats.total }}</strong>
          </div>
          <div>
            <span>已售</span>
            <strong>{{ salesStats.sold }}</strong>
          </div>
          <div>
            <span>剩余</span>
            <strong>{{ salesStats.remain }}</strong>
          </div>
          <div>
            <span>售出率</span>
            <strong>{{ salesOverallPercent }}%</strong>
          </div>
        </div>

        <div v-if="salesRows.length === 0" class="seat-empty">
          请选择自动分配座位的节目后查看销售情况
        </div>
        <div v-else class="sales-list">
          <div
            v-for="row in salesRows"
            :key="row.id"
            class="sales-row"
            :class="{ 'sales-row--sold-out': salesRemain(row) <= 0 && salesTotal(row) > 0 }"
          >
            <div class="sales-row-main">
              <div class="sales-row-title">
                <span>{{ row.introduce || '未命名票档' }}</span>
                <em>{{ salesStatusText(row) }}</em>
              </div>
              <div class="sales-row-meta">
                <span>票价 ￥{{ row.price || '-' }}</span>
                <span>已售 {{ salesSold(row) }} / {{ salesTotal(row) }}</span>
                <span>剩余 {{ salesRemain(row) }}</span>
              </div>
            </div>
            <div class="sales-progress">
              <div class="sales-bar">
                <i :style="{ width: formatPercent(row.soldPercent ?? salesPercent(row)) }"></i>
              </div>
              <span>{{ row.soldPercent ?? salesPercent(row) }}%</span>
            </div>
          </div>
        </div>
      </div>

      <div v-show="seatViewMode === 'map' && canOpenSeatMap" v-loading="seatMapLoading || seatMapPreparing" class="seat-map-panel">
        <div class="seat-map-summary">
          <div>
            <span>座位总数</span>
            <strong>{{ seatMapStats.total }}</strong>
          </div>
          <div>
            <span>可售</span>
            <strong>{{ seatMapStats.available }}</strong>
          </div>
          <div>
            <span>锁定</span>
            <strong>{{ seatMapStats.locked }}</strong>
          </div>
          <div>
            <span>已售</span>
            <strong>{{ seatMapStats.sold }}</strong>
          </div>
        </div>

        <div v-if="seatMapGroups.length === 0" class="seat-empty">
          请选择节目/电影场次和票档后查看座位图
        </div>
        <div v-else class="seat-map-scroll">
          <div class="seat-map-body">
            <div class="seat-screen-row">
              <div class="seat-row-label seat-row-label--screen">方向</div>
              <div class="seat-screen" :style="seatMapGridWidthStyle">舞台/银幕方向</div>
            </div>
            <div v-for="group in seatMapGroups" :key="group.rowCode" class="seat-row">
              <div class="seat-row-label">{{ group.rowCode }}排</div>
              <div class="seat-row-cells" :style="seatMapGridStyle">
                <button
                  v-for="seat in group.seats"
                  :key="`${group.rowCode}-${seat.colCode}-${seat.id || 'empty'}`"
                  class="seat-cell"
                  :class="seatClass(seat)"
                  :title="seatTooltip(seat)"
                >
                  {{ seatText(seat) }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="seatPriceGroups.length > 0" class="seat-price-legend">
          <span v-for="item in seatPriceGroups" :key="item.key">
            <i class="seat-price-dot" :class="`seat-price-${item.index % 6}`"></i>{{ item.label }}
          </span>
        </div>

        <div class="seat-legend">
          <span><i class="legend-available"></i>可售</span>
          <span><i class="legend-locked"></i>锁定</span>
          <span><i class="legend-sold"></i>已售</span>
          <span><i class="legend-disabled"></i>不可售/过道</span>
        </div>
      </div>
    </div>
    <ProgramBusinessPicker
      ref="programPickerRef"
      :allow-movie-all="false"
      title="选择节目"
      @select-movie-screening="chooseMovieScreeningFromPicker"
      @select-program="chooseProgramFromPicker"
    />
  </Page>
</template>

<style scoped>
.seat-list-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
  min-height: 0;
}

.seat-view-toolbar {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 14px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.seat-view-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  flex: 1 1 auto;
}

.seat-view-title span {
  color: #64748b;
  font-size: 12px;
}

.seat-view-title strong {
  overflow: hidden;
  font-size: 14px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.seat-title-line {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.seat-title-line strong {
  min-width: 0;
}

.seat-title-switch {
  flex: 0 0 auto;
  height: 24px;
  padding: 0 4px;
  border: 0;
  background: transparent;
  color: #2563eb;
  cursor: pointer;
  font-size: 13px;
}

.seat-view-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  flex: 0 0 auto;
}

.seat-view-actions button {
  height: 30px;
  padding: 0 12px;
  border: 1px solid #dbe3ee;
  background: #fff;
  color: #475569;
  font-size: 13px;
}

.seat-view-actions button.active {
  border-color: #2563eb;
  color: #2563eb;
}

.seat-view-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.seat-table-panel {
  min-height: 0;
  flex: 1 1 auto;
  overflow: hidden;
}

.sales-panel,
.seat-map-panel {
  min-height: 0;
  flex: 1 1 auto;
  overflow: auto;
  padding: 16px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.sales-summary,
.seat-map-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1px;
  overflow: hidden;
  margin-bottom: 16px;
  border: 1px solid #e5e7eb;
  background: #e5e7eb;
}

.sales-summary div,
.seat-map-summary div {
  padding: 10px 12px;
  background: #fff;
}

.sales-summary span,
.seat-map-summary span {
  display: block;
  margin-bottom: 4px;
  color: #64748b;
  font-size: 12px;
}

.sales-summary strong,
.seat-map-summary strong {
  font-size: 20px;
  font-weight: 500;
}

.sales-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sales-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(240px, 34%);
  gap: 18px;
  align-items: center;
  padding: 12px 14px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.sales-row--sold-out {
  background: #fff7f7;
}

.sales-row-main {
  min-width: 0;
}

.sales-row-title {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  margin-bottom: 7px;
}

.sales-row-title span {
  overflow: hidden;
  color: #0f172a;
  font-size: 14px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sales-row-title em {
  flex: 0 0 auto;
  padding: 1px 6px;
  border: 1px solid #dbeafe;
  color: #2563eb;
  font-style: normal;
  font-size: 12px;
}

.sales-row--sold-out .sales-row-title em {
  border-color: #fecaca;
  color: #dc2626;
}

.sales-row-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: #64748b;
  font-size: 12px;
}

.sales-progress {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 44px;
  align-items: center;
  gap: 10px;
  color: #475569;
  font-size: 12px;
}

.sales-bar {
  height: 10px;
  overflow: hidden;
  background: #eef2f7;
}

.sales-bar i {
  display: block;
  height: 100%;
  background: #2563eb;
}

.sales-row--sold-out .sales-bar i {
  background: #dc2626;
}

.seat-screen {
  flex: 0 0 auto;
  max-width: none;
  margin: 0;
  padding: 7px 0;
  border: 1px solid #dbe3ee;
  background: #f8fafc;
  color: #64748b;
  text-align: center;
  font-size: 12px;
}

.seat-empty {
  display: grid;
  min-height: 220px;
  place-items: center;
  color: #64748b;
  font-size: 13px;
}

.seat-map-scroll {
  overflow: auto;
  padding: 4px 0 8px;
}

.seat-map-body {
  width: max-content;
  margin: 0 auto;
}

.seat-screen-row,
.seat-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: max-content;
  margin-bottom: 8px;
}

.seat-screen-row {
  margin-bottom: 18px;
}

.seat-row-label {
  width: 44px;
  flex: 0 0 auto;
  color: #64748b;
  text-align: right;
  font-size: 12px;
}

.seat-row-label--screen {
  visibility: hidden;
}

.seat-row-cells {
  display: grid;
  gap: 6px;
}

.seat-cell {
  --seat-price-color: transparent;
  box-sizing: border-box;
  display: grid;
  width: 30px;
  min-width: 30px;
  height: 28px;
  place-items: center;
  border: 1px solid transparent;
  font-size: 11px;
  line-height: 1;
  overflow: hidden;
  white-space: nowrap;
  box-shadow: inset 0 -3px 0 var(--seat-price-color);
}

.seat-cell--available {
  border-color: #93c5fd;
  background: #eff6ff;
  color: #1d4ed8;
}

.seat-cell--locked {
  border-color: #fbbf24;
  background: #fffbeb;
  color: #b45309;
}

.seat-cell--sold {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.seat-cell--disabled {
  border-color: #d1d5db;
  background: #f3f4f6;
  color: #9ca3af;
}

.seat-cell--placeholder {
  border-color: transparent;
  background: transparent;
  box-shadow: none;
  pointer-events: none;
}

.seat-price-0 {
  --seat-price-color: #2563eb;
}

.seat-price-1 {
  --seat-price-color: #16a34a;
}

.seat-price-2 {
  --seat-price-color: #9333ea;
}

.seat-price-3 {
  --seat-price-color: #dc2626;
}

.seat-price-4 {
  --seat-price-color: #0891b2;
}

.seat-price-5 {
  --seat-price-color: #ca8a04;
}

.seat-cell--available.seat-price-0 {
  border-color: #93c5fd;
  background: #eff6ff;
}

.seat-cell--available.seat-price-1 {
  border-color: #86efac;
  background: #f0fdf4;
  color: #166534;
}

.seat-cell--available.seat-price-2 {
  border-color: #d8b4fe;
  background: #faf5ff;
  color: #7e22ce;
}

.seat-cell--available.seat-price-3 {
  border-color: #fca5a5;
  background: #fef2f2;
  color: #b91c1c;
}

.seat-cell--available.seat-price-4 {
  border-color: #67e8f9;
  background: #ecfeff;
  color: #0e7490;
}

.seat-cell--available.seat-price-5 {
  border-color: #fde68a;
  background: #fffbeb;
  color: #a16207;
}

.seat-price-legend,
.seat-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 14px;
  color: #64748b;
  font-size: 12px;
}

.seat-price-legend span,
.seat-legend span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.seat-price-dot,
.seat-legend i {
  width: 12px;
  height: 12px;
  border: 1px solid transparent;
}

.seat-price-dot {
  background: var(--seat-price-color);
}

.legend-available {
  border-color: #93c5fd !important;
  background: #eff6ff;
}

.legend-locked {
  border-color: #fbbf24 !important;
  background: #fffbeb;
}

.legend-sold {
  border-color: #fecaca !important;
  background: #fef2f2;
}

.legend-disabled {
  border-color: #d1d5db !important;
  background: #f3f4f6;
}

@media (max-width: 760px) {
  .seat-view-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .seat-map-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .sales-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .sales-row {
    grid-template-columns: 1fr;
  }
}
</style>
