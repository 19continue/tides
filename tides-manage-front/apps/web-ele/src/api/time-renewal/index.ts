import { requestClient } from '#/api/request';

export namespace TimeRenewalApi {
  export type RenewalType = 'movie' | 'program';

  export interface TimeRenewalResult {
    renewalCount: number;
    programIdList: Array<number | string>;
    screeningIdList: Array<number | string>;
  }
}

const RENEWAL_TIMEOUT = 300_000;

export async function programTimeRenewalApi() {
  return requestClient.post<TimeRenewalApi.TimeRenewalResult>(
    '/program/program/time/renewal/program/execute',
    undefined,
    { timeout: RENEWAL_TIMEOUT },
  );
}

export async function movieTimeRenewalApi() {
  return requestClient.post<TimeRenewalApi.TimeRenewalResult>(
    '/program/program/time/renewal/movie/execute',
    undefined,
    { timeout: RENEWAL_TIMEOUT },
  );
}
