<script setup>
import HomePage from './pages/HomePage.vue'
import CartPage from './pages/CartPage.vue'
import CheckoutPage from './pages/CheckoutPage.vue'
import AdminProductReviewDetailPage from './pages/AdminProductReviewDetailPage.vue'
import AdminProductReviewListPage from './pages/AdminProductReviewListPage.vue'
import AdminDashboardPage from './pages/AdminDashboardPage.vue'
import LegacyFallbackPage from './pages/LegacyFallbackPage.vue'
import LoginPage from './pages/LoginPage.vue'
import MerchantDashboardPage from './pages/MerchantDashboardPage.vue'
import MerchantProductEditPage from './pages/MerchantProductEditPage.vue'
import MerchantOrdersPage from './pages/MerchantOrdersPage.vue'
import MerchantProfilePage from './pages/MerchantProfilePage.vue'
import MerchantPublishPage from './pages/MerchantPublishPage.vue'
import MerchantProductsPage from './pages/MerchantProductsPage.vue'
import MerchantReturnAddressPage from './pages/MerchantReturnAddressPage.vue'
import OrderHistoryPage from './pages/OrderHistoryPage.vue'
import PaymentResultPage from './pages/PaymentResultPage.vue'
import ProfilePage from './pages/ProfilePage.vue'
import TopUpPage from './pages/TopUpPage.vue'
import WalletPage from './pages/WalletPage.vue'
import WithdrawPage from './pages/WithdrawPage.vue'
import ProductDetailPage from './pages/ProductDetailPage.vue'
import SearchResultPage from './pages/SearchResultPage.vue'
import SignupPage from './pages/SignupPage.vue'

const routes = {
  '/': HomePage,
  '/index.jsp': HomePage,
  '/public/login.jsp': LoginPage,
  '/login': LoginPage,
  '/public/signup.jsp': SignupPage,
  '/admin/product/review/list': AdminProductReviewListPage,
  '/admin/product/review': AdminProductReviewDetailPage,
  '/admin/dashboard': AdminDashboardPage,
  '/register': SignupPage,
  '/customer/product/search_result.jsp': SearchResultPage,
  '/customer/product/product_detail.jsp': ProductDetailPage,
  '/customer/order/cart.jsp': CartPage,
  '/customer/order/order_confirmation.jsp': CheckoutPage,
  '/customer/order/payment_success.jsp': PaymentResultPage,
  '/customer/order/payment_failed.jsp': PaymentResultPage,
  '/customer/orders': OrderHistoryPage,
  '/profile': ProfilePage,
  '/merchant/merchant_dashboard.jsp': MerchantDashboardPage,
  '/merchant/product/product_edit.jsp': MerchantProductEditPage,
  '/merchant/user/merchant_profile.jsp': MerchantProfilePage,
  '/merchant/product/publish.jsp': MerchantPublishPage,
  '/merchant/product/product_management.jsp': MerchantProductsPage,
  '/merchant/order/order_management': MerchantOrdersPage,
  '/public/wallet/wallet.jsp': WalletPage,
  '/public/wallet/topup.jsp': TopUpPage,
  '/public/wallet/withdraw.jsp': WithdrawPage,
}

const currentPage = window.location.pathname === '/merchant/order/order_management'
  && new URLSearchParams(window.location.search).get('action') === 'toReturnAddress'
  ? MerchantReturnAddressPage
  : routes[window.location.pathname] || LegacyFallbackPage
</script>

<template>
  <component :is="currentPage" />
</template>
