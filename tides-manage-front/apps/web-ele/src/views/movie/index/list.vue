<script setup lang="ts">
import type { MovieApi } from '#/api/movie';

import { computed, onMounted, reactive, ref, watch } from 'vue';

import { Page } from '@vben/common-ui';

import {
  ElButton,
  ElDatePicker,
  ElDialog,
  ElDrawer,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElPagination,
  ElSelect,
  ElTabPane,
  ElTable,
  ElTableColumn,
  ElTabs,
  ElTag,
} from 'element-plus';

import {
  cinemaPageApi,
  movieInventoryReconcileApi,
  movieInventoryReconcileIssueHandleApi,
  movieInventoryReconcileIssuePageApi,
  movieMediaPageApi,
  movieMediaSaveApi,
  movieMediaStatusUpdateApi,
  moviePageApi,
  movieProfilePageApi,
  movieProfileSaveApi,
  movieProfileStatusUpdateApi,
  movieSaveApi,
  movieScreeningPageApi,
  movieScreeningPriceListApi,
  movieScreeningPriceSaveApi,
  movieScreeningSeatGenerateApi,
  movieScreeningSaveApi,
  movieScreeningStatusUpdateApi,
  movieStatusUpdateApi,
} from '#/api/movie';
import { areaManageListQueryApi } from '#/api/base-data';
import {
  buildAreaIds,
  buildCityOptions,
  buildProvinceOptions,
  readAreaRows,
  type AreaSelectOption,
} from '#/utils/manage-area';

type MovieForm = MovieApi.MovieSaveParams & { id?: string };
type ProfileForm = Partial<MovieApi.MovieProfileSaveParams>;
type MediaForm = Partial<MovieApi.MovieMediaSaveParams>;
type ScreeningForm = Partial<MovieApi.MovieScreeningSaveParams>;
type PriceForm = Partial<MovieApi.MovieScreeningPriceSaveParams>;
type ScreeningIdPickerType = 'cinema' | 'movie' | 'screening';

const activeTab = ref('movies');
const movieLoaded = ref(false);
const screeningLoaded = ref(false);
const areaRows = ref<any[]>([]);
const provinceOptions = ref<AreaSelectOption[]>([{ label: '全部省份', value: '' }]);
const movieCityOptions = ref<AreaSelectOption[]>([{ label: '全部城市', value: '' }]);
const screeningCityOptions = ref<AreaSelectOption[]>([{ label: '全部城市', value: '' }]);

const statusOptions = [
  { label: '全部', value: '' },
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
];

const releaseStatusOptions = [
  { label: '全部', value: '' },
  { label: '待映', value: 1 },
  { label: '热映中', value: 2 },
  { label: '已下映', value: 3 },
];

const mediaTypeOptions = [
  { label: '海报', value: 1 },
  { label: '剧照', value: 2 },
  { label: '预告片', value: 3 },
  { label: '主创物料', value: 4 },
];

const movieFilters = reactive({
  areaId: '',
  movieId: '',
  movieName: '',
  programId: '',
  provinceAreaId: '',
  status: '' as number | string,
});
const moviePager = reactive({ page: 1, size: 20, total: 0 });
const movieRows = ref<MovieApi.MovieManageResult[]>([]);
const movieLoading = ref(false);
const movieDrawerVisible = ref(false);
const movieSaving = ref(false);

const movieForm = reactive<MovieForm>({
  actors: '',
  description: '',
  director: '',
  durationMinutes: '',
  id: '',
  language: '',
  movieAlias: '',
  movieName: '',
  poster: '',
  programId: '',
  region: '',
  releaseDate: '',
});

const selectedMovie = ref<MovieApi.MovieManageResult | null>(null);
const focusedMovie = ref<MovieApi.MovieManageResult | null>(null);
const profileDrawerVisible = ref(false);
const profileRows = ref<MovieApi.MovieProfileManageResult[]>([]);
const profileLoading = ref(false);
const profileEditing = ref(false);
const profileSaving = ref(false);
const profileForm = reactive<ProfileForm>({
  ageTips: '',
  boxOfficeAmount: '',
  distributor: '',
  genre: '',
  id: '',
  longDescription: '',
  movieId: '',
  producer: '',
  programId: '',
  ratingScore: '',
  releaseStatus: 1,
  wantWatchCount: '',
  watchedCount: '',
});

const mediaDrawerVisible = ref(false);
const mediaRows = ref<MovieApi.MovieMediaManageResult[]>([]);
const mediaLoading = ref(false);
const mediaEditing = ref(false);
const mediaSaving = ref(false);
const mediaForm = reactive<MediaForm>({
  auditStatus: 1,
  coverUrl: '',
  id: '',
  mediaType: 1,
  mediaUrl: '',
  movieId: '',
  sortOrder: 0,
  title: '',
});

const screeningFilters = reactive({
  areaId: '',
  cinemaId: '',
  movieId: '',
  provinceAreaId: '',
  screeningId: '',
  screeningStatus: '' as number | string,
});
const screeningPager = reactive({ page: 1, size: 20, total: 0 });
const screeningRows = ref<MovieApi.MovieScreeningManageResult[]>([]);
const screeningLoading = ref(false);
const screeningDrawerVisible = ref(false);
const screeningSaving = ref(false);
const screeningForm = reactive<ScreeningForm>({
  cinemaId: '',
  endTime: '',
  hallId: '',
  id: '',
  language: '',
  lowestPrice: '',
  movieId: '',
  programId: '',
  screeningStatus: 1,
  showDayTime: '',
  showTime: '',
  showWeekTime: '',
  stopSellTime: '',
  version: '',
});

const idPickerVisible = ref(false);
const idPickerType = ref<ScreeningIdPickerType>('movie');
const idPickerKeyword = ref('');
const idPickerManualId = ref('');
const idPickerLoading = ref(false);
const idPickerRows = ref<any[]>([]);
const idPickerPager = reactive({ page: 1, size: 10, total: 0 });

const selectedScreening = ref<MovieApi.MovieScreeningManageResult | null>(null);
const priceDrawerVisible = ref(false);
const priceRows = ref<MovieApi.MovieScreeningPriceManageResult[]>([]);
const priceLoading = ref(false);
const priceSaving = ref(false);
const priceForm = reactive<PriceForm>({
  id: '',
  price: '',
  priceName: '',
  remainNumber: '',
  screeningId: '',
  ticketCategoryId: '',
  totalNumber: '',
});

const reconcileDrawerVisible = ref(false);
const reconcileLoading = ref(false);
const reconcileResult = ref<MovieApi.MovieInventoryReconcileResult | null>(null);
const reconcileIssues = ref<MovieApi.MovieInventoryReconcileIssueResult[]>([]);
const issueLoading = ref(false);

const summary = computed(() => {
  const rows = movieRows.value;
  const enabled = rows.filter((item: any) => String(item.status ?? '1') === '1').length;
  const paidTicketCount = rows.reduce((sum: number, item: any) => sum + Number(item.paidTicketCount || 0), 0);
  const soldOutCount =
    screeningRows.value.filter((item: any) => item.soldOut === true || Number(item.dbRemainNumber || 0) === 0)
      .length || rows.reduce((sum: number, item: any) => sum + Number(item.soldOutScreeningCount || 0), 0);
  return [
    { label: '影片资料', value: moviePager.total || rows.length, unit: '部' },
    { label: '启用影片', value: enabled, unit: '部' },
    { label: '真实销量', value: paidTicketCount, unit: '张' },
    { label: '排片场次', value: screeningPager.total || screeningRows.value.length, unit: '场' },
    { label: '售罄场次', value: soldOutCount, unit: '场' },
  ];
});

const focusedMovieRows = computed(() => (focusedMovie.value ? [focusedMovie.value] : []));

const idPickerTitle = computed(() => {
  if (idPickerType.value === 'screening') return '选择场次';
  if (idPickerType.value === 'cinema') return '选择影院';
  return '选择电影';
});

const idPickerPlaceholder = computed(() => {
  if (idPickerType.value === 'screening') return '按场次 ID 直接筛选';
  if (idPickerType.value === 'cinema') return '按影院 ID 直接筛选';
  return '按电影 ID 直接筛选';
});

const idPickerKeywordPlaceholder = computed(() => {
  if (idPickerType.value === 'cinema') return '影院名称';
  if (idPickerType.value === 'movie') return '电影名称';
  return '场次选择会结合当前电影、影院和城市筛选';
});

function clearEmpty<T extends Record<string, any>>(params: T) {
  const result: Record<string, any> = {};
  Object.entries(params).forEach(([key, value]) => {
    if (Array.isArray(value) && value.length === 0) {
      return;
    }
    if (value !== '' && value !== undefined && value !== null) {
      result[key] = value;
    }
  });
  return result as Partial<T>;
}

function readRecords<T>(data: any): T[] {
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

function formatDate(value: unknown) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 19);
}

function statusText(status?: number | string) {
  return String(status ?? '1') === '1' ? '启用' : '停用';
}

function releaseStatusText(status?: number | string) {
  if (String(status) === '2') return '热映中';
  if (String(status) === '3') return '已下映';
  return '待映';
}

function mediaTypeText(type?: number | string) {
  return mediaTypeOptions.find((item) => String(item.value) === String(type))?.label ?? '物料';
}

function boolText(value: unknown) {
  return value === true || value === 1 ? '一致' : '异常';
}

async function loadAreaOptions() {
  try {
    const res = await areaManageListQueryApi();
    areaRows.value = readAreaRows(res);
    provinceOptions.value = buildProvinceOptions(areaRows.value);
    movieCityOptions.value = buildCityOptions(areaRows.value);
    screeningCityOptions.value = buildCityOptions(areaRows.value);
  } catch (error) {
    areaRows.value = [];
    provinceOptions.value = [{ label: '全部省份', value: '' }];
    movieCityOptions.value = [{ label: '全部城市', value: '' }];
    screeningCityOptions.value = [{ label: '全部城市', value: '' }];
  }
}

function onMovieProvinceChange() {
  movieFilters.areaId = '';
  movieCityOptions.value = buildCityOptions(areaRows.value, movieFilters.provinceAreaId);
}

function onScreeningProvinceChange() {
  screeningFilters.areaId = '';
  screeningCityOptions.value = buildCityOptions(areaRows.value, screeningFilters.provinceAreaId);
}

function resetMovieFilters() {
  Object.assign(movieFilters, {
    areaId: '',
    movieId: '',
    movieName: '',
    programId: '',
    provinceAreaId: '',
    status: '',
  });
  movieCityOptions.value = buildCityOptions(areaRows.value);
  searchMovies();
}

function resetScreeningFilters() {
  Object.assign(screeningFilters, {
    areaId: '',
    cinemaId: '',
    movieId: '',
    provinceAreaId: '',
    screeningId: '',
    screeningStatus: '',
  });
  focusedMovie.value = null;
  screeningCityOptions.value = buildCityOptions(areaRows.value);
  searchScreenings();
}

function resetMovieForm() {
  Object.assign(movieForm, {
    actors: '',
    description: '',
    director: '',
    durationMinutes: '',
    id: '',
    language: '',
    movieAlias: '',
    movieName: '',
    poster: '',
    programId: '',
    region: '',
    releaseDate: '',
  });
}

async function loadMovies() {
  movieLoading.value = true;
  try {
    const res = await moviePageApi(
      clearEmpty({
        areaId: movieFilters.areaId,
        areaIds: buildAreaIds(areaRows.value, movieFilters.areaId),
        movieId: movieFilters.movieId,
        movieName: movieFilters.movieName,
        pageNumber: moviePager.page,
        pageSize: moviePager.size,
        programId: movieFilters.programId,
        status: movieFilters.status,
      }) as MovieApi.MovieManageParams,
    );
    movieRows.value = readRecords<MovieApi.MovieManageResult>(res);
    moviePager.total = readTotal(res, movieRows.value.length);
    movieLoaded.value = true;
  } catch (error) {
    movieRows.value = [];
    ElMessage.error('电影资料加载失败，请确认后端服务已启动');
  } finally {
    movieLoading.value = false;
  }
}

function searchMovies() {
  moviePager.page = 1;
  return loadMovies();
}

function openMovieForm(row?: MovieApi.MovieManageResult) {
  resetMovieForm();
  if (row) {
    Object.assign(movieForm, {
      actors: row.actors ?? '',
      description: row.description ?? '',
      director: row.director ?? '',
      durationMinutes: row.durationMinutes ?? '',
      id: String(row.id ?? ''),
      language: row.language ?? '',
      movieAlias: row.movieAlias ?? '',
      movieName: row.movieName ?? '',
      poster: row.poster ?? '',
      programId: String(row.programId ?? ''),
      region: row.region ?? '',
      releaseDate: row.releaseDate ? String(row.releaseDate).slice(0, 10) : '',
    });
  }
  movieDrawerVisible.value = true;
}

async function saveMovie() {
  if (!movieForm.programId || !movieForm.movieName) {
    ElMessage.warning('请填写节目ID和电影名称');
    return;
  }
  movieSaving.value = true;
  try {
    await movieSaveApi(
      clearEmpty({
        ...movieForm,
        durationMinutes: movieForm.durationMinutes ? Number(movieForm.durationMinutes) : undefined,
      }) as MovieApi.MovieSaveParams,
    );
    ElMessage.success('电影资料已保存');
    movieDrawerVisible.value = false;
    await loadMovies();
  } catch (error) {
    ElMessage.error('保存失败，请检查节目ID是否存在');
  } finally {
    movieSaving.value = false;
  }
}

async function toggleMovieStatus(row: MovieApi.MovieManageResult) {
  const status = String(row.status ?? '1') === '1' ? 0 : 1;
  try {
    await ElMessageBox.confirm(`确认${status === 1 ? '启用' : '停用'}《${row.movieName}》？`, '状态变更', {
      type: status === 1 ? 'info' : 'warning',
    });
    await movieStatusUpdateApi({
      movieId: row.id,
      programId: row.programId,
      status,
    });
    ElMessage.success('状态已更新');
    await loadMovies();
  } catch (error) {
    if (String((error as Error)?.message || '') !== 'cancel') {
      ElMessage.error('状态更新失败');
    }
  }
}

function resetProfileForm(row?: MovieApi.MovieProfileManageResult) {
  Object.assign(profileForm, {
    ageTips: row?.ageTips ?? '',
    boxOfficeAmount: row?.boxOfficeAmount ?? '',
    distributor: row?.distributor ?? '',
    genre: row?.genre ?? '',
    id: row?.id ? String(row.id) : '',
    longDescription: row?.longDescription ?? '',
    movieId: selectedMovie.value?.id ?? row?.movieId ?? '',
    producer: row?.producer ?? '',
    programId: selectedMovie.value?.programId ?? row?.programId ?? '',
    ratingScore: row?.ratingScore ?? '',
    releaseStatus: row?.releaseStatus ?? 1,
    wantWatchCount: row?.wantWatchCount ?? '',
    watchedCount: row?.watchedCount ?? '',
  });
}

function startProfileEdit(row?: MovieApi.MovieProfileManageResult) {
  resetProfileForm(row);
  profileEditing.value = true;
}

function cancelProfileEdit() {
  resetProfileForm();
  profileEditing.value = false;
}

async function openProfileDrawer(row: MovieApi.MovieManageResult) {
  selectedMovie.value = row;
  resetProfileForm();
  profileEditing.value = false;
  profileDrawerVisible.value = true;
  await loadProfiles();
}

async function loadProfiles() {
  if (!selectedMovie.value) return;
  profileLoading.value = true;
  try {
    const res = await movieProfilePageApi({
      movieId: selectedMovie.value.id,
      pageNumber: 1,
      pageSize: 20,
      programId: selectedMovie.value.programId,
    });
    profileRows.value = readRecords<MovieApi.MovieProfileManageResult>(res);
  } catch (error) {
    profileRows.value = [];
    ElMessage.error('发行资料加载失败');
  } finally {
    profileLoading.value = false;
  }
}

async function saveProfile() {
  if (!profileForm.movieId || !profileForm.programId) {
    ElMessage.warning('缺少电影ID或节目ID');
    return;
  }
  profileSaving.value = true;
  try {
    await movieProfileSaveApi(clearEmpty(profileForm) as MovieApi.MovieProfileSaveParams);
    ElMessage.success('发行资料已保存');
    profileEditing.value = false;
    resetProfileForm();
    await loadProfiles();
  } catch (error) {
    ElMessage.error('发行资料保存失败');
  } finally {
    profileSaving.value = false;
  }
}

async function toggleProfileStatus(row: MovieApi.MovieProfileManageResult) {
  try {
    await movieProfileStatusUpdateApi({
      profileId: row.id,
      programId: row.programId,
      status: String(row.status ?? '1') === '1' ? 0 : 1,
    });
    await loadProfiles();
    ElMessage.success('发行资料状态已更新');
  } catch (error) {
    ElMessage.error('发行资料状态更新失败');
  }
}

function resetMediaForm(row?: MovieApi.MovieMediaManageResult) {
  Object.assign(mediaForm, {
    auditStatus: row?.auditStatus ?? 1,
    coverUrl: row?.coverUrl ?? '',
    id: row?.id ? String(row.id) : '',
    mediaType: row?.mediaType ?? 1,
    mediaUrl: row?.mediaUrl ?? '',
    movieId: selectedMovie.value?.id ?? row?.movieId ?? '',
    sortOrder: row?.sortOrder ?? 0,
    title: row?.title ?? '',
  });
}

function startMediaEdit(row?: MovieApi.MovieMediaManageResult) {
  resetMediaForm(row);
  mediaEditing.value = true;
}

function cancelMediaEdit() {
  resetMediaForm();
  mediaEditing.value = false;
}

async function openMediaDrawer(row: MovieApi.MovieManageResult) {
  selectedMovie.value = row;
  resetMediaForm();
  mediaEditing.value = false;
  mediaDrawerVisible.value = true;
  await loadMediaRows();
}

async function loadMediaRows() {
  if (!selectedMovie.value) return;
  mediaLoading.value = true;
  try {
    const res = await movieMediaPageApi({
      movieId: selectedMovie.value.id,
      pageNumber: 1,
      pageSize: 50,
    });
    mediaRows.value = readRecords<MovieApi.MovieMediaManageResult>(res);
  } catch (error) {
    mediaRows.value = [];
    ElMessage.error('电影物料加载失败');
  } finally {
    mediaLoading.value = false;
  }
}

async function saveMedia() {
  if (!mediaForm.movieId || !mediaForm.mediaUrl || !mediaForm.mediaType) {
    ElMessage.warning('请填写物料类型和资源地址');
    return;
  }
  mediaSaving.value = true;
  try {
    await movieMediaSaveApi(clearEmpty(mediaForm) as MovieApi.MovieMediaSaveParams);
    ElMessage.success('电影物料已保存');
    mediaEditing.value = false;
    resetMediaForm();
    await loadMediaRows();
  } catch (error) {
    ElMessage.error('电影物料保存失败');
  } finally {
    mediaSaving.value = false;
  }
}

async function toggleMediaStatus(row: MovieApi.MovieMediaManageResult) {
  try {
    await movieMediaStatusUpdateApi({
      mediaId: row.id,
      movieId: row.movieId,
      status: String(row.status ?? '1') === '1' ? 0 : 1,
    });
    await loadMediaRows();
    ElMessage.success('电影物料状态已更新');
  } catch (error) {
    ElMessage.error('电影物料状态更新失败');
  }
}

async function loadScreenings() {
  screeningLoading.value = true;
  try {
    const res = await movieScreeningPageApi(
      clearEmpty({
        areaId: screeningFilters.areaId,
        areaIds: buildAreaIds(areaRows.value, screeningFilters.areaId),
        cinemaId: screeningFilters.cinemaId,
        movieId: screeningFilters.movieId,
        pageNumber: screeningPager.page,
        pageSize: screeningPager.size,
        screeningId: screeningFilters.screeningId,
        screeningStatus: screeningFilters.screeningStatus,
      }) as MovieApi.MovieScreeningManageParams,
    );
    screeningRows.value = readRecords<MovieApi.MovieScreeningManageResult>(res);
    screeningPager.total = readTotal(res, screeningRows.value.length);
    screeningLoaded.value = true;
  } catch (error) {
    screeningRows.value = [];
    ElMessage.error('排片场次加载失败');
  } finally {
    screeningLoading.value = false;
  }
}

function searchScreenings() {
  screeningPager.page = 1;
  return loadScreenings();
}

async function viewMovieScreenings(row: MovieApi.MovieManageResult) {
  focusedMovie.value = row;
  screeningFilters.movieId = String(row.id ?? '');
  screeningFilters.screeningId = '';
  activeTab.value = 'screenings';
  await searchScreenings();
}

function expandMovieList() {
  activeTab.value = 'movies';
}

function clearFocusedMovieFilter() {
  focusedMovie.value = null;
  screeningFilters.movieId = '';
  searchScreenings();
}

function currentIdPickerFilterValue() {
  if (idPickerType.value === 'screening') return String(screeningFilters.screeningId || '');
  if (idPickerType.value === 'cinema') return String(screeningFilters.cinemaId || '');
  return String(screeningFilters.movieId || '');
}

function setIdPickerFilterValue(value: string) {
  if (idPickerType.value === 'screening') {
    screeningFilters.screeningId = value;
    return;
  }
  if (idPickerType.value === 'cinema') {
    screeningFilters.cinemaId = value;
    return;
  }
  screeningFilters.movieId = value;
}

function openIdPicker(type: ScreeningIdPickerType) {
  idPickerType.value = type;
  idPickerManualId.value = currentIdPickerFilterValue();
  idPickerKeyword.value = '';
  idPickerPager.page = 1;
  idPickerVisible.value = true;
  loadIdPickerRows();
}

async function loadIdPickerRows() {
  idPickerLoading.value = true;
  try {
    const areaId = String(screeningFilters.areaId || '');
    const baseParams: any = {
      pageNumber: idPickerPager.page,
      pageSize: idPickerPager.size,
    };
    if (idPickerType.value === 'movie') {
      const params: any = {
        ...baseParams,
        movieId: idPickerManualId.value,
        movieName: idPickerKeyword.value.trim(),
      };
      if (areaId) {
        params.areaId = areaId;
        params.areaIds = buildAreaIds(areaRows.value, areaId);
      }
      const res = await moviePageApi(clearEmpty(params) as MovieApi.MovieManageParams);
      idPickerRows.value = readRecords<MovieApi.MovieManageResult>(res);
      idPickerPager.total = readTotal(res, idPickerRows.value.length);
      return;
    }
    if (idPickerType.value === 'cinema') {
      const res = await cinemaPageApi(
        clearEmpty({
          ...baseParams,
          areaId,
          cinemaId: idPickerManualId.value,
          cinemaName: idPickerKeyword.value.trim(),
          status: 1,
        }) as MovieApi.CinemaManageParams,
      );
      idPickerRows.value = readRecords<MovieApi.CinemaManageResult>(res);
      idPickerPager.total = readTotal(res, idPickerRows.value.length);
      return;
    }
    const params: any = {
      ...baseParams,
      cinemaId: screeningFilters.cinemaId,
      movieId: screeningFilters.movieId,
      screeningId: idPickerManualId.value,
      screeningStatus: screeningFilters.screeningStatus,
    };
    if (areaId) {
      params.areaId = areaId;
      params.areaIds = buildAreaIds(areaRows.value, areaId);
    }
    const res = await movieScreeningPageApi(clearEmpty(params) as MovieApi.MovieScreeningManageParams);
    idPickerRows.value = readRecords<MovieApi.MovieScreeningManageResult>(res);
    idPickerPager.total = readTotal(res, idPickerRows.value.length);
  } catch (error) {
    idPickerRows.value = [];
    idPickerPager.total = 0;
    ElMessage.error('选择器数据加载失败');
  } finally {
    idPickerLoading.value = false;
  }
}

function applyManualIdPicker() {
  setIdPickerFilterValue(idPickerManualId.value.trim());
  idPickerVisible.value = false;
  searchScreenings();
}

function chooseIdPickerRow(row: any) {
  if (idPickerType.value === 'movie') {
    screeningFilters.movieId = String(row?.id ?? '');
    focusedMovie.value = row as MovieApi.MovieManageResult;
  } else if (idPickerType.value === 'cinema') {
    screeningFilters.cinemaId = String(row?.id ?? '');
  } else {
    screeningFilters.screeningId = String(row?.id ?? '');
  }
  idPickerVisible.value = false;
  searchScreenings();
}

function resetScreeningForm(row?: MovieApi.MovieScreeningManageResult) {
  Object.assign(screeningForm, {
    cinemaId: row?.cinemaId ?? '',
    endTime: row?.endTime ?? '',
    hallId: row?.hallId ?? '',
    id: row?.id ?? '',
    language: row?.language ?? '',
    lowestPrice: row?.lowestPrice ?? '',
    movieId: row?.movieId ?? '',
    programId: row?.programId ?? '',
    screeningStatus: row?.screeningStatus ?? 1,
    showDayTime: row?.showDayTime ? String(row.showDayTime).slice(0, 10) : '',
    showTime: row?.showTime ?? '',
    showWeekTime: row?.showWeekTime ?? '',
    stopSellTime: row?.stopSellTime ?? '',
    version: row?.version ?? '',
  });
}

function openScreeningForm(row?: MovieApi.MovieScreeningManageResult) {
  resetScreeningForm(row);
  screeningDrawerVisible.value = true;
}

async function saveScreening() {
  if (!screeningForm.movieId || !screeningForm.programId || !screeningForm.cinemaId || !screeningForm.hallId || !screeningForm.showTime) {
    ElMessage.warning('请填写电影、节目、影院、影厅和放映时间');
    return;
  }
  screeningSaving.value = true;
  try {
    await movieScreeningSaveApi(clearEmpty(screeningForm) as MovieApi.MovieScreeningSaveParams);
    ElMessage.success('排片场次已保存');
    screeningDrawerVisible.value = false;
    await loadScreenings();
  } catch (error) {
    ElMessage.error('排片场次保存失败');
  } finally {
    screeningSaving.value = false;
  }
}

async function toggleScreeningStatus(row: MovieApi.MovieScreeningManageResult) {
  try {
    await movieScreeningStatusUpdateApi({
      screeningId: row.id,
      screeningStatus: String(row.screeningStatus ?? '1') === '1' ? 0 : 1,
    });
    await loadScreenings();
    ElMessage.success('场次状态已更新');
  } catch (error) {
    ElMessage.error('场次状态更新失败');
  }
}

function resetPriceForm(row?: MovieApi.MovieScreeningPriceManageResult) {
  Object.assign(priceForm, {
    id: row?.id ?? '',
    price: row?.price ?? '',
    priceName: row?.priceName ?? '',
    remainNumber: row?.dbRemainNumber ?? row?.totalNumber ?? '',
    screeningId: selectedScreening.value?.id ?? row?.screeningId ?? '',
    ticketCategoryId: row?.ticketCategoryId ?? '',
    totalNumber: row?.totalNumber ?? '',
  });
}

async function openPriceDrawer(row: MovieApi.MovieScreeningManageResult) {
  selectedScreening.value = row;
  resetPriceForm();
  priceDrawerVisible.value = true;
  await loadPriceRows();
}

async function loadPriceRows() {
  if (!selectedScreening.value) return;
  priceLoading.value = true;
  try {
    const res = await movieScreeningPriceListApi({ screeningId: selectedScreening.value.id });
    priceRows.value = readRecords<MovieApi.MovieScreeningPriceManageResult>(res);
  } catch (error) {
    priceRows.value = [];
    ElMessage.error('票价库存加载失败');
  } finally {
    priceLoading.value = false;
  }
}

async function savePrice() {
  if (!priceForm.screeningId || !priceForm.ticketCategoryId || !priceForm.price || !priceForm.totalNumber || !priceForm.remainNumber) {
    ElMessage.warning('请填写票档、票价、总量和余量');
    return;
  }
  priceSaving.value = true;
  try {
    await movieScreeningPriceSaveApi(clearEmpty(priceForm) as MovieApi.MovieScreeningPriceSaveParams);
    ElMessage.success('票价库存已保存');
    resetPriceForm();
    await loadPriceRows();
  } catch (error) {
    ElMessage.error('票价库存保存失败');
  } finally {
    priceSaving.value = false;
  }
}

async function generateSeatByPrice(row: MovieApi.MovieScreeningPriceManageResult) {
  try {
    const count = await movieScreeningSeatGenerateApi({
      price: row.price ?? 0,
      priceName: row.priceName,
      screeningId: row.screeningId,
      ticketCategoryId: row.ticketCategoryId,
    });
    ElMessage.success(`已按影厅模板生成 ${count ?? 0} 个座位`);
  } catch (error) {
    ElMessage.error('座位生成失败，请确认影厅模板和票档配置');
  }
}

async function openReconcileDrawer(row: MovieApi.MovieScreeningManageResult) {
  selectedScreening.value = row;
  reconcileDrawerVisible.value = true;
  await Promise.all([loadReconcile(), loadReconcileIssues()]);
}

async function loadReconcile() {
  if (!selectedScreening.value) return;
  reconcileLoading.value = true;
  try {
    reconcileResult.value = await movieInventoryReconcileApi({ screeningId: selectedScreening.value.id });
  } catch (error) {
    reconcileResult.value = null;
    ElMessage.error('库存对账诊断失败');
  } finally {
    reconcileLoading.value = false;
  }
}

async function loadReconcileIssues() {
  if (!selectedScreening.value) return;
  issueLoading.value = true;
  try {
    const res = await movieInventoryReconcileIssuePageApi({
      pageNumber: 1,
      pageSize: 20,
      screeningId: selectedScreening.value.id,
    });
    reconcileIssues.value = readRecords<MovieApi.MovieInventoryReconcileIssueResult>(res);
  } catch (error) {
    reconcileIssues.value = [];
  } finally {
    issueLoading.value = false;
  }
}

async function handleIssue(row: MovieApi.MovieInventoryReconcileIssueResult, issueStatus: number) {
  try {
    await movieInventoryReconcileIssueHandleApi({
      handleRemark: issueStatus === 2 ? '后台确认已处理' : '后台确认忽略',
      id: row.id,
      issueStatus,
      screeningId: row.screeningId || selectedScreening.value?.id || '',
    });
    ElMessage.success('工单状态已更新');
    await loadReconcileIssues();
  } catch (error) {
    ElMessage.error('工单处理失败');
  }
}

watch(activeTab, (value) => {
  if (value === 'movies' && !movieLoaded.value) {
    loadMovies();
  }
  if (value === 'screenings' && !screeningLoaded.value) {
    loadScreenings();
  }
});

onMounted(async () => {
  await loadAreaOptions();
  loadMovies();
});
</script>

<template>
  <Page auto-content-height>
    <div class="movie-workbench">
      <div class="workbench-header">
        <div>
          <div class="eyebrow">电影运营</div>
          <h1>电影内容与排片工作台</h1>
        </div>
        <div class="header-actions">
          <el-button type="primary" @click="openMovieForm()">新增电影</el-button>
        </div>
      </div>

      <div class="metric-strip">
        <div v-for="item in summary" :key="item.label" class="metric-item">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}<em>{{ item.unit }}</em></strong>
        </div>
      </div>

      <el-tabs v-model="activeTab" class="workbench-tabs">
        <el-tab-pane label="影片资料" name="movies">
          <div class="toolbar-row">
            <el-select v-model="movieFilters.provinceAreaId" clearable placeholder="省份" @change="onMovieProvinceChange">
              <el-option
                v-for="item in provinceOptions"
                :key="item.value || 'all-province'"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <el-select v-model="movieFilters.areaId" clearable placeholder="城市">
              <el-option
                v-for="item in movieCityOptions"
                :key="item.value || 'all-city'"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <el-input v-model="movieFilters.movieName" clearable placeholder="电影名称" />
            <el-input v-model="movieFilters.movieId" clearable placeholder="电影ID" />
            <el-input v-model="movieFilters.programId" clearable placeholder="节目ID" />
            <el-select v-model="movieFilters.status" placeholder="状态">
              <el-option
                v-for="item in statusOptions"
                :key="item.label"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <el-button type="primary" @click="searchMovies">查询</el-button>
            <el-button @click="resetMovieFilters">
              重置
            </el-button>
          </div>

          <el-table v-loading="movieLoading" :data="movieRows" border stripe class="dense-table">
            <el-table-column label="影片" min-width="260">
              <template #default="{ row }">
                <div class="movie-title-cell">
                  <img v-if="row.poster" :src="row.poster" :alt="row.movieName" />
                  <div v-else class="poster-fallback">{{ String(row.movieName || '?').slice(0, 1) }}</div>
                  <div>
                    <strong>{{ row.movieName }}</strong>
                    <span>{{ row.movieAlias || '未配置别名' }}</span>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="programId" label="节目ID" width="130" />
            <el-table-column prop="director" label="导演" min-width="120" />
            <el-table-column prop="actors" label="主演" min-width="220" show-overflow-tooltip />
            <el-table-column label="片长/语种" width="150">
              <template #default="{ row }">
                {{ row.durationMinutes || '-' }} 分钟 · {{ row.language || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="region" label="地区" width="130" />
            <el-table-column label="上映日期" width="130">
              <template #default="{ row }">{{ formatDate(row.releaseDate).slice(0, 10) }}</template>
            </el-table-column>
            <el-table-column label="真实销量" width="120">
              <template #default="{ row }">
                {{ row.paidTicketCount ?? 0 }} 张
              </template>
            </el-table-column>
            <el-table-column label="排片/售罄" width="120">
              <template #default="{ row }">
                {{ row.screeningCount ?? 0 }} / {{ row.soldOutScreeningCount ?? 0 }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="String(row.status ?? '1') === '1' ? 'success' : 'info'" effect="plain">
                  {{ statusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column fixed="right" label="操作" width="360">
              <template #default="{ row }">
                <el-button link type="primary" @click="openMovieForm(row)">编辑</el-button>
                <el-button link type="primary" @click="viewMovieScreenings(row)">查看场次</el-button>
                <el-button link type="primary" @click="openProfileDrawer(row)">发行资料</el-button>
                <el-button link type="primary" @click="openMediaDrawer(row)">物料</el-button>
                <el-button link :type="String(row.status ?? '1') === '1' ? 'warning' : 'success'" @click="toggleMovieStatus(row)">
                  {{ String(row.status ?? '1') === '1' ? '停用' : '启用' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pager-row">
            <el-pagination
              v-model:current-page="moviePager.page"
              v-model:page-size="moviePager.size"
              background
              layout="total, sizes, prev, pager, next"
              :page-sizes="[10, 20, 50, 100]"
              :total="moviePager.total"
              @current-change="loadMovies"
              @size-change="searchMovies"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="排片库存" name="screenings">
          <div v-if="focusedMovie" class="focused-movie-panel">
            <div class="focused-panel-title">
              <span>当前电影</span>
              <div>
                <el-button link type="primary" @click="expandMovieList">展开电影列表</el-button>
                <el-button link type="info" @click="clearFocusedMovieFilter">清除电影筛选</el-button>
              </div>
            </div>
            <el-table :data="focusedMovieRows" class="dense-table focused-movie-table">
              <el-table-column label="影片" min-width="260">
                <template #default="{ row }">
                  <div class="movie-title-cell">
                    <img v-if="row.poster" :src="row.poster" :alt="row.movieName" />
                    <div v-else class="poster-empty">无图</div>
                    <div>
                      <strong>{{ row.movieName }}</strong>
                      <span>{{ row.movieAlias || '' }}</span>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="director" label="导演" min-width="120" />
              <el-table-column prop="region" label="地区" width="120" />
              <el-table-column label="真实销量" width="120">
                <template #default="{ row }">{{ row.paidTicketCount ?? 0 }} 张</template>
              </el-table-column>
              <el-table-column label="排片/售罄" width="120">
                <template #default="{ row }">{{ row.screeningCount ?? 0 }} / {{ row.soldOutScreeningCount ?? 0 }}</template>
              </el-table-column>
            </el-table>
          </div>
          <div class="toolbar-row">
            <el-select v-model="screeningFilters.provinceAreaId" clearable placeholder="省份" @change="onScreeningProvinceChange">
              <el-option
                v-for="item in provinceOptions"
                :key="item.value || 'screening-all-province'"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <el-select v-model="screeningFilters.areaId" clearable placeholder="城市">
              <el-option
                v-for="item in screeningCityOptions"
                :key="item.value || 'screening-all-city'"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <el-input v-model="screeningFilters.movieId" clearable placeholder="电影ID">
              <template #append>
                <el-button @click="openIdPicker('movie')">选择</el-button>
              </template>
            </el-input>
            <el-input v-model="screeningFilters.screeningId" clearable placeholder="场次ID">
              <template #append>
                <el-button @click="openIdPicker('screening')">选择</el-button>
              </template>
            </el-input>
            <el-input v-model="screeningFilters.cinemaId" clearable placeholder="影院ID">
              <template #append>
                <el-button @click="openIdPicker('cinema')">选择</el-button>
              </template>
            </el-input>
            <el-select v-model="screeningFilters.screeningStatus" placeholder="售卖状态">
              <el-option
                v-for="item in statusOptions"
                :key="item.label"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <el-button type="primary" @click="searchScreenings">查询</el-button>
            <el-button @click="openScreeningForm()">新增场次</el-button>
            <el-button @click="resetScreeningFilters">重置</el-button>
          </div>

          <el-table v-loading="screeningLoading" :data="screeningRows" border stripe class="dense-table">
            <el-table-column prop="id" label="场次ID" width="150" />
            <el-table-column label="影院/影厅" min-width="240">
              <template #default="{ row }">
                <strong>{{ row.cinemaName || row.cinemaId }}</strong>
                <span class="subline">{{ row.hallName || row.hallId }} · {{ row.hallType || '标准厅' }}</span>
                <span class="subline">{{ row.cityName || '-' }} {{ row.districtName || '' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="放映时间" min-width="170">
              <template #default="{ row }">{{ formatDate(row.showTime) }}</template>
            </el-table-column>
            <el-table-column label="语言/版本" width="150">
              <template #default="{ row }">{{ row.language || '-' }} · {{ row.version || '-' }}</template>
            </el-table-column>
            <el-table-column prop="lowestPrice" label="最低价" width="100" />
            <el-table-column label="库存" width="150">
              <template #default="{ row }">
                <span>可售 {{ row.dbRemainNumber ?? 0 }} / {{ row.totalNumber ?? 0 }}</span>
                <span class="subline">锁定 {{ row.lockedNumber ?? 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="真实销量" width="110">
              <template #default="{ row }">{{ row.soldNumber ?? row.paidTicketCount ?? 0 }} 张</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag
                  :type="row.soldOut ? 'danger' : String(row.screeningStatus ?? '1') === '1' ? 'success' : 'info'"
                  effect="plain"
                >
                  {{ row.soldOut ? '售罄' : statusText(row.screeningStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column fixed="right" label="操作" width="320">
              <template #default="{ row }">
                <el-button link type="primary" @click="openScreeningForm(row)">编辑</el-button>
                <el-button link type="primary" @click="openPriceDrawer(row)">票价库存</el-button>
                <el-button link type="primary" @click="openReconcileDrawer(row)">库存对账</el-button>
                <el-button link :type="String(row.screeningStatus ?? '1') === '1' ? 'warning' : 'success'" @click="toggleScreeningStatus(row)">
                  {{ String(row.screeningStatus ?? '1') === '1' ? '下架' : '上架' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pager-row">
            <el-pagination
              v-model:current-page="screeningPager.page"
              v-model:page-size="screeningPager.size"
              background
              layout="total, sizes, prev, pager, next"
              :page-sizes="[10, 20, 50, 100]"
              :total="screeningPager.total"
              @current-change="loadScreenings"
              @size-change="searchScreenings"
            />
          </div>
        </el-tab-pane>

      </el-tabs>

      <el-dialog v-model="idPickerVisible" :title="idPickerTitle" width="860px" destroy-on-close>
        <div class="id-picker-panel">
          <div class="id-picker-toolbar">
            <el-input v-model="idPickerManualId" clearable :placeholder="idPickerPlaceholder" />
            <el-input
              v-model="idPickerKeyword"
              clearable
              :disabled="idPickerType === 'screening'"
              :placeholder="idPickerKeywordPlaceholder"
              @keydown.enter="() => { idPickerPager.page = 1; loadIdPickerRows(); }"
            />
            <el-button type="primary" @click="() => { idPickerPager.page = 1; loadIdPickerRows(); }">查询</el-button>
            <el-button @click="applyManualIdPicker">直接使用ID</el-button>
          </div>
          <el-table v-loading="idPickerLoading" :data="idPickerRows" class="dense-table">
            <el-table-column v-if="idPickerType === 'movie'" label="电影" min-width="260">
              <template #default="{ row }">
                <div class="movie-title-cell">
                  <img v-if="row.poster" :src="row.poster" :alt="row.movieName" />
                  <div v-else class="poster-empty">无图</div>
                  <div>
                    <strong>{{ row.movieName }}</strong>
                    <span>ID {{ row.id }} · 节目 {{ row.programId }}</span>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column v-if="idPickerType === 'movie'" prop="director" label="导演" width="120" />
            <el-table-column v-if="idPickerType === 'movie'" label="销量/排片" width="130">
              <template #default="{ row }">{{ row.paidTicketCount ?? 0 }} 张 / {{ row.screeningCount ?? 0 }} 场</template>
            </el-table-column>

            <el-table-column v-if="idPickerType === 'screening'" prop="id" label="场次ID" width="150" />
            <el-table-column v-if="idPickerType === 'screening'" label="影院/影厅" min-width="250">
              <template #default="{ row }">
                <strong>{{ row.cinemaName || row.cinemaId }}</strong>
                <span class="subline">{{ row.hallName || row.hallId }} · {{ row.hallType || '标准厅' }}</span>
                <span class="subline">{{ row.cityName || '-' }} {{ row.districtName || '' }}</span>
              </template>
            </el-table-column>
            <el-table-column v-if="idPickerType === 'screening'" label="放映时间" min-width="170">
              <template #default="{ row }">{{ formatDate(row.showTime) }}</template>
            </el-table-column>
            <el-table-column v-if="idPickerType === 'screening'" label="语言/版本" width="140">
              <template #default="{ row }">{{ row.language || '-' }} · {{ row.version || '-' }}</template>
            </el-table-column>
            <el-table-column v-if="idPickerType === 'screening'" label="库存/销量" width="150">
              <template #default="{ row }">
                <span>可售 {{ row.dbRemainNumber ?? 0 }} / {{ row.totalNumber ?? 0 }}</span>
                <span class="subline">已售 {{ row.soldNumber ?? row.paidTicketCount ?? 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column v-if="idPickerType === 'screening'" label="状态" width="90">
              <template #default="{ row }">
                <el-tag
                  :type="row.soldOut ? 'danger' : String(row.screeningStatus ?? '1') === '1' ? 'success' : 'info'"
                  effect="plain"
                >
                  {{ row.soldOut ? '售罄' : statusText(row.screeningStatus) }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column v-if="idPickerType === 'cinema'" prop="id" label="影院ID" width="150" />
            <el-table-column v-if="idPickerType === 'cinema'" label="影院" min-width="260">
              <template #default="{ row }">
                <strong>{{ row.cinemaName }}</strong>
                <span class="subline">{{ row.cityName || '-' }} {{ row.districtName || '' }} · {{ row.businessArea || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column v-if="idPickerType === 'cinema'" prop="brandName" label="品牌" width="140" />

            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="chooseIdPickerRow(row)">选择</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pager-row">
            <el-pagination
              v-model:current-page="idPickerPager.page"
              v-model:page-size="idPickerPager.size"
              background
              layout="total, sizes, prev, pager, next"
              :page-sizes="[10, 20, 50]"
              :total="idPickerPager.total"
              @current-change="loadIdPickerRows"
              @size-change="() => { idPickerPager.page = 1; loadIdPickerRows(); }"
            />
          </div>
        </div>
      </el-dialog>

      <el-drawer v-model="movieDrawerVisible" size="560px" :title="movieForm.id ? '编辑电影资料' : '新增电影资料'">
        <el-form label-width="96px" class="drawer-form">
          <el-form-item label="节目ID" required>
            <el-input v-model="movieForm.programId" placeholder="关联 d_program.id" />
          </el-form-item>
          <el-form-item label="电影名称" required>
            <el-input v-model="movieForm.movieName" />
          </el-form-item>
          <el-form-item label="英文/别名">
            <el-input v-model="movieForm.movieAlias" />
          </el-form-item>
          <el-form-item label="导演">
            <el-input v-model="movieForm.director" />
          </el-form-item>
          <el-form-item label="主演">
            <el-input v-model="movieForm.actors" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="片长">
            <el-input v-model="movieForm.durationMinutes" placeholder="分钟" />
          </el-form-item>
          <el-form-item label="语言">
            <el-input v-model="movieForm.language" />
          </el-form-item>
          <el-form-item label="地区">
            <el-input v-model="movieForm.region" />
          </el-form-item>
          <el-form-item label="上映日期">
            <el-date-picker v-model="movieForm.releaseDate" value-format="YYYY-MM-DD" type="date" class="w-full" />
          </el-form-item>
          <el-form-item label="海报地址">
            <el-input v-model="movieForm.poster" />
          </el-form-item>
          <el-form-item label="简介">
            <el-input v-model="movieForm.description" type="textarea" :rows="4" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="movieDrawerVisible = false">取消</el-button>
          <el-button type="primary" :loading="movieSaving" @click="saveMovie">保存</el-button>
        </template>
      </el-drawer>

      <el-drawer v-model="profileDrawerVisible" size="760px" title="发行资料">
        <div class="drawer-section-head">
          <div class="context-title">{{ selectedMovie?.movieName }}</div>
          <el-button type="primary" plain @click="startProfileEdit()">新增资料</el-button>
        </div>
        <el-table v-loading="profileLoading" :data="profileRows" border class="dense-table mb-4">
          <el-table-column prop="genre" label="类型" min-width="140" />
          <el-table-column label="发行状态" width="100">
            <template #default="{ row }">{{ releaseStatusText(row.releaseStatus) }}</template>
          </el-table-column>
          <el-table-column prop="ratingScore" label="评分" width="90" />
          <el-table-column prop="wantWatchCount" label="想看" width="110" />
          <el-table-column prop="boxOfficeAmount" label="票房" width="120" />
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button link type="primary" @click="startProfileEdit(row)">编辑</el-button>
              <el-button link :type="String(row.status ?? '1') === '1' ? 'warning' : 'success'" @click="toggleProfileStatus(row)">
                {{ String(row.status ?? '1') === '1' ? '停用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-form v-if="profileEditing" label-width="96px" class="drawer-form split-form edit-panel">
          <el-form-item label="类型">
            <el-input v-model="profileForm.genre" />
          </el-form-item>
          <el-form-item label="发行状态">
            <el-select v-model="profileForm.releaseStatus" class="w-full">
              <el-option
                v-for="item in releaseStatusOptions.slice(1)"
                :key="item.label"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="想看人数">
            <el-input v-model="profileForm.wantWatchCount" />
          </el-form-item>
          <el-form-item label="已看人数">
            <el-input v-model="profileForm.watchedCount" />
          </el-form-item>
          <el-form-item label="评分">
            <el-input v-model="profileForm.ratingScore" />
          </el-form-item>
          <el-form-item label="票房">
            <el-input v-model="profileForm.boxOfficeAmount" />
          </el-form-item>
          <el-form-item label="出品方">
            <el-input v-model="profileForm.producer" />
          </el-form-item>
          <el-form-item label="发行方">
            <el-input v-model="profileForm.distributor" />
          </el-form-item>
          <el-form-item label="年龄提示" class="span-2">
            <el-input v-model="profileForm.ageTips" />
          </el-form-item>
          <el-form-item label="长简介" class="span-2">
            <el-input v-model="profileForm.longDescription" type="textarea" :rows="4" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button v-if="profileEditing" @click="cancelProfileEdit">取消编辑</el-button>
          <el-button @click="profileDrawerVisible = false">关闭</el-button>
          <el-button v-if="profileEditing" type="primary" :loading="profileSaving" @click="saveProfile">保存发行资料</el-button>
        </template>
      </el-drawer>

      <el-drawer v-model="mediaDrawerVisible" size="820px" title="电影物料">
        <div class="drawer-section-head">
          <div class="context-title">{{ selectedMovie?.movieName }}</div>
          <el-button type="primary" plain @click="startMediaEdit()">新增物料</el-button>
        </div>
        <el-table v-loading="mediaLoading" :data="mediaRows" border class="dense-table mb-4">
          <el-table-column label="类型" width="90">
            <template #default="{ row }">{{ mediaTypeText(row.mediaType) }}</template>
          </el-table-column>
          <el-table-column prop="title" label="标题" min-width="160" />
          <el-table-column prop="mediaUrl" label="资源地址" min-width="260" show-overflow-tooltip />
          <el-table-column prop="sortOrder" label="排序" width="80" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">{{ statusText(row.status) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button link type="primary" @click="startMediaEdit(row)">编辑</el-button>
              <el-button link :type="String(row.status ?? '1') === '1' ? 'warning' : 'success'" @click="toggleMediaStatus(row)">
                {{ String(row.status ?? '1') === '1' ? '停用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-form v-if="mediaEditing" label-width="96px" class="drawer-form split-form edit-panel">
          <el-form-item label="物料类型">
            <el-select v-model="mediaForm.mediaType" class="w-full">
              <el-option
                v-for="item in mediaTypeOptions"
                :key="item.label"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="排序">
            <el-input v-model="mediaForm.sortOrder" />
          </el-form-item>
          <el-form-item label="标题" class="span-2">
            <el-input v-model="mediaForm.title" />
          </el-form-item>
          <el-form-item label="封面地址" class="span-2">
            <el-input v-model="mediaForm.coverUrl" />
          </el-form-item>
          <el-form-item label="资源地址" class="span-2" required>
            <el-input v-model="mediaForm.mediaUrl" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button v-if="mediaEditing" @click="cancelMediaEdit">取消编辑</el-button>
          <el-button @click="mediaDrawerVisible = false">关闭</el-button>
          <el-button v-if="mediaEditing" type="primary" :loading="mediaSaving" @click="saveMedia">保存物料</el-button>
        </template>
      </el-drawer>

      <el-drawer v-model="screeningDrawerVisible" size="560px" :title="screeningForm.id ? '编辑排片场次' : '新增排片场次'">
        <el-form label-width="96px" class="drawer-form">
          <el-form-item label="电影ID" required>
            <el-input v-model="screeningForm.movieId" />
          </el-form-item>
          <el-form-item label="节目ID" required>
            <el-input v-model="screeningForm.programId" />
          </el-form-item>
          <el-form-item label="影院ID" required>
            <el-input v-model="screeningForm.cinemaId" />
          </el-form-item>
          <el-form-item label="影厅ID" required>
            <el-input v-model="screeningForm.hallId" />
          </el-form-item>
          <el-form-item label="放映时间" required>
            <el-date-picker v-model="screeningForm.showTime" value-format="YYYY-MM-DD HH:mm:ss" type="datetime" class="w-full" />
          </el-form-item>
          <el-form-item label="放映日期">
            <el-date-picker v-model="screeningForm.showDayTime" value-format="YYYY-MM-DD" type="date" class="w-full" />
          </el-form-item>
          <el-form-item label="星期">
            <el-input v-model="screeningForm.showWeekTime" placeholder="周五" />
          </el-form-item>
          <el-form-item label="结束时间">
            <el-date-picker v-model="screeningForm.endTime" value-format="YYYY-MM-DD HH:mm:ss" type="datetime" class="w-full" />
          </el-form-item>
          <el-form-item label="语言">
            <el-input v-model="screeningForm.language" />
          </el-form-item>
          <el-form-item label="版本">
            <el-input v-model="screeningForm.version" placeholder="IMAX 2D / 国语" />
          </el-form-item>
          <el-form-item label="最低价">
            <el-input v-model="screeningForm.lowestPrice" />
          </el-form-item>
          <el-form-item label="停售时间">
            <el-date-picker v-model="screeningForm.stopSellTime" value-format="YYYY-MM-DD HH:mm:ss" type="datetime" class="w-full" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="screeningDrawerVisible = false">取消</el-button>
          <el-button type="primary" :loading="screeningSaving" @click="saveScreening">保存场次</el-button>
        </template>
      </el-drawer>

      <el-drawer v-model="priceDrawerVisible" size="760px" title="票价库存">
        <div class="context-title">
          场次 {{ selectedScreening?.id }} · {{ selectedScreening?.cinemaName || selectedScreening?.cinemaId }}
        </div>
        <el-table v-loading="priceLoading" :data="priceRows" border class="dense-table mb-4">
          <el-table-column prop="ticketCategoryId" label="票档ID" width="130" />
          <el-table-column prop="priceName" label="票档" min-width="150" />
          <el-table-column prop="price" label="票价" width="90" />
          <el-table-column prop="totalNumber" label="总量" width="90" />
          <el-table-column prop="dbRemainNumber" label="DB余量" width="100" />
          <el-table-column prop="redisRemainNumber" label="Redis余量" width="110" />
          <el-table-column label="操作" width="170">
            <template #default="{ row }">
              <el-button link type="primary" @click="resetPriceForm(row)">编辑</el-button>
              <el-button link type="primary" @click="generateSeatByPrice(row)">生成座位</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-form label-width="96px" class="drawer-form split-form">
          <el-form-item label="票档ID" required>
            <el-input v-model="priceForm.ticketCategoryId" />
          </el-form-item>
          <el-form-item label="票档名称">
            <el-input v-model="priceForm.priceName" />
          </el-form-item>
          <el-form-item label="票价" required>
            <el-input v-model="priceForm.price" />
          </el-form-item>
          <el-form-item label="总量" required>
            <el-input v-model="priceForm.totalNumber" />
          </el-form-item>
          <el-form-item label="余量" required>
            <el-input v-model="priceForm.remainNumber" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="priceDrawerVisible = false">关闭</el-button>
          <el-button type="primary" :loading="priceSaving" @click="savePrice">保存票价库存</el-button>
        </template>
      </el-drawer>

      <el-drawer v-model="reconcileDrawerVisible" size="900px" title="库存对账">
        <div v-loading="reconcileLoading">
          <div class="reconcile-summary">
            <div>
              <span>一致性</span>
              <strong :class="reconcileResult?.consistent ? 'ok' : 'risk'">
                {{ reconcileResult?.consistent ? '一致' : '存在异常' }}
              </strong>
            </div>
            <div>
              <span>问题数</span>
              <strong>{{ reconcileResult?.issueCount ?? 0 }}</strong>
            </div>
            <div>
              <span>DB未售</span>
              <strong>{{ reconcileResult?.totalDbNoSoldCount ?? 0 }}</strong>
            </div>
            <div>
              <span>Redis未售</span>
              <strong>{{ reconcileResult?.totalRedisNoSoldCount ?? 0 }}</strong>
            </div>
          </div>
          <el-table :data="reconcileResult?.itemList || []" border class="dense-table mb-4">
            <el-table-column prop="ticketCategoryId" label="票档ID" width="120" />
            <el-table-column prop="priceName" label="票档" min-width="140" />
            <el-table-column prop="dbRemainNumber" label="DB余量" width="90" />
            <el-table-column prop="dbNoSoldCount" label="DB未售" width="90" />
            <el-table-column prop="redisNoSoldCount" label="Redis未售" width="100" />
            <el-table-column label="余量一致" width="100">
              <template #default="{ row }">{{ boolText(row.remainConsistent) }}</template>
            </el-table-column>
            <el-table-column label="座位一致" width="100">
              <template #default="{ row }">{{ boolText(row.seatStatusConsistent) }}</template>
            </el-table-column>
            <el-table-column prop="problemDesc" label="诊断" min-width="220" show-overflow-tooltip />
          </el-table>
        </div>
        <div class="context-title">对账工单</div>
        <el-table v-loading="issueLoading" :data="reconcileIssues" border class="dense-table">
          <el-table-column prop="id" label="工单ID" width="160" />
          <el-table-column prop="priceName" label="票档" min-width="140" />
          <el-table-column prop="issueType" label="类型" width="120" />
          <el-table-column prop="problemDesc" label="问题" min-width="240" show-overflow-tooltip />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">{{ row.issueStatusName || row.issueStatus }}</template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleIssue(row, 2)">处理</el-button>
              <el-button link type="info" @click="handleIssue(row, 3)">忽略</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-drawer>
    </div>
  </Page>
</template>

<style scoped>
.movie-workbench {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 100%;
  padding: 16px;
  color: #1f2937;
}

.workbench-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.workbench-header h1 {
  margin: 4px 0 0;
  font-size: 22px;
  font-weight: 650;
  letter-spacing: 0;
}

.eyebrow {
  font-size: 12px;
  color: #64748b;
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.metric-strip {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 1px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #e5e7eb;
}

.metric-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 78px;
  padding: 14px 16px;
  background: #fff;
}

.metric-item span {
  font-size: 13px;
  color: #64748b;
}

.metric-item strong {
  font-size: 26px;
  font-weight: 650;
  line-height: 1;
}

.metric-item em {
  margin-left: 4px;
  font-size: 13px;
  font-style: normal;
  color: #64748b;
}

.toolbar-row {
  display: grid;
  grid-template-columns: repeat(6, minmax(150px, 1fr)) repeat(3, auto);
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}

.focused-movie-panel {
  margin-bottom: 12px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.focused-panel-title,
.drawer-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.focused-panel-title span {
  font-size: 13px;
  color: #64748b;
}

.focused-movie-table {
  --el-table-border-color: #eef2f7;
}

.dense-table {
  width: 100%;
}

.movie-title-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.movie-title-cell img,
.poster-fallback,
.poster-empty {
  width: 42px;
  height: 56px;
  flex: 0 0 auto;
  border-radius: 6px;
  object-fit: cover;
}

.movie-title-cell--small img,
.movie-title-cell--small .poster-empty {
  width: 34px;
  height: 46px;
}

.poster-fallback {
  display: grid;
  place-items: center;
  background: #334155;
  color: #fff;
  font-size: 18px;
  font-weight: 650;
}

.poster-empty {
  display: grid;
  place-items: center;
  border: 1px solid #e5e7eb;
  background: #f8fafc;
  color: #94a3b8;
  font-size: 12px;
}

.movie-title-cell strong,
.movie-title-cell span,
.subline {
  display: block;
  min-width: 0;
}

.movie-title-cell strong {
  font-weight: 650;
}

.movie-title-cell span,
.subline {
  color: #64748b;
  font-size: 12px;
}

.pager-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.drawer-form {
  padding-right: 12px;
}

.edit-panel {
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
}

.id-picker-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.id-picker-toolbar {
  display: grid;
  grid-template-columns: minmax(160px, 1fr) minmax(220px, 1.4fr) auto auto;
  gap: 10px;
  align-items: center;
}

.split-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 12px;
}

.span-2 {
  grid-column: span 2;
}

.context-title {
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 650;
  color: #334155;
}

.reconcile-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1px;
  overflow: hidden;
  margin-bottom: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #e5e7eb;
}

.reconcile-summary div {
  padding: 12px;
  background: #fff;
}

.reconcile-summary span {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
  color: #64748b;
}

.reconcile-summary strong {
  font-size: 18px;
}

.ok {
  color: #15803d;
}

.risk {
  color: #b45309;
}

:deep(.el-tabs__header) {
  margin-bottom: 12px;
}

:deep(.el-table th.el-table__cell) {
  background: #f8fafc;
  color: #475569;
  font-weight: 650;
}

@media (max-width: 1200px) {
  .toolbar-row {
    grid-template-columns: repeat(3, minmax(150px, 1fr));
  }
}

@media (max-width: 760px) {
  .movie-workbench {
    padding: 10px;
  }

  .workbench-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .metric-strip,
  .reconcile-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .toolbar-row,
  .id-picker-toolbar,
  .split-form {
    grid-template-columns: 1fr;
  }

  .span-2 {
    grid-column: span 1;
  }
}
</style>
