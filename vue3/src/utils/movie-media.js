const PIPI_IMAGE_HOST_PATTERN = /^https?:\/\/p\d+\.pipi\.cn\//i

const IMAGE_SIZE_MAP = {
  avatar: '240x240',
  poster: '520x760',
  wide: '960x540',
  origin: '1200x1200',
}

export function resolveMovieImageUrl(url, type = 'origin') {
  const value = String(url || '').trim()
  if (!value) {
    return ''
  }
  if (!PIPI_IMAGE_HOST_PATTERN.test(value) || value.includes('imageMogr2')) {
    return value
  }
  const size = IMAGE_SIZE_MAP[type] || IMAGE_SIZE_MAP.origin
  return `${value}${value.includes('?') ? '&' : '?'}imageMogr2/thumbnail/${size}%3E`
}

export function isMovieVideoUrl(url) {
  return /\.(mp4|webm|ogg)(\?.*)?$/i.test(String(url || ''))
}
