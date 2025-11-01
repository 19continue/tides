import request from '@/utils/request'

/**
 * 推荐列表
 * @param params
 * */
export function getProgramRecommendList(params){
    return request({
        url: '/tides/program/program/recommend/list',
        method: 'post',
        data: params
    })
}