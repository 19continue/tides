import request from '@/utils/request'

export function getSeatList(data) {
    return request({
        url: '/tides/program/seat/relate/info',
        method: 'post',
        data:data

    })
}

export function getMovieSeatList(data) {
    return request({
        url: '/tides/program/movie/screening/seat/info',
        method: 'post',
        data:data
    })
}
