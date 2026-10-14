<script setup>
import { computed, onMounted, ref } from 'vue'
import { getCategories, getCurrentUser, getProducts, legacyBase, productImageUrl } from '../api'
import LoginCheckModal from '../components/LoginCheckModal.vue'
import PrimegoHeader from '../components/PrimegoHeader.vue'

const query = new URLSearchParams(window.location.search)
const keyword = ref(query.get('keyword') || '')
const categoryId = ref(query.get('categoryId') || '')
const minPrice = ref(query.get('minPrice') || '')
const maxPrice = ref(query.get('maxPrice') || '')
const categories = ref([])
const products = ref([])
const currentUser = ref(null)
const loginModalOpen = ref(false)
const loading = ref(true)
const error = ref('')

const resultTitle = computed(() => {
  let title = 'All Products'
  if (keyword.value) title = 'Search Results for "' + keyword.value + '"'
  else if (categoryId.value) title = 'Category Results'
  if (minPrice.value || maxPrice.value) title += ' (Filtered)'
  return title
})

function searchUrl(values) {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(values)) if (value) params.set(key, value)
  return '/customer/product/search_result.jsp' + (params.size ? '?' + params.toString() : '')
}

function addToCart(productId) {
  if (!currentUser.value) {
    loginModalOpen.value = true
    return
  }
  window.location.href = legacyBase + '/cart_action?action=add&productId=' + encodeURIComponent(productId) + '&quantity=1'
}

function imageFailed(event) {
  event.target.src = '/assets/images/product-placeholder.svg'
  event.target.onerror = null
}

async function loadPage() {
  const [categoryResult, productResult, userResult] = await Promise.allSettled([
    getCategories(),
    getProducts({
      keyword: keyword.value,
      categoryId: categoryId.value,
      minPrice: minPrice.value,
      maxPrice: maxPrice.value,
      inStockOnly: true,
      pageNum: 1,
      pageSize: 100,
    }),
    getCurrentUser(),
  ])
  if (categoryResult.status === 'fulfilled') categories.value = categoryResult.value
  if (productResult.status === 'fulfilled') products.value = productResult.value.rows
  else error.value = 'Unable to load products. Please try again later.'
  if (userResult.status === 'fulfilled' && userResult.value.authenticated) currentUser.value = userResult.value
  loading.value = false
}

onMounted(loadPage)
</script>

<template>
  <div class="search-page">
    <div class="background-blob blob-red"></div>
    <div class="background-blob blob-yellow"></div>
    <div class="background-blob blob-orange"></div>
    <PrimegoHeader :categories="categories" :current-user="currentUser" />

    <div class="search-bar-container">
      <form :action="searchUrl({})" method="get">
        <input v-model="keyword" type="text" name="keyword" class="search-input" placeholder="Search for treasures..." />
        <button type="submit" class="btn-search">Search</button>
      </form>
    </div>

    <div class="main-layout">
      <aside class="sidebar">
        <span class="filter-title">Categories</span>
        <a :href="searchUrl({ keyword })" :class="['filter-option', { active: !categoryId }]">All Categories</a>
        <a
          v-for="category in categories"
          :key="category.id"
          :href="searchUrl({ categoryId: category.id })"
          :class="['filter-option', { active: String(category.id) === categoryId }]"
        >
          {{ category.name }}
        </a>

        <hr class="filter-divider" />
        <span class="filter-title">Price Range</span>
        <form :action="searchUrl({})" method="get">
          <input v-if="keyword" type="hidden" name="keyword" :value="keyword" />
          <input v-if="categoryId" type="hidden" name="categoryId" :value="categoryId" />
          <div class="price-range">
            <input v-model="minPrice" type="number" min="0" step="0.01" name="minPrice" placeholder="Min" />
            <input v-model="maxPrice" type="number" min="0" step="0.01" name="maxPrice" placeholder="Max" />
          </div>
          <button type="submit" class="apply-filter">Apply</button>
        </form>
      </aside>

      <main>
        <h3 class="results-title">{{ resultTitle }}</h3>
        <div v-if="loading" class="product-grid" aria-live="polite" aria-busy="true">
          <div v-for="item in 4" :key="item" class="product-card loading-card"></div>
        </div>
        <p v-else-if="error" class="empty-products" role="alert">{{ error }}</p>
        <div v-else-if="products.length" class="product-grid">
          <article v-for="product in products" :key="product.id" class="product-card">
            <a class="product-link" :href="'/customer/product/product_detail.jsp?id=' + product.id">
              <div class="product-img-container">
                <img :src="productImageUrl(product.imageUrl)" :alt="product.name" @error="imageFailed" />
              </div>
              <div class="product-details">
                <h4 class="product-name">{{ product.name }}</h4>
                <p class="product-price">RM {{ Number(product.price).toFixed(2) }}</p>
                <p class="product-description">{{ (product.description || '').length > 50 ? product.description.slice(0, 50) + '...' : product.description }}</p>
              </div>
            </a>
            <button class="btn-add-cart" type="button" @click="addToCart(product.id)">Add to Cart</button>
          </article>
        </div>
        <div v-else class="empty-products">
          <h3>No products found.</h3>
          <p>Try adjusting your search or category.</p>
        </div>
      </main>
    </div>
    <LoginCheckModal :open="loginModalOpen" @close="loginModalOpen = false" />
  </div>
</template>

<style scoped>
.search-page { min-height: 100vh; position: relative; overflow: hidden; color: #333; }
.background-blob { position: fixed; border-radius: 50%; z-index: -1; pointer-events: none; filter: drop-shadow(30px 40px 50px rgba(0,0,0,.2)); }
.blob-red { width: 750px; height: 650px; top: -200px; left: -200px; transform: rotate(-10deg); background: linear-gradient(145deg,#ff5e55,#d92e25); box-shadow: inset 10px 10px 30px rgba(255,255,255,.5), inset -20px -20px 60px rgba(139,0,0,.4); }
.blob-yellow { width: 900px; height: 700px; top: -250px; right: -100px; transform: rotate(30deg); background: linear-gradient(145deg,#ffdb4d,#e6b800); box-shadow: inset 10px 10px 40px rgba(255,255,255,.7), inset -30px -30px 60px rgba(184,134,11,.3); }
.blob-orange { width: 1800px; height: 950px; bottom: -650px; left: -600px; transform: rotate(-10deg); background: linear-gradient(145deg,#ffad33,#e68a00); box-shadow: inset 15px 15px 50px rgba(255,255,255,.5), inset -40px -40px 80px rgba(160,82,45,.3); }
.search-bar-container { margin: 140px auto 30px; text-align: center; max-width: 800px; padding: 0 20px; }
.search-bar-container form { display: flex; justify-content: center; gap: 10px; }
.search-input { width: 70%; padding: 15px 25px; border: 0; border-radius: 30px; background: rgba(255,255,255,.8); backdrop-filter: blur(10px); box-shadow: 0 5px 15px rgba(0,0,0,.05); outline: none; font-size: 1rem; }
.btn-search { padding: 15px 30px; border: 0; border-radius: 30px; background: #333; color: white; cursor: pointer; font-weight: 600; }
.main-layout { max-width: 1200px; margin: 0 auto 50px; padding: 0 20px; display: grid; grid-template-columns: 250px 1fr; gap: 30px; align-items: start; }
.sidebar { padding: 25px; border-radius: 20px; background: rgba(255,255,255,.6); backdrop-filter: blur(20px); border: 1px solid rgba(255,255,255,.6); }
.filter-title { display: block; margin-bottom: 15px; font-weight: 700; }
.filter-option { display: block; margin-bottom: 10px; color: #555; text-decoration: none; transition: .2s; }
.filter-option:hover { color: #ff3b30; padding-left: 5px; }
.filter-option.active { color: #ff3b30; font-weight: 600; }
.filter-divider { border: 0; border-top: 1px solid rgba(0,0,0,.1); margin: 20px 0; }
.price-range { display: flex; gap: 5px; margin-bottom: 10px; }
.price-range input { min-width: 0; width: 100%; padding: 8px; border: 1px solid #ccc; border-radius: 10px; }
.apply-filter { width: 100%; padding: 8px; border: 0; border-radius: 10px; background: #333; color: white; cursor: pointer; font-weight: 600; }
.results-title { margin: 0 0 20px; color: #555; }
.product-grid { display: grid; grid-template-columns: repeat(auto-fill,minmax(260px,1fr)); gap: 25px; }
.product-card { height: 100%; overflow: hidden; display: flex; flex-direction: column; border: 1px solid rgba(255,255,255,.6); border-radius: 20px; background: rgba(255,255,255,.7); backdrop-filter: blur(20px); color: inherit; transition: transform .3s,box-shadow .3s; }
.product-card:hover { transform: translateY(-10px); box-shadow: 0 10px 20px rgba(0,0,0,.1); }
.product-link { display: flex; flex: 1; flex-direction: column; color: inherit; text-decoration: none; }
.product-img-container { width: 100%; aspect-ratio: 1; overflow: hidden; display: flex; align-items: center; justify-content: center; background: #f5f6fa; }
.product-img-container img { width: 100%; height: 100%; object-fit: cover; }
.product-details { flex: 1; padding: 15px; display: flex; flex-direction: column; }
.product-name { overflow: hidden; margin: 0 0 5px; font-size: 1rem; font-weight: 600; white-space: nowrap; text-overflow: ellipsis; }
.product-price { margin: 0 0 10px; color: #ff3b30; font-size: 1.1rem; font-weight: 700; }
.product-description { margin: 0 0 8px; color: #666; font-size: .85rem; line-height: 1.3; }
.btn-add-cart { margin: 0 15px 15px; padding: 8px; border: 2px solid #333; border-radius: 20px; background: transparent; color: #333; cursor: pointer; font-weight: 600; transition: all .3s; }
.btn-add-cart:hover { background: #333; color: white; }
.empty-products { grid-column: 1 / -1; padding: 40px; color: #666; text-align: center; }
.loading-card { min-height: 330px; animation: pulse 1.2s ease-in-out infinite alternate; }
@keyframes pulse { from { opacity: .55; } to { opacity: .9; } }
@media (max-width: 760px) { .search-bar-container { margin-top: 100px; } .search-bar-container form { gap: 7px; } .search-input { width: 70%; padding: 12px 16px; } .btn-search { padding: 12px 18px; } .main-layout { grid-template-columns: 1fr; padding: 0 14px; gap: 20px; } .sidebar { display: grid; grid-template-columns: 1fr 1fr; column-gap: 18px; } .filter-divider { grid-column: 1 / -1; width: 100%; } .product-grid { grid-template-columns: repeat(auto-fill,minmax(150px,1fr)); gap: 12px; } .product-details { padding: 9px; } .product-name { font-size: .9rem; } .product-price { font-size: 1rem; } .product-description { font-size: .76rem; } .btn-add-cart { margin: 0 9px 9px; font-size: .8rem; } }
</style>
