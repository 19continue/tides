<script setup lang="ts">
import type { DemoDataApi } from '#/api/demo-data';

import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  demoDataRestoreSnapshotApi,
  demoDataSnapshotStatusApi,
} from '#/api/demo-data';

import {
  ElButton,
  ElCard,
  ElDescriptions,
  ElDescriptionsItem,
  ElMessage,
  ElMessageBox,
  ElSkeleton,
  ElTable,
  ElTableColumn,
  ElTag,
} from 'element-plus';

interface ServiceItem {
  key: DemoDataApi.ServiceName;
  name: string;
  desc: string;
}

interface ServiceStatus extends ServiceItem {
  loading: boolean;
  error?: string;
  status?: DemoDataApi.SnapshotStatus;
}

const serviceList: ServiceItem[] = [
  {
    key: 'program',
    name: '节目与电影库存',
    desc: '节目票档、座位、电影场次库存、电影座位和库存流水',
  },
  {
    key: 'order',
    name: '订单数据',
    desc: '订单、购票人订单、订单操作记录和订单节目路由记录',
  },
  {
    key: 'pay',
    name: '支付账单',
    desc: '支付账单和退款账单，恢复后订单详情不会残留压测支付记录',
  },
  {
    key: 'customize',
    name: 'MQ消息记录',
    desc: '消息发送记录和消息消费记录',
  },
  {
    key: 'user',
    name: '用户与购票人',
    desc: '压测生成的用户、手机号、邮箱和购票人会恢复到基准状态',
  },
];

const services = ref<ServiceStatus[]>(
  serviceList.map((item) => ({ ...item, loading: false })),
);
const operating = ref(false);

const baselineReady = computed(() =>
  services.value.every((item) => item.status?.snapshotExists),
);

function updateService(serviceName: DemoDataApi.ServiceName, patch: Partial<ServiceStatus>) {
  const target = services.value.find((item) => item.key === serviceName);
  if (target) {
    Object.assign(target, patch);
  }
}

function formatTime(value?: string) {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').slice(0, 19);
}

function tableRows(status?: DemoDataApi.SnapshotStatus) {
  return Object.entries(status?.tableRowCount ?? {}).map(([tableName, rowCount]) => ({
    rowCount,
    tableName,
  }));
}

async function refreshStatus() {
  await Promise.all(
    services.value.map(async (item) => {
      updateService(item.key, { error: undefined, loading: true });
      try {
        const result = await demoDataSnapshotStatusApi(item.key);
        updateService(item.key, { status: result as any });
      } catch (error: any) {
        updateService(item.key, {
          error: error?.message || '服务暂不可用',
          status: undefined,
        });
      } finally {
        updateService(item.key, { loading: false });
      }
    }),
  );
}

async function restoreAll() {
  await ElMessageBox.confirm(
    '恢复会覆盖订单、支付、库存、座位和消息记录，请确认压测已经结束。',
    '恢复演示基准',
    {
      cancelButtonText: '取消',
      confirmButtonText: '确认恢复',
      type: 'warning',
    },
  );

  operating.value = true;
  try {
    const executionList: DemoDataApi.ServiceName[] = ['order', 'pay', 'customize', 'program', 'user'];
    for (const serviceName of executionList) {
      const item = services.value.find((service) => service.key === serviceName);
      if (!item) {
        continue;
      }
      updateService(item.key, { error: undefined, loading: true });
      const result = await demoDataRestoreSnapshotApi(item.key);
      updateService(item.key, { status: result as any });
      updateService(item.key, { loading: false });
    }
    ElMessage.success('已恢复到演示基准');
  } catch (error: any) {
    ElMessage.error(error?.message || '操作失败');
  } finally {
    operating.value = false;
    await refreshStatus();
  }
}

onMounted(() => {
  refreshStatus();
});
</script>

<template>
  <Page>
    <div class="snapshot-page">
      <section class="snapshot-header">
        <div>
          <h1>演示数据恢复</h1>
          <p>压测后将订单、支付、库存、座位和消息记录恢复到当前固化的演示基准。</p>
        </div>
        <div class="snapshot-actions">
          <ElButton :loading="operating" @click="refreshStatus">刷新</ElButton>
          <ElButton
            :disabled="!baselineReady"
            :loading="operating"
            type="danger"
            @click="restoreAll"
          >
            恢复演示基准
          </ElButton>
        </div>
      </section>

      <div class="service-grid">
        <ElCard
          v-for="item in services"
          :key="item.key"
          class="service-card"
          shadow="never"
        >
          <template #header>
            <div class="service-card-title">
              <div>
                <h2>{{ item.name }}</h2>
                <span>{{ item.desc }}</span>
              </div>
              <ElTag v-if="item.error" type="danger">异常</ElTag>
              <ElTag v-else-if="item.status?.snapshotExists" type="success">基准可用</ElTag>
              <ElTag v-else type="info">缺少基准</ElTag>
            </div>
          </template>

          <ElSkeleton v-if="item.loading" :rows="4" animated />
          <div v-else>
            <p v-if="item.error" class="service-error">{{ item.error }}</p>
            <ElDescriptions v-else :column="2" border>
              <ElDescriptionsItem label="基准时间">
                {{ formatTime(item.status?.snapshotTime) }}
              </ElDescriptionsItem>
              <ElDescriptionsItem label="总行数">
                {{ item.status?.rowCount ?? 0 }}
              </ElDescriptionsItem>
              <ElDescriptionsItem label="覆盖表数">
                {{ item.status?.tableCount ?? 0 }}
              </ElDescriptionsItem>
              <ElDescriptionsItem label="服务">
                {{ item.key }}
              </ElDescriptionsItem>
            </ElDescriptions>

            <ElTable
              :data="tableRows(item.status)"
              class="snapshot-table"
              size="small"
            >
              <ElTableColumn label="表名" min-width="220" prop="tableName" />
              <ElTableColumn align="right" label="基准行数" prop="rowCount" width="110" />
            </ElTable>
          </div>
        </ElCard>
      </div>
    </div>
  </Page>
</template>

<style scoped>
.snapshot-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.snapshot-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 22px;
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: 8px;
}

.snapshot-header h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  line-height: 28px;
}

.snapshot-header p {
  margin: 6px 0 0;
  color: hsl(var(--muted-foreground));
  font-size: 13px;
}

.snapshot-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.service-card {
  border-radius: 8px;
}

.service-card-title {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  justify-content: space-between;
}

.service-card-title h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;
}

.service-card-title span {
  display: block;
  margin-top: 4px;
  color: hsl(var(--muted-foreground));
  font-size: 12px;
  line-height: 18px;
}

.service-error {
  min-height: 96px;
  padding: 12px;
  margin: 0;
  color: #b42318;
  background: #fff4f2;
  border-radius: 6px;
}

.snapshot-table {
  margin-top: 12px;
}

@media (max-width: 1280px) {
  .service-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .snapshot-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 14px;
  }

  .snapshot-actions {
    justify-content: flex-start;
  }
}
</style>
