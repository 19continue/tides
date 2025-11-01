import request from '@/utils/request'

export function getProgramDetials(data) {
    return request({
        url: '/tides/program/program/detail',
        method: 'post',
        data:data

    })
}

export function getMovieScreeningList(data) {
    return request({
        url: '/tides/program/movie/screening/list',
        method: 'post',
        data:data
    })
}
