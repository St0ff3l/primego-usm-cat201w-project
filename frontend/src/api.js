const apiBase = import.meta.env.VITE_API_BASE_URL ?? ''

async function request(path) {
  const response = await fetch(`${apiBase}${path}`, {
    headers: { Accept: 'application/json' },
  })

  if (!response.ok) {
    throw new Error(`Request failed (${response.status})`)
  }

  return response.json()
}

export function getProducts(filters = {}) {
  const query = new URLSearchParams()
  for (const [key, value] of Object.entries(filters)) {
    if (value !== undefined && value !== null && value !== '') {
      query.set(key, value)
    }
  }
  const suffix = query.size ? `?${query.toString()}` : ''
  return request(`/api/products${suffix}`)
}

export function getProduct(productId) {
  return request(`/api/products/${encodeURIComponent(productId)}`)
}

export function getCategories() {
  return request('/api/categories')
}

export function productImageUrl(imageUrl) {
  if (!imageUrl) return '/assets/images/product-placeholder.svg'
  if (/^https?:\/\//i.test(imageUrl)) return imageUrl
  const path = imageUrl.startsWith('/') ? imageUrl : `/${imageUrl}`
  return `${apiBase}${path}`
}
