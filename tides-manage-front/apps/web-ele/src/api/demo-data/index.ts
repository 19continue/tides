import { requestClient } from '#/api/request';

export namespace DemoDataApi {
  export type ServiceName = 'customize' | 'order' | 'pay' | 'program' | 'user';

  export interface SnapshotStatus {
    serviceName: ServiceName;
    snapshotExists: boolean;
    snapshotTime?: string;
    tableCount: number;
    rowCount: number;
    tableRowCount: Record<string, number>;
  }
}

const serviceApiPrefix: Record<DemoDataApi.ServiceName, string> = {
  customize: '/customize/customize/demo-data',
  order: '/order/order/demo-data',
  pay: '/pay/pay/demo-data',
  program: '/program/program/demo-data',
  user: '/user/user/demo-data',
};

const SNAPSHOT_STATUS_TIMEOUT = 30_000;
const SNAPSHOT_OPERATE_TIMEOUT = 300_000;

export async function demoDataSnapshotStatusApi(serviceName: DemoDataApi.ServiceName) {
  return requestClient.post<DemoDataApi.SnapshotStatus>(
    `${serviceApiPrefix[serviceName]}/status`,
    undefined,
    { timeout: SNAPSHOT_STATUS_TIMEOUT },
  );
}

export async function demoDataRestoreSnapshotApi(serviceName: DemoDataApi.ServiceName) {
  return requestClient.post<DemoDataApi.SnapshotStatus>(
    `${serviceApiPrefix[serviceName]}/restore`,
    undefined,
    { timeout: SNAPSHOT_OPERATE_TIMEOUT },
  );
}
