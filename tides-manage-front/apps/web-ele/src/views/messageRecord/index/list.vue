<script setup lang="ts">
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { CustomizeDataApi } from '#/api/customize';

import { h, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { executeExceptionMessageApi, messageRecordPageApi } from '#/api/customize';
import { $t } from '#/locales';
import ProgramBusinessPicker from '#/views/_components/program-business-picker.vue';

import { useColumns } from './data';

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
          placeholder: '请选择节目或电影',
          onClick: () => openProgramPicker(),
        },
        renderComponentContent: () => ({
          suffix: () =>
            h(
              'span',
              { class: 'text-blue-600 cursor-pointer', onClick: () => openProgramPicker() },
              '选择',
            ),
        }),
        formItemClass: 'col-span-2',
      },
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
    ],
    showCollapseButton: false,
    submitOnChange: true,
  },
  gridOptions: {
    columns: useColumns(),
    height: '100%',
    keepSource: true,
    pagerConfig: {
      currentPage: 1,
      enabled: true,
      pageSize: 20,
      pageSizes: [10, 20, 50, 100],
    },
    proxyConfig: {
      autoLoad: false,
      ajax: {
        query: async ({ page }, formValues) => {
          const programId = (formValues as any)?.programId as string | undefined;
          if (!programId) {
            return { items: [], total: 0 } as any;
          }
          const res: any = await messageRecordPageApi({
            messageBusinessesId: programId,
            pageNumber: page.currentPage,
            pageSize: page.pageSize,
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
      keyField: 'messageTraceId',
    },
    toolbarConfig: {
      custom: true,
      export: false,
      refresh: { code: 'query' },
      search: true,
      zoom: true,
    },
  } as VxeTableGridOptions<CustomizeDataApi.MessageRecordVo>,
});

const programPickerRef = ref<InstanceType<typeof ProgramBusinessPicker> | null>(null);

function openProgramPicker() {
  programPickerRef.value?.open();
}

function setSelectedBusinessFromPicker(payload: any) {
  const form = (gridApi as any)?.formApi;
  form?.setFieldValue?.('programId', payload.programId);
  form?.setFieldValue?.('programTitle', payload.title);
  form?.setFieldValue?.('sourceType', payload.sourceType);
  form?.submit?.();
}

async function onHandleException(row: any) {
  try {
    await ElMessageBox.confirm(`确定要处理消息ID为 ${row.messageId} 的异常消息吗？`, '确认处理', {
      cancelButtonText: '取消',
      confirmButtonText: '确定',
      type: 'warning',
    });

    const result = await executeExceptionMessageApi({
      messageId: row.messageId,
    });

    if (result) {
      gridApi.reload();
      ElMessage.success('异常消息处理成功');
    } else {
      ElMessage.error('异常消息处理失败');
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('处理异常消息时发生错误:', error);
      ElMessage.error('处理异常消息时发生错误');
    }
  }
}
</script>

<template>
  <Page auto-content-height>
    <Grid :table-title="$t('tides.index.messageRecordListTitle')">
      <template #orderNumberCell="{ row }">
        <span class="text-gray-800">{{ row.messageProducerRecordId }}</span>
      </template>
      <template #reconciliationStatusCell="{ row }">
        <span
          :class="{
            'text-yellow-600': row.reconciliationStatus === '1',
            'text-red-600': row.reconciliationStatus === '-1',
            'text-green-600': row.reconciliationStatus === '2' || row.reconciliationStatus === '3',
            'text-gray-800': !['1', '-1', '2', '3'].includes(row.reconciliationStatus),
          }"
        >
          {{ row.reconciliationStatusName }}
        </span>
      </template>
      <template #operationCell="{ row }">
        <div v-if="row.reconciliationStatus === '-1'">
          <button
            class="cursor-pointer rounded border border-blue-300 px-2 py-1 text-blue-600 hover:bg-blue-50 hover:text-blue-800"
            @click="onHandleException(row)"
          >
            处理异常
          </button>
        </div>
        <div v-else>
          <span class="px-2 py-1 text-gray-400">无需处理</span>
        </div>
      </template>
    </Grid>

    <ProgramBusinessPicker
      ref="programPickerRef"
      movie-all-text="查询全部场次消息"
      title="选择节目/电影"
      @select-movie-all="setSelectedBusinessFromPicker"
      @select-movie-screening="setSelectedBusinessFromPicker"
      @select-program="setSelectedBusinessFromPicker"
    />
  </Page>
</template>
