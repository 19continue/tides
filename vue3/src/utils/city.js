const CITY_STORAGE_KEY = 'chaosheng_selected_city_v2'

export const ALL_CITY_FILTER = {id: '', name: '全部城市', scope: 'page'}

const NATIONAL_AREA_ID = 1
const JIAXING_AREA_ID = 385
const NANHU_AREA_ID = 3248

const DISPLAY_CITY_ID_MAP = {
  [NANHU_AREA_ID]: JIAXING_AREA_ID
}

const CITY_CHILD_AREA_ID_MAP = {
  [JIAXING_AREA_ID]: [NANHU_AREA_ID]
}

const KNOWN_CITY_LIST = [
  {id: NATIONAL_AREA_ID, name: '全国', cityName: '全国', type: 0},
  {id: JIAXING_AREA_ID, name: '嘉兴', cityName: '嘉兴', type: 2},
  {id: NANHU_AREA_ID, name: '南湖区', cityName: '嘉兴', parentId: JIAXING_AREA_ID, type: 3},
  {id: 2, name: '北京', cityName: '北京', type: 2},
]

const SUPPORTED_BROWSER_LOCATIONS = [
  {id: JIAXING_AREA_ID, name: '嘉兴', cityName: '嘉兴', latitude: 30.747, longitude: 120.783, radiusKm: 35},
  {id: 2, name: '北京', cityName: '北京', latitude: 39.904, longitude: 116.407, radiusKm: 55}
]

let browserLocatedCityPromise = null

export function getKnownCity(id) {
  if (id === undefined || id === null) {
    return null
  }
  const knownCity = KNOWN_CITY_LIST.find(item => Number(item.id) === Number(id))
  return knownCity ? {...knownCity} : null
}

export function normalizeCity(city) {
  if (!city) {
    return null
  }
  const rawId = city.id ?? city.areaId
  if (rawId === '') {
    return null
  }
  const normalizedId = normalizeId(rawId)
  const id = DISPLAY_CITY_ID_MAP[normalizedId] || normalizedId
  const knownCity = getKnownCity(id)
  const name = city.name ?? city.areaIdName ?? knownCity?.name
  if (id === undefined || name === undefined) {
    return null
  }
  const type = normalizeId(city.type ?? knownCity?.type)
  const parentId = normalizeId(city.parentId ?? knownCity?.parentId)
  const parentCity = getKnownCity(parentId)
  const cityName = city.cityName ?? (type === 2 ? name : knownCity?.cityName ?? parentCity?.name)
  return {
    id,
    name: knownCity?.name || name,
    cityName: knownCity?.cityName || cityName,
    parentId: knownCity?.parentId ?? parentId,
    type: knownCity?.type ?? type,
    latitude: city.latitude,
    longitude: city.longitude
  }
}

export function getCityDisplayName(city) {
  const selectedCity = normalizeCity(city)
  if (!selectedCity) {
    return ''
  }
  if (Number(selectedCity.type) === 3 && selectedCity.cityName && selectedCity.cityName !== selectedCity.name) {
    return selectedCity.cityName
  }
  return selectedCity.name
}

export function getProgramQueryAreaId(city) {
  const selectedCity = normalizeCity(city)
  if (!hasRealAreaFilter(selectedCity)) {
    return undefined
  }
  return Number(selectedCity.type) === 3 && selectedCity.parentId ? selectedCity.parentId : selectedCity.id
}

export function getProgramQueryAreaIds(city) {
  const selectedCity = normalizeCity(city)
  if (!hasRealAreaFilter(selectedCity)) {
    return undefined
  }
  const areaIds = [selectedCity.id]
  if (Number(selectedCity.type) === 3 && selectedCity.parentId) {
    areaIds.push(selectedCity.parentId)
  }
  const childAreaIds = CITY_CHILD_AREA_ID_MAP[Number(selectedCity.id)] || []
  areaIds.push(...childAreaIds)
  return Array.from(new Set(areaIds.map(Number).filter(Boolean)))
}

export function getMovieQueryAreaId(city) {
  const selectedCity = normalizeCity(city)
  if (!hasRealAreaFilter(selectedCity)) {
    return undefined
  }
  return selectedCity.id
}

function hasRealAreaFilter(city) {
  if (!city || city.id === '') {
    return false
  }
  return Number(city.id) !== NATIONAL_AREA_ID && Number(city.type) !== 0
}

export function getStoredCity() {
  try {
    return normalizeCity(JSON.parse(localStorage.getItem(CITY_STORAGE_KEY) || 'null'))
  } catch (error) {
    localStorage.removeItem(CITY_STORAGE_KEY)
    return null
  }
}

export function setStoredCity(city) {
  const normalizedCity = normalizeCity(city)
  if (!normalizedCity) {
    return null
  }
  localStorage.setItem(CITY_STORAGE_KEY, JSON.stringify(normalizedCity))
  return normalizedCity
}

export function getBrowserLocatedCity() {
  if (typeof navigator === 'undefined' || !navigator.geolocation) {
    return Promise.resolve(null)
  }
  if (typeof window !== 'undefined' && !window.isSecureContext && !isLocalhost(window.location?.hostname)) {
    return Promise.resolve(null)
  }
  if (!browserLocatedCityPromise) {
    browserLocatedCityPromise = new Promise(resolve => {
      navigator.geolocation.getCurrentPosition(
        position => resolve(matchSupportedLocation(position.coords)),
        () => resolve(null),
        {enableHighAccuracy: true, timeout: 3200, maximumAge: 5 * 60 * 1000}
      )
    })
  }
  return browserLocatedCityPromise
}

function isLocalhost(hostname) {
  return hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '[::1]'
}

function matchSupportedLocation(coords) {
  if (!coords) {
    return null
  }
  const matched = SUPPORTED_BROWSER_LOCATIONS
    .map(item => ({...item, distance: distanceKm(coords.latitude, coords.longitude, item.latitude, item.longitude)}))
    .sort((a, b) => a.distance - b.distance)[0]
  if (!matched || matched.distance > matched.radiusKm) {
    return null
  }
  return normalizeCity({
    ...matched,
    latitude: coords.latitude,
    longitude: coords.longitude
  })
}

function distanceKm(lat1, lon1, lat2, lon2) {
  const radius = 6371
  const dLat = toRad(lat2 - lat1)
  const dLon = toRad(lon2 - lon1)
  const a = Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2
  return radius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

function toRad(value) {
  return value * Math.PI / 180
}

function normalizeId(value) {
  if (value === undefined || value === null || value === '') {
    return value
  }
  const numberValue = Number(value)
  return Number.isNaN(numberValue) ? value : numberValue
}
