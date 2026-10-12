<script setup>
import HomePage from './pages/HomePage.vue'
import CartPage from './pages/CartPage.vue'
import LegacyFallbackPage from './pages/LegacyFallbackPage.vue'
import LoginPage from './pages/LoginPage.vue'
import ProductDetailPage from './pages/ProductDetailPage.vue'
import SearchResultPage from './pages/SearchResultPage.vue'
import SignupPage from './pages/SignupPage.vue'

const routes = {
  '/': HomePage,
  '/index.jsp': HomePage,
  '/public/login.jsp': LoginPage,
  '/login': LoginPage,
  '/public/signup.jsp': SignupPage,
  '/register': SignupPage,
  '/customer/product/search_result.jsp': SearchResultPage,
  '/customer/product/product_detail.jsp': ProductDetailPage,
  '/customer/order/cart.jsp': CartPage,
}

const currentPage = routes[window.location.pathname] || LegacyFallbackPage
</script>

<template>
  <component :is="currentPage" />
</template>
