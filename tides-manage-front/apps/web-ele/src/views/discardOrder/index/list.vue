<script setup lang="ts">
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { OrderMangeApi } from '#/api/order';

import { h, ref } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { discardOrderPageQueryApi } from '#/api/order';
import { $t } from '#/locales';
import ProgramBusinessPicker from '#/views/_components/program-business-picker.vue';

import { useColumns, useSchema } from './data';

const [Drawer, drawerApi] = useVbenDrawer();
const [Form] = useVbenForm({
  commonConfig: {
    // 所有表单项
    componentProps: {
      class: 'w-full',
    },
  },
  resetButtonOptions: {
    content: '关闭',
  },
  layout: 'horizontal',
  handleSubmit: (values) => {
    onSubmit(values);
  },
  handleReset: () => {
    drawerApi.close();
  },
  schema: useSchema(),
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
        formItemClass: 'col-span-2',
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
    ],
    showCollapseButton: false,
    submitOnChange: true,
  },
  gridOptions: {
    columns: useColumns(onActionClick),
    height: 'auto',
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
          const screeningId = (formValues as any)?.screeningId as string | undefined;
          if (!programId) {
            return { items: [], total: 0 } as any;
          }
          const query: any = {
            programId,
            pageNumber: String(page.currentPage),
            pageSize: String(page.pageSize),
          };
          if (screeningId) {
            query.screeningId = screeningId;
          }
          const res: any = await discardOrderPageQueryApi(query);
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
      keyField: 'orderNumber',
    },
    toolbarConfig: {
      custom: true,
      export: false,
      refresh: { code: 'query' },
      search: true,
      zoom: true,
    },
  } as VxeTableGridOptions<OrderMangeApi.DiscardOrderPageQueryResult>,
});

const programPickerRef = ref<InstanceType<typeof ProgramBusinessPicker> | null>(null);

function openProgramPicker() {
  programPickerRef.value?.open();
}

function setBusinessFilter(payload: any) {
  const form = (gridApi as any)?.formApi;
  form?.setFieldValue?.('sourceType', payload.sourceType);
  form?.setFieldValue?.('screeningId', payload.screeningId || '');
  form?.setFieldValue?.('programId', payload.programId);
  form?.setFieldValue?.('programTitle', payload.title);
  form?.submit?.();
}

function onActionClick(_params: any) {
  // 处理操作点击
}

function onSubmit(_values: any) {
  // 处理表单提交
}

function toggleOrderExpand(row: any) {
  const grid = (gridApi as any)?.grid;
  if (!grid) return;
  if (typeof grid.toggleRowExpand === 'function') {
    grid.toggleRowExpand(row);
  } else if (typeof grid.setRowExpand === 'function') {
    const expanded = typeof grid.isRowExpand === 'function' ? !!grid.isRowExpand(row) : false;
    grid.setRowExpand(row, !expanded);
  }
}

function isRowExpanded(row: any) {
  const grid = (gridApi as any)?.grid;
  if (!grid) return false;
  if (typeof grid.isRowExpand === 'function') {
    return !!grid.isRowExpand(row);
  }
  return false;
}
</script>
<template>
  <Page auto-content-height>
    <Drawer class="w-[600px]" title="编辑">
      <Form />
      <template #footer>
        <div></div>
      </template>
    </Drawer>
    <Grid :table-title="$t('tides.index.discardOrderListTitle')">
    <template #orderNumberCell="{ row }">
      <div
        class="cursor-pointer opacity-85 hover:opacity-100 hover:bg-blue-50 hover:ring-1 hover:ring-blue-300 p-1 rounded flex items-center gap-2"
        title="点击展开/收起"
        @click="toggleOrderExpand(row)"
      >
        <span class="text-blue-600 text-xs bg-blue-50 border border-blue-200 rounded px-1">
          {{ isRowExpanded(row) ? '收起' : '展开' }}
        </span>
        <span class="font-mono">{{ row.orderNumber }}</span>
      </div>
    </template>
    <template #actions="{ row }">
      <button
        class="text-blue-600 hover:underline"
        title="展开/收起"
        @click="toggleOrderExpand(row)"
      >{{ isRowExpanded(row) ? '收起明细' : '展开明细' }}</button>
    </template>
    <template #ticketExpand="{ row }">
      <div class="p-2 bg-gray-50 rounded">
        <table class="table-fixed w-full text-left text-sm">
          <thead>
            <tr>
              <th class="py-1 px-2 w-[20%]">购票人id</th>
              <th class="py-1 px-2 w-[15%]">座位信息</th>
              <th class="py-1 px-2 w-[10%]">价格</th>
              <th class="py-1 px-2 w-[12%]">生成时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(d, idx) in (row.discardOrderTicketUserManageVo || [])" :key="idx">
              <td class="py-1 px-2 truncate">{{ d.ticketUserId }}</td>
              <td class="py-1 px-2 truncate">{{ d.seatInfo }}</td>
              <td class="py-1 px-2">{{ d.orderPrice }}</td>
              <td class="py-1 px-2">{{ d.createOrderTime }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
    </Grid>

    <ProgramBusinessPicker
      ref="programPickerRef"
      movie-all-text="查询全部场次废弃订单"
      title="选择节目"
      @select-movie-all="setBusinessFilter"
      @select-movie-screening="setBusinessFilter"
      @select-program="setBusinessFilter"
    />
  </Page>
</template>
