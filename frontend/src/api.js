const defaultLegacyBase = import.meta.env.DEV ? '/legacy' : ''
export const legacyBase = import.meta.env.VITE_LEGACY_BASE_PATH ?? defaultLegacyBase
export const legacyAppBase = import.meta.env.VITE_LEGACY_APP_URL || (import.meta.env.DEV ? 'http://localhost:8081' : window.location.origin)

async function request(path) {
  const response = await fetch(`${legacyBase}${path}`, {
    headers: { Accept: 'application/json' },
    credentials: 'include',
  })

  if (!response.ok) {
    throw new Error(`Request failed (${response.status})`)
  }

  const result = await response.json()
  if (result.code !== 200) {
    throw new Error(result.msg || 'The API request failed')
  }
  return result.data
}

export function getProducts(filters = {}) {
  const query = new URLSearchParams()
  for (const [key, value] of Object.entries(filters)) {
    if (value !== undefined && value !== null && value !== '') {
      query.set(key, value)
    }
  }
  const suffix = query.size ? `?${query.toString()}` : ''
  return request(`/api/catalog/products${suffix}`)
}

export function getProduct(productId) {
  return request(`/api/catalog/products/${encodeURIComponent(productId)}`)
}

export function getCategories() {
  return request('/api/catalog/categories')
}

export function cartAction(action, fields = {}) {
  const body = new URLSearchParams({ action, ...fields })
  return fetch(`${legacyBase}/cart_action`, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
    },
    credentials: 'include',
    body,
  }).then(async (response) => {
    const result = await response.json()
    if (!response.ok || result.code !== 200) throw new Error(result.msg || 'The cart request failed')
    return result.data
  })
}

export function legacyPageUrl(path) {
  return new URL(path, `${legacyAppBase.replace(/\/$/, '')}/`).toString()
}

export async function getCurrentUser() {
  const response = await fetch(legacyBase + '/auth/session', {
    headers: { Accept: 'application/json' },
    credentials: 'include',
  })
  if (!response.ok) throw new Error('Could not read the current session')
  return response.json()
}

export function productImageUrl(imageUrl) {
  if (!imageUrl) return '/assets/images/product-placeholder.svg'
  if (/^https?:\/\//i.test(imageUrl)) return imageUrl
  const path = imageUrl.startsWith('/') ? imageUrl : `/${imageUrl}`
  return path
}
