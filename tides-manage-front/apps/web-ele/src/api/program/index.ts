import { requestClient } from '#/api/request';

export namespace ProgramApi {
  /** 节目种类数据返回值 */
  export interface ProgramCategoryQueryResult {
    /** 节目种类id */
    id: string;
    /** 父节目种类id */
    parentId: string;
    /** 节目种类名字 */
    name: string;
    /** 节目种类 */
    type: string;
  }

  /** 节目种类参数 */
  export interface ProgramCategoryParams {
    type: string;
  }

  /** 节目种类参数 */
  export interface ParentProgramCategoryIdParams {
    parentProgramCategoryId: string;
  }

  /** 节目列表结果 */
  export interface ProgramListResult {
    id: string;
    title: string;
    actor: string;
    place: string;
    itemPicture: string;
    areaName: string;
    programCategoryName: string;
    showTime: string;
    minPrice: string;
    maxPrice: string;
    permitChooseSeat?: number | string;
  }

  export interface ProgramDetailParams {
    id: number | string;
  }

  export interface ProgramDetailResult {
    chooseSeatExplain?: string;
    id: number | string;
    permitChooseSeat?: number | string;
    title?: string;
  }

  /** 节目列表参数 */
  export interface ProgramListParams {
    areaId: string;
    areaIds?: string[];
    content?: string;
    pageNumber: string;
    pageSize: string;
    parentProgramCategoryId: string;
    programCategoryId: string;
    timeType: string;
    type: string;
  }

  /**
   * 通用分页返回结构，与后端 PageVo 对应
   */
  export interface PageVo<T> {
    pageNum: number;
    pageSize: number;
    total: number;
    list: T[];
  }

  /**
   * mybatisPlus分页返回结构，与后端 PageVo 对应
   */
  export interface IPageVo<T> {
    current: number;
    size: number;
    total: number;
    records: T[];
  }

  export interface TicketCategoryListResult {
    id: string;
    programId: string;
    introduce: string;
    price: string;
    totalNumber: string;
    dbRemainNumber: string;
    redisRemainNumber: string;
  }

  export interface DbTicketCategoryListResult {
    id: string;
    programId: string;
    introduce: string;
  }

  export interface TicketCategoryListParams {
    programId: string;
  }

  /** 座位数据参数 */
  export interface SeatPageQueryParams {
    programId: string;
    ticketCategoryId: string;
    pageNumber: string;
    pageSize: string;
  }

  /** 座位数据返回值 */
  export interface SeatPageQueryResult {
    id: string;
    programId: string;
    ticketCategoryId: string;
    rowCode: string;
    colCode: string;
    price: string;
    dbSellStatusName: string;
    redisSellStatus: string;
  }
}

/**
 * 查询节目类型
 */
export async function selectByTypeQueryApi(data: ProgramApi.ProgramCategoryParams) {
  return requestClient.post<ProgramApi.ProgramCategoryQueryResult>('/program/program/category/selectByType', data);
}


/**
 * 通过父级节目类型查询节目类型
 */
export async function selectByParentProgramCategoryIdQueryApi(data: ProgramApi.ParentProgramCategoryIdParams) {
  return requestClient.post<ProgramApi.ProgramCategoryQueryResult>('/program/program/category/selectByParentProgramCategoryId', data);
}

/**
 * 通过节目类型查询节目分页列表
 */
export async function programPageQueryApi(data: ProgramApi.ProgramListParams) {
  return requestClient.post<ProgramApi.PageVo<ProgramApi.ProgramListResult>>('/program/program/page', data);
}

/**
 * 搜索节目分页列表
 */
export async function programSearchQueryApi(data: ProgramApi.ProgramListParams) {
  return requestClient.post<ProgramApi.PageVo<ProgramApi.ProgramListResult>>('/program/program/search', data);
}

/**
 * 查询节目详情
 */
export async function programDetailApi(data: ProgramApi.ProgramDetailParams) {
  return requestClient.post<ProgramApi.ProgramDetailResult>('/program/program/detail', data);
}

/**
 * 查询节目票档信息集合
 */
export async function ticketCategoryListQueryApi(data: ProgramApi.TicketCategoryListParams) {
  return requestClient.post<ProgramApi.PageVo<ProgramApi.TicketCategoryListResult>>('/program/program/manage/ticket/category/list', data);
}

/**
 * 查询数据库中节目票档信息集合
 */
export async function dbTicketCategoryListQueryApi(data: ProgramApi.TicketCategoryListParams) {
  return requestClient.post<ProgramApi.DbTicketCategoryListResult>('/program/program/manage/db/ticket/category/list', data);
}

/**
 * 座位分页列表
 */
export async function seatPageQueryApi(data: ProgramApi.SeatPageQueryParams) {
  return requestClient.post<ProgramApi.IPageVo<ProgramApi.SeatPageQueryResult>>('/program/program/manage/seat/page', data);
}
