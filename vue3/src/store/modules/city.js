import {defineStore} from 'pinia'
import {getCurrentCity, getHotCity, getOtherCity, getCityInfo} from '@/api/area'
import {
  getBrowserLocatedCity,
  getKnownCity,
  getMovieQueryAreaId,
  getProgramQueryAreaId,
  getProgramQueryAreaIds,
  getStoredCity,
  normalizeCity,
  setStoredCity
} from '@/utils/city'

let initCityPromise = null

function mergeCityList(list = [], extraList = []) {
  const map = new Map()
  ;[...extraList, ...list].filter(Boolean).forEach(item => {
    const city = normalizeCity(item)
    if (city) {
      map.set(Number(city.id), city)
    }
  })
  return Array.from(map.values())
}

const useCityStore = defineStore('city', {
  state: () => ({
    selectedCity: null,
    initialized: false,
    loading: false,
    hotCity: [],
    otherCity: []
  }),
  getters: {
    localName: state => state.selectedCity?.name || '',
    programAreaId: state => getProgramQueryAreaId(state.selectedCity),
    programAreaIds: state => getProgramQueryAreaIds(state.selectedCity),
    movieAreaId: state => getMovieQueryAreaId(state.selectedCity),
    allCityList: state => mergeCityList(state.otherCity, state.hotCity)
  },
  actions: {
    async initCity(options = {}) {
      const useBrowserLocation = Boolean(options.useBrowserLocation)
      if (this.initialized && this.selectedCity) {
        return this.selectedCity
      }
      if (initCityPromise) {
        return initCityPromise
      }
      this.loading = true
      initCityPromise = (async () => {
        let selectedCity = getStoredCity()
        if (!selectedCity && useBrowserLocation) {
          selectedCity = await getBrowserLocatedCity()
        }
        if (!selectedCity && useBrowserLocation) {
          selectedCity = getKnownCity(385)
        }
        if (!selectedCity) {
          try {
            const response = await getCurrentCity()
            selectedCity = response.data
          } catch (error) {
            selectedCity = null
          }
        }
        if (!selectedCity) {
          selectedCity = getKnownCity(385)
        }
        if (selectedCity) {
          this.applyCity(selectedCity)
        }
        this.initialized = true
        return this.selectedCity
      })()
      try {
        return await initCityPromise
      } finally {
        this.loading = false
        initCityPromise = null
      }
    },
    applyCity(city) {
      const selectedCity = setStoredCity(city)
      if (!selectedCity) {
        return null
      }
      this.selectedCity = selectedCity
      this.initialized = true
      return selectedCity
    },
    async selectCity(city) {
      const cityId = city?.id ?? city?.areaId
      if (!cityId) {
        return null
      }
      try {
        const response = await getCityInfo({id: cityId})
        return this.applyCity(response.data?.id ? response.data : city)
      } catch (error) {
        return this.applyCity(city)
      }
    },
    async loadCityOptions() {
      const [hotResult, otherResult] = await Promise.allSettled([getHotCity(), getOtherCity()])
      const hotData = hotResult.status === 'fulfilled' ? hotResult.value.data : []
      const otherData = otherResult.status === 'fulfilled' ? otherResult.value.data : []
      this.hotCity = mergeCityList(hotData, [getKnownCity(1), getKnownCity(385)])
      const hotCityIdSet = new Set(this.hotCity.map(item => Number(item.id)))
      this.otherCity = mergeCityList(otherData).filter(item => !hotCityIdSet.has(Number(item.id)))
      return {
        hotCity: this.hotCity,
        otherCity: this.otherCity
      }
    }
  }
})

export default useCityStore
