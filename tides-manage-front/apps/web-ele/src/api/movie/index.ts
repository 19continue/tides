import { requestClient } from '#/api/request';

export namespace MovieApi {
  export interface PageParams {
    pageNumber: number | string;
    pageSize: number | string;
  }

  export interface IPageVo<T> {
    current: number;
    pages?: number;
    records: T[];
    size: number;
    total: number;
  }

  export interface MovieManageParams extends PageParams {
    areaId?: number | string;
    areaIds?: Array<number | string>;
    movieId?: number | string;
    movieName?: string;
    programId?: number | string;
    status?: number | string;
  }

  export interface MovieManageResult {
    actors?: string;
    createTime?: string;
    description?: string;
    director?: string;
    durationMinutes?: number;
    editTime?: string;
    id: number | string;
    language?: string;
    movieAlias?: string;
    movieName: string;
    poster?: string;
    programId: number | string;
    paidOrderCount?: number | string;
    paidTicketCount?: number | string;
    region?: string;
    releaseDate?: string;
    screeningCount?: number | string;
    soldOutScreeningCount?: number | string;
    status?: number;
  }

  export interface CinemaManageParams extends PageParams {
    areaId?: number | string;
    brandName?: string;
    businessArea?: string;
    cinemaId?: number | string;
    cinemaName?: string;
    cityName?: string;
    districtName?: string;
    status?: number | string;
  }

  export interface CinemaManageResult {
    address?: string;
    areaId?: number | string;
    brandName?: string;
    businessArea?: string;
    cinemaName?: string;
    cityName?: string;
    coverUrl?: string;
    createTime?: string;
    districtName?: string;
    editTime?: string;
    featureTags?: string;
    id: number | string;
    latitude?: number | string;
    longitude?: number | string;
    openingHours?: string;
    parkingInfo?: string;
    phone?: string;
    status?: number;
    trafficInfo?: string;
  }

  export interface MovieSaveParams {
    actors?: string;
    description?: string;
    director?: string;
    durationMinutes?: number | string;
    id?: number | string;
    language?: string;
    movieAlias?: string;
    movieName: string;
    poster?: string;
    programId: number | string;
    region?: string;
    releaseDate?: string;
  }

  export interface MovieStatusUpdateParams {
    movieId: number | string;
    programId: number | string;
    status: number;
  }

  export interface MovieProfileManageParams extends PageParams {
    movieId?: number | string;
    profileId?: number | string;
    programId?: number | string;
    releaseStatus?: number | string;
    status?: number | string;
  }

  export interface MovieProfileManageResult {
    ageTips?: string;
    boxOfficeAmount?: number | string;
    createTime?: string;
    distributor?: string;
    editTime?: string;
    genre?: string;
    id: number | string;
    longDescription?: string;
    movieId: number | string;
    producer?: string;
    programId: number | string;
    ratingScore?: number | string;
    releaseStatus?: number;
    status?: number;
    wantWatchCount?: number | string;
    watchedCount?: number | string;
  }

  export interface MovieProfileSaveParams {
    ageTips?: string;
    boxOfficeAmount?: number | string;
    distributor?: string;
    genre?: string;
    id?: number | string;
    longDescription?: string;
    movieId: number | string;
    producer?: string;
    programId: number | string;
    ratingScore?: number | string;
    releaseStatus?: number | string;
    wantWatchCount?: number | string;
    watchedCount?: number | string;
  }

  export interface MovieProfileStatusUpdateParams {
    profileId: number | string;
    programId: number | string;
    status: number;
  }

  export interface MovieMediaManageParams extends PageParams {
    auditStatus?: number | string;
    mediaId?: number | string;
    mediaType?: number | string;
    movieId?: number | string;
    status?: number | string;
  }

  export interface MovieMediaManageResult {
    auditStatus?: number;
    coverUrl?: string;
    createTime?: string;
    editTime?: string;
    id: number | string;
    mediaType?: number;
    mediaUrl?: string;
    movieId: number | string;
    sortOrder?: number;
    status?: number;
    title?: string;
  }

  export interface MovieMediaSaveParams {
    auditStatus?: number | string;
    coverUrl?: string;
    id?: number | string;
    mediaType: number | string;
    mediaUrl: string;
    movieId: number | string;
    sortOrder?: number | string;
    title?: string;
  }

  export interface MovieMediaStatusUpdateParams {
    mediaId: number | string;
    movieId: number | string;
    status: number;
  }

  export interface MovieScreeningManageParams extends PageParams {
    areaId?: number | string;
    areaIds?: Array<number | string>;
    cinemaId?: number | string;
    movieId?: number | string;
    programId?: number | string;
    screeningId?: number | string;
    screeningStatus?: number | string;
  }

  export interface MovieScreeningManageResult {
    areaId?: number | string;
    cinemaAddress?: string;
    cinemaId: number | string;
    cinemaName?: string;
    cityName?: string;
    dbRemainNumber?: number | string;
    districtName?: string;
    endTime?: string;
    hallId: number | string;
    hallName?: string;
    hallType?: string;
    id: number | string;
    language?: string;
    lowestPrice?: number | string;
    lockedNumber?: number | string;
    movieId: number | string;
    movieName?: string;
    occupiedNumber?: number | string;
    paidOrderCount?: number | string;
    paidTicketCount?: number | string;
    poster?: string;
    programId: number | string;
    screeningStatus?: number;
    showDayTime?: string;
    showTime?: string;
    showWeekTime?: string;
    soldSeatNumber?: number | string;
    soldNumber?: number | string;
    soldOut?: boolean;
    stopSellTime?: string;
    totalNumber?: number | string;
    version?: string;
  }

  export interface MovieScreeningSaveParams {
    cinemaId: number | string;
    endTime?: string;
    hallId: number | string;
    id?: number | string;
    language?: string;
    lowestPrice?: number | string;
    movieId: number | string;
    programId: number | string;
    screeningStatus?: number | string;
    showDayTime?: string;
    showTime: string;
    showWeekTime?: string;
    stopSellTime?: string;
    version?: string;
  }

  export interface MovieScreeningStatusUpdateParams {
    screeningId: number | string;
    screeningStatus: number;
  }

  export interface MovieScreeningPriceManageParams {
    screeningId: number | string;
  }

  export interface MovieScreeningPriceManageResult {
    dbRemainNumber?: number | string;
    id: number | string;
    price?: number | string;
    priceName?: string;
    redisRemainNumber?: number | string;
    screeningId: number | string;
    ticketCategoryId: number | string;
    totalNumber?: number | string;
  }

  export interface MovieScreeningPriceSaveParams {
    id?: number | string;
    price: number | string;
    priceName?: string;
    remainNumber: number | string;
    screeningId: number | string;
    ticketCategoryId: number | string;
    totalNumber: number | string;
  }

  export interface MovieScreeningPriceStatusUpdateParams {
    priceId: number | string;
    screeningId: number | string;
    status: number;
  }

  export interface MovieScreeningSeatGenerateParams {
    price: number | string;
    priceName?: string;
    screeningId: number | string;
    ticketCategoryId: number | string;
  }

  export interface MovieScreeningSeatManageParams extends PageParams {
    screeningId: number | string;
    ticketCategoryId?: number | string;
  }

  export interface MovieScreeningSeatManageResult {
    aisleFlag?: number;
    accessibleFlag?: number;
    colCode?: number | string;
    coupleFlag?: number;
    dbSellStatus?: number;
    dbSellStatusName?: string;
    id: number | string;
    price?: number | string;
    priceLevel?: string;
    programId?: number | string;
    redisSellStatus?: number;
    redisSellStatusName?: string;
    repairFlag?: number;
    rowCode?: number | string;
    screeningId?: number | string;
    seatNo?: string;
    seatType?: number;
    seatTypeName?: string;
    sellableFlag?: number;
    ticketCategoryId?: number | string;
    vipFlag?: number;
    zoneName?: string;
  }

  export interface MovieInventoryReconcileResult {
    cinemaId?: number | string;
    consistent?: boolean;
    hallId?: number | string;
    issueCount?: number;
    itemList?: MovieInventoryReconcileItem[];
    movieId?: number | string;
    programId?: number | string;
    screeningId?: number | string;
    totalDbLockCount?: number | string;
    totalDbNoSoldCount?: number | string;
    totalDbRemainNumber?: number | string;
    totalDbSoldCount?: number | string;
    totalRedisLockCount?: number | string;
    totalRedisNoSoldCount?: number | string;
    totalRedisSoldCount?: number | string;
  }

  export interface MovieInventoryReconcileItem {
    consistent?: boolean;
    dbLockCount?: number | string;
    dbNoSoldCount?: number | string;
    dbRemainNumber?: number | string;
    dbSoldCount?: number | string;
    priceName?: string;
    problemDesc?: string;
    redisCacheReady?: boolean;
    redisLockCount?: number | string;
    redisNoSoldCount?: number | string;
    redisSoldCount?: number | string;
    remainConsistent?: boolean;
    seatStatusConsistent?: boolean;
    ticketCategoryId?: number | string;
  }

  export interface MovieInventoryReconcileIssueParams extends PageParams {
    issueStatus?: number | string;
    movieId?: number | string;
    programId?: number | string;
    screeningId?: number | string;
  }

  export interface MovieInventoryReconcileIssueResult {
    createTime?: string;
    dbLockCount?: number | string;
    dbNoSoldCount?: number | string;
    dbRemainNumber?: number | string;
    dbSoldCount?: number | string;
    handleRemark?: string;
    handleTime?: string;
    handlerId?: number | string;
    id: number | string;
    issueStatus?: number;
    issueStatusName?: string;
    issueType?: string;
    lastReconcileTime?: string;
    movieId?: number | string;
    priceName?: string;
    problemDesc?: string;
    programId?: number | string;
    redisCacheReady?: number;
    redisLockCount?: number | string;
    redisNoSoldCount?: number | string;
    redisSoldCount?: number | string;
    remainConsistent?: number;
    screeningId?: number | string;
    seatStatusConsistent?: number;
    ticketCategoryId?: number | string;
  }

  export interface MovieInventoryReconcileIssueHandleParams {
    handleRemark?: string;
    handlerId?: number | string;
    id: number | string;
    issueStatus: number;
    screeningId: number | string;
  }
}

export async function moviePageApi(data: MovieApi.MovieManageParams) {
  return requestClient.post<MovieApi.IPageVo<MovieApi.MovieManageResult>>(
    '/program/program/manage/movie/page',
    data,
  );
}

export async function movieSaveApi(data: MovieApi.MovieSaveParams) {
  return requestClient.post<number | string>('/program/program/manage/movie/save', data);
}

export async function movieStatusUpdateApi(data: MovieApi.MovieStatusUpdateParams) {
  return requestClient.post<boolean>('/program/program/manage/movie/status/update', data);
}

export async function cinemaPageApi(data: MovieApi.CinemaManageParams) {
  return requestClient.post<MovieApi.IPageVo<MovieApi.CinemaManageResult>>(
    '/program/program/manage/cinema/page',
    data,
  );
}

export async function movieProfilePageApi(data: MovieApi.MovieProfileManageParams) {
  return requestClient.post<MovieApi.IPageVo<MovieApi.MovieProfileManageResult>>(
    '/program/program/manage/movie/profile/page',
    data,
  );
}

export async function movieProfileSaveApi(data: MovieApi.MovieProfileSaveParams) {
  return requestClient.post<number | string>('/program/program/manage/movie/profile/save', data);
}

export async function movieProfileStatusUpdateApi(data: MovieApi.MovieProfileStatusUpdateParams) {
  return requestClient.post<boolean>('/program/program/manage/movie/profile/status/update', data);
}

export async function movieMediaPageApi(data: MovieApi.MovieMediaManageParams) {
  return requestClient.post<MovieApi.IPageVo<MovieApi.MovieMediaManageResult>>(
    '/program/program/manage/movie/media/page',
    data,
  );
}

export async function movieMediaSaveApi(data: MovieApi.MovieMediaSaveParams) {
  return requestClient.post<number | string>('/program/program/manage/movie/media/save', data);
}

export async function movieMediaStatusUpdateApi(data: MovieApi.MovieMediaStatusUpdateParams) {
  return requestClient.post<boolean>('/program/program/manage/movie/media/status/update', data);
}

export async function movieScreeningPageApi(data: MovieApi.MovieScreeningManageParams) {
  return requestClient.post<MovieApi.IPageVo<MovieApi.MovieScreeningManageResult>>(
    '/program/program/manage/movie/screening/page',
    data,
  );
}

export async function movieScreeningSaveApi(data: MovieApi.MovieScreeningSaveParams) {
  return requestClient.post<number | string>('/program/program/manage/movie/screening/save', data);
}

export async function movieScreeningStatusUpdateApi(data: MovieApi.MovieScreeningStatusUpdateParams) {
  return requestClient.post<boolean>('/program/program/manage/movie/screening/status/update', data);
}

export async function movieScreeningPriceListApi(data: MovieApi.MovieScreeningPriceManageParams) {
  return requestClient.post<MovieApi.MovieScreeningPriceManageResult[]>(
    '/program/program/manage/movie/screening/price/list',
    data,
  );
}

export async function movieScreeningPriceSaveApi(data: MovieApi.MovieScreeningPriceSaveParams) {
  return requestClient.post<number | string>('/program/program/manage/movie/screening/price/save', data);
}

export async function movieScreeningPriceStatusUpdateApi(
  data: MovieApi.MovieScreeningPriceStatusUpdateParams,
) {
  return requestClient.post<boolean>('/program/program/manage/movie/screening/price/status/update', data);
}

export async function movieScreeningSeatGenerateApi(data: MovieApi.MovieScreeningSeatGenerateParams) {
  return requestClient.post<number>('/program/program/manage/movie/screening/seat/generate', data);
}

export async function movieScreeningSeatPageApi(data: MovieApi.MovieScreeningSeatManageParams) {
  return requestClient.post<MovieApi.IPageVo<MovieApi.MovieScreeningSeatManageResult>>(
    '/program/program/manage/movie/screening/seat/page',
    data,
  );
}

export async function movieInventoryReconcileApi(data: { screeningId: number | string }) {
  return requestClient.post<MovieApi.MovieInventoryReconcileResult>(
    '/program/program/manage/movie/inventory/reconcile',
    data,
  );
}

export async function movieInventoryReconcileIssuePageApi(
  data: MovieApi.MovieInventoryReconcileIssueParams,
) {
  return requestClient.post<MovieApi.IPageVo<MovieApi.MovieInventoryReconcileIssueResult>>(
    '/program/program/manage/movie/inventory/reconcile/issue/page',
    data,
  );
}

export async function movieInventoryReconcileIssueHandleApi(
  data: MovieApi.MovieInventoryReconcileIssueHandleParams,
) {
  return requestClient.post<boolean>('/program/program/manage/movie/inventory/reconcile/issue/handle', data);
}
