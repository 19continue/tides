import request from '@/utils/request'

export function getMovieDetail(data) {
    return request({
        url: '/tides/program/movie/detail',
        method: 'post',
        data
    })
}

export function getMovieProgramPage(data) {
    return request({
        url: '/tides/program/program/page',
        method: 'post',
        data
    })
}

export function getMoviePage(data) {
    return request({
        url: '/tides/program/movie/page',
        method: 'post',
        data
    })
}

export function getMovieScreeningList(data) {
    return request({
        url: '/tides/program/movie/screening/list',
        method: 'post',
        data
    })
}
