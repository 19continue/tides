<script setup lang="ts">
import type { TimeRenewalApi } from '#/api/time-renewal';

import { computed, reactive } from 'vue';

import { Page } from '@vben/common-ui';

import {
  movieTimeRenewalApi,
  programTimeRenewalApi,
} from '#/api/time-renewal';

import {
  ElButton,
  ElCard,
  ElDescriptions,
  ElDescriptionsItem,
  ElMessage,
  ElMessageBox,
  ElTag,
} from 'element-plus';

interface RenewalPanel {
  key: TimeRenewalApi.RenewalType;
  title: string;
  desc: string;
  target: string;
  effect: string;
  loading: boolean;
  result?: TimeRenewalApi.TimeRenewalResult;
}

const panels = reactive<RenewalPanel[]>([
  {
    key: 'program',
    title: '演出时间续期',
    desc: '将临近过期或已过期的演出时间顺延到未来，清理节目缓存并重建演出搜索索引。',
    target: 'd_program_show_time.show_time <= 当前时间 + 2天',
    effect: '更新演出时间、演出日期、星期、Redis 缓存、本地缓存和演出 ES',
    loading: false,
  },
  {
    key: 'movie',
    title: '电影场次续期',
    desc: '将临近过期或已过期的电影放映场次顺延到未来，让前台电影列表重新出现可售场次。',
    target: 'd_movie_screening.show_time <= 当前时间 + 2天',
    effect: '更新放映时间、放映日期、星期、结束时间、停售时间和电影 ES',
    loading: false,
  },
]);

const operating = computed(() => panels.some((item) => item.loading));

function resultIds(result: TimeRenewalApi.TimeRenewalResult | undefined, key: TimeRenewalApi.RenewalType) {
  if (!result) {
    return '-';
  }
  const list = key === 'movie' ? result.screeningIdList : result.programIdList;
  if (!list?.length) {
    return '-';
  }
  return list.slice(0, 30).join(', ') + (list.length > 30 ? ` 等 ${list.length} 个` : '');
}

function resultText(result?: TimeRenewalApi.TimeRenewalResult) {
  if (!result) {
    return '未执行';
  }
  return result.renewalCount > 0 ? `已续期 ${result.renewalCount} 条` : '无需续期';
}

function resultTagType(result?: TimeRenewalApi.TimeRenewalResult) {
  if (!result) {
    return 'info';
  }
  return result.renewalCount > 0 ? 'success' : 'info';
}

async function executeRenewal(panel: RenewalPanel) {
  await ElMessageBox.confirm(
    `${panel.title}会直接更新数据库时间，并触发搜索索引重建。确认执行？`,
    panel.title,
    {
      cancelButtonText: '取消',
      confirmButtonText: '确认执行',
      type: 'warning',
    },
  );

  panel.loading = true;
  try {
    panel.result =
      panel.key === 'movie'
        ? await movieTimeRenewalApi()
        : await programTimeRenewalApi();
    if (panel.result.renewalCount > 0) {
      ElMessage.success(`${panel.title}完成，已续期 ${panel.result.renewalCount} 条`);
    } else {
      ElMessage.info(`${panel.title}完成，没有需要续期的数据`);
    }
  } catch (error: any) {
    ElMessage.error(error?.message || `${panel.title}失败`);
  } finally {
    panel.loading = false;
  }
}
</script>

<template>
  <Page>
    <div class="renewal-page">
      <section class="renewal-header">
        <div>
          <h1>时间续期</h1>
          <p>手动修复演示数据里的历史演出和电影场次时间，执行后前台列表会重新按未来时间展示。</p>
        </div>
      </section>

      <div class="renewal-grid">
        <ElCard
          v-for="panel in panels"
          :key="panel.key"
          class="renewal-card"
          shadow="never"
        >
          <template #header>
            <div class="renewal-card-title">
              <div>
                <h2>{{ panel.title }}</h2>
                <span>{{ panel.desc }}</span>
              </div>
              <ElTag :type="resultTagType(panel.result)">
                {{ resultText(panel.result) }}
              </ElTag>
            </div>
          </template>

          <ElDescriptions :column="1" border>
            <ElDescriptionsItem label="续期范围">
              {{ panel.target }}
            </ElDescriptionsItem>
            <ElDescriptionsItem label="影响内容">
              {{ panel.effect }}
            </ElDescriptionsItem>
            <ElDescriptionsItem label="最近结果">
              {{ resultText(panel.result) }}
            </ElDescriptionsItem>
            <ElDescriptionsItem :label="panel.key === 'movie' ? '场次ID' : '节目ID'">
              <span class="id-list">{{ resultIds(panel.result, panel.key) }}</span>
            </ElDescriptionsItem>
          </ElDescriptions>

          <div class="renewal-actions">
            <ElButton
              :disabled="operating && !panel.loading"
              :loading="panel.loading"
              type="primary"
              @click="executeRenewal(panel)"
            >
              执行续期
            </ElButton>
          </div>
        </ElCard>
      </div>
    </div>
  </Page>
</template>

<style scoped>
.renewal-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.renewal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 22px;
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: 8px;
}

.renewal-header h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  line-height: 28px;
}

.renewal-header p {
  max-width: 72ch;
  margin: 6px 0 0;
  color: hsl(var(--muted-foreground));
  font-size: 13px;
  line-height: 20px;
}

.renewal-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.renewal-card {
  border-radius: 8px;
}

.renewal-card-title {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  justify-content: space-between;
}

.renewal-card-title h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;
}

.renewal-card-title span {
  display: block;
  margin-top: 4px;
  color: hsl(var(--muted-foreground));
  font-size: 12px;
  line-height: 18px;
}

.id-list {
  word-break: break-all;
}

.renewal-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

@media (max-width: 1280px) {
  .renewal-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .renewal-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 14px;
  }

  .renewal-actions {
    justify-content: flex-start;
  }
}
</style>
