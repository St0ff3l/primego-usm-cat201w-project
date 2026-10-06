<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { getCategories, getProduct, getProducts, productImageUrl } from './api'

const categories = ref([])
const products = ref([])
const keyword = ref('')
const activeKeyword = ref('')
const selectedCategory = ref(null)
const loading = ref(true)
const error = ref('')
const selectedProduct = ref(null)
const detailLoading = ref(false)

const resultLabel = computed(() => {
  if (activeKeyword.value) return `Results for “${activeKeyword.value}”`
  if (selectedCategory.value) {
    return categories.value.find((category) => category.id === selectedCategory.value)?.name ?? 'Products'
  }
  return 'Picked for you'
})

async function loadProducts() {
  loading.value = true
  error.value = ''
  try {
    products.value = await getProducts({
      keyword: activeKeyword.value,
      categoryId: selectedCategory.value,
      inStockOnly: !activeKeyword.value && !selectedCategory.value,
    })
  } catch {
    error.value = 'We couldn’t load the collection. Check the connection and try again.'
  } finally {
    loading.value = false
  }
}

async function loadInitialData() {
  try {
    categories.value = await getCategories()
  } catch {
    categories.value = []
  }
  await loadProducts()
}

function submitSearch() {
  activeKeyword.value = keyword.value.trim()
  selectedCategory.value = null
  loadProducts()
}

function selectCategory(categoryId) {
  selectedCategory.value = categoryId
  activeKeyword.value = ''
  keyword.value = ''
  loadProducts()
}

async function openProduct(product) {
  selectedProduct.value = product
  detailLoading.value = true
  try {
    selectedProduct.value = await getProduct(product.id)
  } catch {
    // Keep the card data visible if the detail request is temporarily unavailable.
  } finally {
    detailLoading.value = false
  }
}

function closeProduct() {
  selectedProduct.value = null
}

function onKeydown(event) {
  if (event.key === 'Escape') closeProduct()
}

onMounted(() => {
  loadInitialData()
  window.addEventListener('keydown', onKeydown)
})

onUnmounted(() => window.removeEventListener('keydown', onKeydown))
</script>

<template>
  <div class="site-shell">
    <header class="topbar">
      <a class="brand" href="#top" aria-label="PrimeGo home">
        <img src="/assets/images/logo.png" alt="" width="42" height="42" />
        <span>primego<span class="brand-dot">.</span></span>
      </a>

      <nav class="desktop-nav" aria-label="Main navigation">
        <a class="nav-active" href="#collection">Discover</a>
        <a href="#story">Our point of view</a>
      </nav>

      <form class="search-form" role="search" @submit.prevent="submitSearch">
        <span class="search-icon" aria-hidden="true">⌕</span>
        <input v-model="keyword" type="search" placeholder="Find something good" aria-label="Search products" />
        <button type="submit" aria-label="Search">↗</button>
      </form>
    </header>

    <main id="top">
      <section class="hero" aria-labelledby="hero-title">
        <div class="hero-copy">
          <p class="eyebrow"><span class="eyebrow-line"></span> A marketplace with a point of view</p>
          <h1 id="hero-title">Good things<br />find their <em>people.</em></h1>
          <p class="hero-description">Thoughtful finds from independent merchants, gathered in one place for the everyday moments that matter.</p>
          <a class="hero-link" href="#collection">Explore the collection <span aria-hidden="true">↘</span></a>
        </div>
        <div class="hero-art" aria-hidden="true">
          <div class="sun-disc"></div>
          <div class="art-note">CURATED<br />WITH CARE</div>
          <div class="orbit orbit-one"></div>
          <div class="orbit orbit-two"></div>
          <div class="hero-seal"><span>FIND<br />YOUR<br />FAVORITE</span><b>✳</b></div>
          <div class="hero-art-caption">Small discoveries.<br />Big everyday energy.</div>
        </div>
        <div class="hero-index" aria-hidden="true">01 / 04</div>
      </section>

      <section id="collection" class="collection" aria-labelledby="collection-title">
        <div class="collection-heading">
          <div>
            <p class="eyebrow">THE PRIMEGO EDIT</p>
            <h2 id="collection-title">{{ resultLabel }}<span class="heading-star">✳</span></h2>
          </div>
          <p class="result-count">{{ loading ? 'Finding your next favorite…' : `${products.length} ${products.length === 1 ? 'find' : 'finds'}` }}</p>
        </div>

        <div class="category-row" aria-label="Filter by category">
          <button :class="['category-chip', { selected: selectedCategory === null && !activeKeyword }]" @click="selectCategory(null)">All finds</button>
          <button
            v-for="category in categories"
            :key="category.id"
            :class="['category-chip', { selected: selectedCategory === category.id }]"
            @click="selectCategory(category.id)"
          >
            {{ category.name }}
          </button>
        </div>

        <div v-if="loading" class="product-grid" aria-live="polite" aria-busy="true">
          <div v-for="item in 4" :key="item" class="product-card skeleton-card">
            <div class="skeleton-image"></div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
          </div>
        </div>

        <div v-else-if="error" class="empty-state" role="alert">
          <span class="empty-symbol">↻</span>
          <h3>Let’s try that again.</h3>
          <p>{{ error }}</p>
          <button class="text-button" @click="loadProducts">Reload collection <span>↗</span></button>
        </div>

        <div v-else-if="products.length" class="product-grid">
          <article v-for="(product, index) in products" :key="product.id" class="product-card" :style="{ '--card-index': index }">
            <button class="product-open" :aria-label="`View ${product.name}`" @click="openProduct(product)">
              <div class="product-image-wrap">
                <img :src="productImageUrl(product.imageUrl)" :alt="product.name" loading="lazy" @error="(event) => { event.target.src = '/assets/images/product-placeholder.svg' }" />
                <span class="image-index">{{ String(index + 1).padStart(2, '0') }}</span>
                <span class="quick-view">Take a closer look <b>↗</b></span>
              </div>
              <div class="product-info">
                <p class="product-category">{{ product.categoryName || 'THE EVERYDAY EDIT' }}</p>
                <h3>{{ product.name }}</h3>
                <p class="product-description">{{ product.description || 'A thoughtful find for your everyday.' }}</p>
                <div class="product-bottom">
                  <span class="product-price">RM {{ Number(product.price).toFixed(2) }}</span>
                  <span class="product-arrow" aria-hidden="true">↗</span>
                </div>
              </div>
            </button>
          </article>
        </div>

        <div v-else class="empty-state">
          <span class="empty-symbol">✳</span>
          <h3>No finds just yet.</h3>
          <p>Try another search or take a look at all the latest arrivals.</p>
          <button class="text-button" @click="selectCategory(null)">Show all finds <span>↗</span></button>
        </div>
      </section>

      <section id="story" class="story-band">
        <span class="story-mark" aria-hidden="true">✳</span>
        <p>Made for the curious.<br /><em>Chosen for the everyday.</em></p>
        <a href="#collection" aria-label="Back to collection">↑</a>
      </section>
    </main>

    <footer class="footer">
      <a class="brand footer-brand" href="#top"><img src="/assets/images/logo.png" alt="" width="30" height="30" /><span>primego<span class="brand-dot">.</span></span></a>
      <span>GOOD FINDS, GOOD DAYS.</span>
      <span>© PrimeGo · USM CAT201 Project</span>
    </footer>

    <div v-if="selectedProduct" class="modal-backdrop" @click.self="closeProduct">
      <section class="product-modal" role="dialog" aria-modal="true" :aria-label="selectedProduct.name">
        <button class="modal-close" aria-label="Close product details" @click="closeProduct">×</button>
        <div class="modal-image"><img :src="productImageUrl(selectedProduct.imageUrl)" :alt="selectedProduct.name" @error="(event) => { event.target.src = '/assets/images/product-placeholder.svg' }" /></div>
        <div class="modal-copy">
          <p class="eyebrow">{{ selectedProduct.categoryName || 'THE PRIMEGO EDIT' }}</p>
          <h2>{{ selectedProduct.name }}</h2>
          <p class="modal-merchant" v-if="selectedProduct.merchantName">A find from {{ selectedProduct.merchantName }}</p>
          <p class="modal-description">{{ detailLoading ? 'Gathering the details…' : selectedProduct.description }}</p>
          <div class="modal-price">RM {{ Number(selectedProduct.price).toFixed(2) }}</div>
          <p class="stock-note">{{ selectedProduct.stockQuantity > 0 ? 'Ready to find its way to you' : 'Currently out of stock' }}</p>
        </div>
      </section>
    </div>
  </div>
</template>
