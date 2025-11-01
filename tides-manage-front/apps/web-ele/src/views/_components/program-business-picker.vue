<script setup lang="ts">
import { computed, nextTick, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { areaManageListQueryApi } from '#/api/base-data';
import { moviePageApi, movieScreeningPageApi } from '#/api/movie';
import {
  programPageQueryApi,
  programSearchQueryApi,
  selectByParentProgramCategoryIdQueryApi,
  selectByTypeQueryApi,
} from '#/api/program';
import { buildAreaIds, buildCityOptions, readAreaRows } from '#/utils/manage-area';

type PickerMode = 'program' | 'movie';

interface BusinessSelection {
  programId: string;
  row: any;
  screening?: any;
  screeningId?: string;
  sourceType: PickerMode;
  title: string;
}

withDefaults(
  defineProps<{
    allowMovieAll?: boolean;
    movieAllText?: string;
    title?: string;
  }>(),
  {
    allowMovieAll: true,
    movieAllText: '查询全部场次',
    title: '选择节目',
  },
);

const emit = defineEmits<{
  (event: 'select-movie-all', payload: BusinessSelection): void;
  (event: 'select-movie-screening', payload: BusinessSelection): void;
  (event: 'select-program', payload: BusinessSelection): void;
}>();

const [ProgramPicker, programPickerApi] = useVbenDrawer();

const areaRows = ref<any[]>([]);
const areaOptions = ref<Array<{ label: string; value: string }>>([]);
const categoryOptions = ref<Array<{ label: string; value: string }>>([]);
const childCategoryOptions = ref<Array<{ label: string; value: string }>>([]);
const pickerMode = ref<PickerMode>('program');
const pickerKeyword = ref('');
const pickerLoading = ref(false);
const pickerAreaId = ref('');
const pickerParentCategoryId = ref('ALL');
const pickerCategoryId = ref('ALL');
const businessRows = ref<any[]>([]);
const businessPager = ref({ page: 1, size: 10, total: 0 });
const pageSizeOptions = [10, 20, 50, 100];

const selectedMovieRow = ref<any | null>(null);
const movieScreeningRows = ref<any[]>([]);
const movieScreeningLoading = ref(false);
const screeningPager = ref({ page: 1, size: 10, total: 0 });

const businessTotalPages = computed(() => {
  const total = Number(businessPager.value.total || 0);
  const size = Number(businessPager.value.size || 10);
  return size > 0 ? Math.max(1, Math.ceil(total / size)) : 1;
});

const screeningTotalPages = computed(() => {
  const total = Number(screeningPager.value.total || 0);
  const size = Number(screeningPager.value.size || 10);
  return size > 0 ? Math.max(1, Math.ceil(total / size)) : 1;
});

const isMovieTableCollapsed = computed(() => pickerMode.value === 'movie' && !!selectedMovieRow.value);
const tableRows = computed(() => (isMovieTableCollapsed.value ? [selectedMovieRow.value] : businessRows.value));

function readRecords(data: any) {
  const source = data?.data ?? data;
  if (Array.isArray(source)) return source;
  if (Array.isArray(source?.records)) return source.records;
  if (Array.isArray(source?.list)) return source.list;
  if (Array.isArray(source?.items)) return source.items;
  return [];
}

function readTotal(data: any, fallback: number) {
  const source = data?.data ?? data;
  return Number(source?.total ?? source?.totalSize ?? fallback);
}

function getAreaLabel(areaId: string) {
  return areaOptions.value.find((item) => item.value === areaId)?.label ?? '';
}

function movieDisplayName(row: any) {
  return String(row?.movieName ?? row?.title ?? '').replace(/^《|》$/g, '');
}

function resetMovieScreenings() {
  selectedMovieRow.value = null;
  movieScreeningRows.value = [];
  screeningPager.value.page = 1;
  screeningPager.value.total = 0;
}

function expandMovieList() {
  resetMovieScreenings();
}

async function loadPrograms() {
  pickerLoading.value = true;
  try {
    const areaId = String(pickerAreaId.value || '');
    const areaIds = buildAreaIds(areaRows.value, areaId);
    const keyword = pickerKeyword.value.trim();
    const baseParams: any = {
      pageNumber: String(businessPager.value.page),
      pageSize: String(businessPager.value.size),
    };
    if (areaId) {
      baseParams.areaId = areaId;
      baseParams.areaIds = areaIds;
    }

    if (pickerMode.value === 'movie') {
      resetMovieScreenings();
      if (keyword) {
        baseParams.movieName = keyword;
      }
      const res: any = await moviePageApi(baseParams);
      const data = res?.data ?? res;
      const list = readRecords(data);
      businessRows.value = list.map((item: any) => ({
        ...item,
        areaName: areaId ? getAreaLabel(areaId) : '全部城市',
        extraText: `${Number(item.screeningCount || 0)} 场 / 已售 ${Number(item.paidTicketCount || 0)} 张`,
        id: String(item.programId ?? item.id ?? ''),
        movieId: String(item.id ?? ''),
        sourceType: 'movie',
        title: `《${item.movieName ?? ''}》`,
      }));
      businessPager.value.total = readTotal(data, businessRows.value.length);
      if (data?.current != null) {
        businessPager.value.page = Number(data.current);
      }
      if (data?.size != null) {
        businessPager.value.size = Number(data.size);
      }
      return;
    }

    const params: any = {
      ...baseParams,
      timeType: '0',
      type: '1',
    };
    if (pickerParentCategoryId.value && pickerParentCategoryId.value !== 'ALL') {
      params.parentProgramCategoryId = String(pickerParentCategoryId.value);
    }
    if (pickerCategoryId.value && pickerCategoryId.value !== 'ALL') {
      params.programCategoryId = String(pickerCategoryId.value);
    }
    if (keyword) {
      params.content = keyword;
    }
    const res: any = keyword ? await programSearchQueryApi(params) : await programPageQueryApi(params);
    const data = res?.data ?? res;
    const list = readRecords(data);
    businessRows.value = list.map((item: any) => ({ ...item, sourceType: 'program' }));
    businessPager.value.total = readTotal(data, businessRows.value.length);
    if (data?.pageNum != null) {
      businessPager.value.page = Number(data.pageNum);
    }
    if (data?.pageSize != null) {
      businessPager.value.size = Number(data.pageSize);
    }
  } finally {
    pickerLoading.value = false;
  }
}

async function loadMovieScreenings(row = selectedMovieRow.value, resetPage = false) {
  if (!row) return;
  selectedMovieRow.value = row;
  if (resetPage) {
    screeningPager.value.page = 1;
  }
  movieScreeningLoading.value = true;
  try {
    const areaId = String(pickerAreaId.value || '');
    const areaIds = buildAreaIds(areaRows.value, areaId);
    const params: any = {
      movieId: String(row?.movieId ?? row?.id ?? ''),
      pageNumber: String(screeningPager.value.page),
      pageSize: String(screeningPager.value.size),
      programId: String(row?.programId ?? row?.id ?? ''),
    };
    if (areaId) {
      params.areaId = areaId;
      params.areaIds = areaIds;
    }
    const res: any = await movieScreeningPageApi(params);
    const data = res?.data ?? res;
    movieScreeningRows.value = readRecords(data);
    screeningPager.value.total = readTotal(data, movieScreeningRows.value.length);
    if (data?.current != null) {
      screeningPager.value.page = Number(data.current);
    }
    if (data?.size != null) {
      screeningPager.value.size = Number(data.size);
    }
  } finally {
    movieScreeningLoading.value = false;
  }
}

async function loadChildOptions(parentId: string | undefined) {
  if (!parentId || parentId === 'ALL') {
    childCategoryOptions.value = [{ label: '全部', value: 'ALL' }];
    return;
  }
  try {
    const list: any = await selectByParentProgramCategoryIdQueryApi({ parentProgramCategoryId: parentId } as any);
    const arr = Array.isArray(list) ? list : Array.isArray(list?.data) ? list.data : [];
    const mapped = arr.map((item: any) => ({ label: item.name, value: String(item.id) }));
    childCategoryOptions.value = [{ label: '全部', value: 'ALL' }, ...mapped];
  } catch (error) {
    childCategoryOptions.value = [{ label: '全部', value: 'ALL' }];
  }
}

async function initPickerFilters() {
  try {
    const [cityResult, categoryResult] = await Promise.allSettled([
      areaManageListQueryApi(),
      selectByTypeQueryApi({ type: '1' } as any),
    ]);

    if (cityResult.status === 'fulfilled') {
      areaRows.value = readAreaRows(cityResult.value);
      areaOptions.value = buildCityOptions(areaRows.value);
      pickerAreaId.value = '';
    } else {
      areaRows.value = [];
      areaOptions.value = [{ label: '全部城市', value: '' }];
      pickerAreaId.value = '';
    }

    if (categoryResult.status === 'fulfilled') {
      const list: any = categoryResult.value;
      const arr = Array.isArray(list) ? list : Array.isArray(list?.data) ? list.data : [];
      categoryOptions.value = [
        { label: '全部演出', value: 'ALL' },
        ...arr
          .filter((item: any) => String(item.id) !== '22')
          .map((item: any) => ({ label: item.name, value: String(item.id) })),
      ];
      pickerParentCategoryId.value = 'ALL';
    } else {
      categoryOptions.value = [{ label: '全部演出', value: 'ALL' }];
      pickerParentCategoryId.value = 'ALL';
    }

    await loadChildOptions(pickerParentCategoryId.value);
    pickerCategoryId.value = 'ALL';
  } finally {
    businessPager.value.page = 1;
    await loadPrograms();
  }
}

function open() {
  programPickerApi.open();
  nextTick(() => {
    initPickerFilters();
  });
}

function close() {
  programPickerApi.close();
}

function changePickerMode(mode: PickerMode) {
  if (pickerMode.value === mode) {
    return;
  }
  pickerMode.value = mode;
  businessPager.value.page = 1;
  resetMovieScreenings();
  loadPrograms();
}

async function chooseProgram(row: any) {
  const sourceType = String(row?.sourceType ?? 'program') as PickerMode;
  if (sourceType === 'movie') {
    await loadMovieScreenings(row, true);
    return;
  }
  emit('select-program', {
    programId: String(row?.programId ?? row?.id ?? ''),
    row,
    sourceType: 'program',
    title: String(row?.title ?? ''),
  });
  close();
}

function chooseMovieAll(row: any) {
  emit('select-movie-all', {
    programId: String(row?.programId ?? row?.id ?? ''),
    row,
    sourceType: 'movie',
    title: `电影：${movieDisplayName(row)} / 全部场次`,
  });
  close();
}

function chooseMovieScreening(row: any, screening: any) {
  emit('select-movie-screening', {
    programId: String(row?.programId ?? row?.id ?? ''),
    row,
    screening,
    screeningId: screening?.id ? String(screening.id) : '',
    sourceType: 'movie',
    title: `电影：${movieDisplayName(row)} / ${screening?.cinemaName ?? ''} ${screening?.showTime ?? ''}`,
  });
  close();
}

defineExpose({ close, open });
</script>

<template>
  <ProgramPicker :modal="false" class="w-[900px]" :title="title">
    <div class="business-picker">
      <div class="picker-mode-tabs">
        <button
          class="picker-tab"
          :class="{ active: pickerMode === 'program' }"
          @click="changePickerMode('program')"
        >
          演出
        </button>
        <button class="picker-tab" :class="{ active: pickerMode === 'movie' }" @click="changePickerMode('movie')">
          电影
        </button>
      </div>

      <div class="picker-filter-grid">
        <div>
          <label>区域</label>
          <select v-model="pickerAreaId" @change="(async () => { businessPager.page = 1; await loadPrograms(); })()">
            <option v-for="opt in areaOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
        </div>
        <div v-if="pickerMode === 'program'">
          <label>分类</label>
          <select
            v-model="pickerParentCategoryId"
            @change="(async () => { await loadChildOptions(pickerParentCategoryId); pickerCategoryId = 'ALL'; businessPager.page = 1; await loadPrograms(); })()"
          >
            <option v-for="opt in categoryOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
        </div>
        <div v-if="pickerMode === 'program'">
          <label>子类</label>
          <select v-model="pickerCategoryId" @change="(async () => { businessPager.page = 1; await loadPrograms(); })()">
            <option v-for="opt in childCategoryOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
        </div>
        <div :class="pickerMode === 'movie' ? 'span-3' : ''">
          <label>关键词</label>
          <div class="keyword-row">
            <input
              v-model="pickerKeyword"
              :placeholder="pickerMode === 'movie' ? '电影名称' : '演出、艺人、场馆'"
              @keydown.enter="(async () => { businessPager.page = 1; await loadPrograms(); })()"
            />
            <button @click="(async () => { businessPager.page = 1; await loadPrograms(); })()">查询</button>
          </div>
        </div>
      </div>

      <div v-if="isMovieTableCollapsed" class="collapsed-context">
        <span>当前电影：{{ movieDisplayName(selectedMovieRow) }}</span>
        <button @click="expandMovieList">展开电影列表</button>
      </div>

      <div class="picker-table-shell">
        <table class="picker-table">
          <thead>
            <tr>
              <th class="w-type">类型</th>
              <th class="w-id">节目ID</th>
              <th class="w-title">标题</th>
              <th class="w-city">城市</th>
              <th class="w-action">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="pickerLoading">
              <td class="empty-cell" colspan="5">加载中...</td>
            </tr>
            <tr v-else-if="tableRows.length === 0">
              <td class="empty-cell" colspan="5">暂无数据</td>
            </tr>
            <tr v-for="row in tableRows" v-else :key="`${row.sourceType}-${row.id}-${row.movieId ?? ''}`">
              <td>{{ row.sourceType === 'movie' ? '电影' : '演出' }}</td>
              <td>{{ row.id }}</td>
              <td>
                <div class="title-line">{{ row.title }}</div>
                <div v-if="row.extraText" class="sub-line">{{ row.extraText }}</div>
              </td>
              <td>{{ row.areaName }}</td>
              <td>
                <button class="link-button" @click="chooseProgram(row)">
                  {{ row.sourceType === 'movie' ? '查看场次' : '选择' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="!isMovieTableCollapsed" class="picker-pager">
        <div>共 {{ businessPager.total }} 条，页 {{ businessPager.page }} / {{ businessTotalPages }}</div>
        <div class="pager-actions">
          <label>每页</label>
          <select v-model.number="businessPager.size" @change="(async () => { businessPager.page = 1; await loadPrograms(); })()">
            <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}</option>
          </select>
          <button :disabled="businessPager.page <= 1" @click="(async () => { businessPager.page = Math.max(1, businessPager.page - 1); await loadPrograms(); })()">
            上一页
          </button>
          <button :disabled="businessPager.page >= businessTotalPages" @click="(async () => { businessPager.page = Math.min(businessTotalPages, businessPager.page + 1); await loadPrograms(); })()">
            下一页
          </button>
        </div>
      </div>

      <div v-if="pickerMode === 'movie' && selectedMovieRow" class="screening-panel">
        <div class="screening-head">
          <div class="title-line">{{ movieDisplayName(selectedMovieRow) }}</div>
          <button v-if="allowMovieAll" class="outline-button" @click="chooseMovieAll(selectedMovieRow)">
            {{ movieAllText }}
          </button>
        </div>
        <table class="picker-table">
          <thead>
            <tr>
              <th class="w-screening">场次ID</th>
              <th class="w-screening-cinema">影院</th>
              <th class="w-screening-time">放映时间</th>
              <th class="w-action">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="movieScreeningLoading">
              <td class="empty-cell" colspan="4">加载场次中...</td>
            </tr>
            <tr v-else-if="movieScreeningRows.length === 0">
              <td class="empty-cell" colspan="4">暂无可选场次</td>
            </tr>
            <tr v-for="screening in movieScreeningRows" v-else :key="screening.id">
              <td>{{ screening.id }}</td>
              <td>
                <div class="title-line">{{ screening.cinemaName }}</div>
                <div class="sub-line">{{ screening.hallName }}</div>
              </td>
              <td>{{ screening.showTime }}</td>
              <td>
                <button class="link-button" @click="chooseMovieScreening(selectedMovieRow, screening)">选择场次</button>
              </td>
            </tr>
          </tbody>
        </table>

        <div class="picker-pager">
          <div>共 {{ screeningPager.total }} 场，页 {{ screeningPager.page }} / {{ screeningTotalPages }}</div>
          <div class="pager-actions">
            <label>每页</label>
            <select v-model.number="screeningPager.size" @change="(async () => { screeningPager.page = 1; await loadMovieScreenings(); })()">
              <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}</option>
            </select>
            <button :disabled="screeningPager.page <= 1" @click="(async () => { screeningPager.page = Math.max(1, screeningPager.page - 1); await loadMovieScreenings(); })()">
              上一页
            </button>
            <button :disabled="screeningPager.page >= screeningTotalPages" @click="(async () => { screeningPager.page = Math.min(screeningTotalPages, screeningPager.page + 1); await loadMovieScreenings(); })()">
              下一页
            </button>
          </div>
        </div>
      </div>
    </div>
  </ProgramPicker>
</template>

<style scoped>
.business-picker {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
}

.picker-mode-tabs,
.screening-head,
.picker-pager,
.pager-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.screening-head,
.picker-pager {
  justify-content: space-between;
}

.picker-tab,
.outline-button,
.pager-actions button,
.keyword-row button {
  height: 32px;
  padding: 0 12px;
  border: 1px solid #dbe3ee;
  color: #475569;
  background: #fff;
  font-size: 13px;
}

.picker-tab.active,
.outline-button,
.keyword-row button {
  border-color: #2563eb;
  color: #2563eb;
}

.picker-filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.picker-filter-grid label {
  display: block;
  margin-bottom: 4px;
  color: #64748b;
  font-size: 12px;
}

.picker-filter-grid select,
.picker-filter-grid input,
.pager-actions select {
  width: 100%;
  height: 32px;
  padding: 4px 8px;
  border: 1px solid #dbe3ee;
  background: #fff;
}

.span-3 {
  grid-column: span 3;
}

.keyword-row {
  display: flex;
  gap: 8px;
}

.keyword-row input {
  flex: 1;
}

.collapsed-context {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 10px;
  border: 1px solid #dbe3ee;
  background: #f8fafc;
  color: #334155;
  font-size: 13px;
}

.collapsed-context button,
.link-button {
  color: #2563eb;
}

.picker-table-shell,
.screening-panel {
  border: 1px solid #e5eaf2;
}

.screening-panel {
  padding: 12px;
}

.picker-table {
  width: 100%;
  table-layout: fixed;
  text-align: left;
  font-size: 13px;
}

.picker-table th,
.picker-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #edf2f7;
  vertical-align: middle;
}

.picker-table thead {
  color: #64748b;
  background: #f8fafc;
}

.title-line,
.sub-line {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sub-line {
  margin-top: 3px;
  color: #64748b;
  font-size: 12px;
}

.empty-cell {
  height: 60px;
  text-align: center;
  color: #64748b;
}

.picker-pager {
  color: #64748b;
  font-size: 13px;
}

.pager-actions select {
  width: 76px;
}

.pager-actions button:disabled {
  opacity: 0.45;
}

.w-type {
  width: 16%;
}

.w-id {
  width: 20%;
}

.w-title {
  width: 34%;
}

.w-city {
  width: 16%;
}

.w-action {
  width: 14%;
}

.w-screening {
  width: 18%;
}

.w-screening-cinema {
  width: 34%;
}

.w-screening-time {
  width: 34%;
}

button {
  cursor: pointer;
}

@media (max-width: 760px) {
  .picker-filter-grid {
    grid-template-columns: 1fr;
  }

  .span-3 {
    grid-column: span 1;
  }

  .picker-pager,
  .screening-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
