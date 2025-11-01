const ORDER_STATE_STORAGE_KEY = 'tides_pending_order_state'

export function saveOrderState(state) {
  if (!state || !state.detailList) {
    return null
  }
  sessionStorage.setItem(ORDER_STATE_STORAGE_KEY, JSON.stringify(state))
  return state
}

export function getOrderState(routeState) {
  if (routeState && routeState.detailList) {
    return saveOrderState(routeState)
  }
  try {
    return JSON.parse(sessionStorage.getItem(ORDER_STATE_STORAGE_KEY) || 'null')
  } catch (error) {
    sessionStorage.removeItem(ORDER_STATE_STORAGE_KEY)
    return null
  }
}

export function clearOrderState() {
  sessionStorage.removeItem(ORDER_STATE_STORAGE_KEY)
}
