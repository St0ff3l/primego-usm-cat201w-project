<script setup>
import HomePage from './pages/HomePage.vue'
import CartPage from './pages/CartPage.vue'
import CheckoutPage from './pages/CheckoutPage.vue'
import LegacyFallbackPage from './pages/LegacyFallbackPage.vue'
import LoginPage from './pages/LoginPage.vue'
import OrderHistoryPage from './pages/OrderHistoryPage.vue'
import PaymentResultPage from './pages/PaymentResultPage.vue'
import ProfilePage from './pages/ProfilePage.vue'
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
  '/customer/order/order_confirmation.jsp': CheckoutPage,
  '/customer/order/payment_success.jsp': PaymentResultPage,
  '/customer/order/payment_failed.jsp': PaymentResultPage,
  '/customer/orders': OrderHistoryPage,
  '/profile': ProfilePage,
}

const currentPage = routes[window.location.pathname] || LegacyFallbackPage
</script>

<template>
  <component :is="currentPage" />
</template>
