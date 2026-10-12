<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { legacyPageUrl } from '../api'

defineProps({
  categories: { type: Array, default: () => [] },
  currentUser: { type: Object, default: null },
  merchantRoute: { type: Boolean, default: false },
})

const searchOpen = ref(false)
const aboutOpen = ref(false)
const searchInput = ref(null)

function toggleSearch() {
  searchOpen.value = !searchOpen.value
  if (searchOpen.value) searchInput.value?.focus()
}

function closeOnEscape(event) {
  if (event.key === 'Escape') {
    searchOpen.value = false
    aboutOpen.value = false
  }
}

onMounted(() => window.addEventListener('keydown', closeOnEscape))
onUnmounted(() => window.removeEventListener('keydown', closeOnEscape))
</script>

<template>
  <header :class="['home-header', { 'pg-header-merchant-minimal': merchantRoute }]">
    <div class="navbar">
      <a href="/index.jsp" class="brand-link">
        <img src="/assets/images/logo.png" alt="PrimeGo Logo" class="brand-logo-img" width="40" height="40" />
        <span class="brand-text">PrimeGo</span>
      </a>

      <ul class="nav-menu">
        <li><a href="/index.jsp">Home</a></li>
        <li class="nav-item-dropdown">
          <a href="#categories" class="dropdown-trigger">Categories ▾</a>
          <ul class="dropdown-menu">
            <li v-if="categories.length === 0"><a href="#categories">No Categories</a></li>
            <li v-for="category in categories" :key="category.id">
              <a :href="'/customer/product/search_result.jsp?categoryId=' + category.id">{{ category.name }}</a>
            </li>
          </ul>
        </li>
        <li><a href="#about" @click.prevent="aboutOpen = true">About Us</a></li>
        <li v-if="!currentUser"><a href="/public/login.jsp">Login</a></li>
        <template v-else>
          <li v-if="currentUser.role === 'ADMIN'"><a href="/admin/dashboard">Dashboard</a></li>
          <li v-if="currentUser.role === 'MERCHANT'"><a href="/merchant/merchant_dashboard.jsp">Dashboard</a></li>
          <li><a href="/profile">Profile</a></li>
          <li><a :href="legacyPageUrl('/logout')">Logout</a></li>
        </template>
      </ul>

      <div class="nav-icons">
        <div :class="['search-container', { active: searchOpen }]">
          <form action="/customer/product/search_result.jsp" method="get" class="search-form">
            <input ref="searchInput" type="text" name="keyword" class="search-input-header" placeholder="Search..." />
            <button type="submit" class="search-btn-header" aria-label="Search"><i class="ri-search-line"></i></button>
          </form>
        </div>
        <button class="nav-icon nav-icon-search" type="button" title="Search" aria-label="Search" @click="toggleSearch">
          <i class="ri-search-line"></i>
        </button>
        <a class="nav-icon nav-icon-cart" href="/customer/order/cart.jsp" title="Cart" aria-label="Cart">
          <i class="ri-shopping-cart-fill"></i>
        </a>
        <a v-if="!currentUser" href="/public/login.jsp" class="nav-login-btn-link"><span class="nav-login-btn">Login</span></a>
        <template v-else>
          <a v-if="merchantRoute && currentUser.role === 'MERCHANT'" class="pg-merchant-wallet" href="/public/wallet/wallet.jsp" title="Wallet">Wallet</a>
          <span v-if="merchantRoute && currentUser.role === 'MERCHANT'" class="pg-merchant-username" :title="currentUser.username">{{ currentUser.username }}</span>
          <a href="/profile" class="nav-avatar" :title="'Current User: ' + currentUser.username">
            {{ (currentUser.username || '?').charAt(0).toUpperCase() }}
          </a>
        </template>
      </div>
    </div>
  </header>

  <div v-if="aboutOpen" class="about-modal-overlay" @click.self="aboutOpen = false">
    <div class="about-modal-content">
      <button class="about-modal-close" type="button" aria-label="Close" @click="aboutOpen = false">&times;</button>
      <h2 class="about-title">👥 About Us</h2>
      <div class="about-course">CAT201 Project</div>
      <p class="about-description">
        Welcome to PrimeGo, a comprehensive B2C e-commerce solution designed to provide a seamless premium shopping experience. This project demonstrates full-stack Java web development capabilities including user management, product handling, and secure transactions.
        <br /><br />
        1. If you are a user, please contact: User@PrimeGo.com<br />
        2. If you want to become a merchant, please contact: Merchant@PrimeGo.com
      </p>
      <ul class="team-list">
        <li class="team-member"><span class="member-name">ZHANG ZHIQI</span><span class="member-id">23101918</span></li>
        <li class="team-member"><span class="member-name">ZHANG YIFEI</span><span class="member-id">23101912</span></li>
        <li class="team-member"><span class="member-name">ZHANG JUN</span><span class="member-id">23101903</span></li>
        <li class="team-member"><span class="member-name">CHEN ZEKAI</span><span class="member-id">23101653</span></li>
      </ul>
    </div>
  </div>
</template>

<style>
.home-header, .home-header *, .home-header *::before, .home-header *::after { box-sizing: border-box; }
.home-header, .home-header a, .home-header input, .home-header button, .home-header span:not(.nav-icon) { font-family: 'Poppins', sans-serif; }
.home-header i[class^='ri-'], .home-header i[class*=' ri-'] { font-family: 'remixicon' !important; font-style: normal; -webkit-font-smoothing: antialiased; line-height: 1; vertical-align: middle; display: inline-block; }
.home-header { position: fixed; top: 15px; left: 50%; transform: translateX(-50%); z-index: 1000; width: 92%; max-width: 1300px; border-radius: 50px; padding: 12px 40px; background: rgba(255,255,255,.85); backdrop-filter: blur(25px); -webkit-backdrop-filter: blur(25px); border: 1px solid rgba(255,255,255,.9); box-shadow: 0 10px 30px rgba(0,0,0,.1), 0 4px 6px rgba(0,0,0,.05); transition: background .3s ease, box-shadow .3s ease; }
.navbar { display: flex; justify-content: space-between; align-items: center; width: 100%; min-width: 0; }
.home-header .brand-link, .home-header .nav-icons { flex: 0 0 auto; min-width: 0; }
.home-header .nav-menu { flex: 1 1 auto; min-width: 0; }
.brand-link { display: flex; align-items: center; gap: 12px; text-decoration: none; cursor: pointer; }
.brand-logo-img { height: 40px; width: auto; object-fit: contain; transition: transform .3s ease; }
.brand-text { font-size: 1.5rem; font-weight: 700; color: #d63031; letter-spacing: 1px; line-height: 1; transition: color .3s ease; }
.brand-link:hover .brand-logo-img { transform: scale(1.1); }
.brand-link:hover .brand-text { color: #ff9500; }
.nav-menu { display: flex; justify-content: center; align-items: center; list-style: none; gap: 20px; margin: 0; padding: 0; }
.nav-menu a { text-decoration: none; color: #444; font-weight: 600; font-size: .92rem; padding: 8px 12px; border-radius: 20px; transition: all .3s ease; white-space: nowrap; }
.nav-menu a:hover { color: #ff9500; background: rgba(0,0,0,.03); }
.nav-icons { position: relative; display: flex; align-items: center; gap: 14px; white-space: nowrap; }
.nav-icons .nav-icon { cursor: pointer; font-size: 1.5rem; display: inline-flex; align-items: center; justify-content: center; transition: transform .3s, color .3s; color: #333; text-decoration: none; border: 0; background: transparent; padding: 0; }
.nav-icons .nav-icon:hover { transform: scale(1.1); color: #ff9500; }
.nav-login-btn-link { text-decoration: none; margin-left: 10px; }
.nav-login-btn { display: inline-block; padding: 8px 20px; border-radius: 20px; background: #333; color: white; cursor: pointer; font-weight: 600; transition: all .3s ease; }
.nav-login-btn:hover { background: #ff9500; transform: translateY(-2px); box-shadow: 0 4px 10px rgba(255,149,0,.3); }
.nav-avatar { display: inline-flex; align-items: center; justify-content: center; width: 40px; height: 40px; background: linear-gradient(135deg,#007bff,#0056b3); color: white; border-radius: 50%; font-weight: bold; font-size: 1.2rem; text-decoration: none; box-shadow: 0 4px 10px rgba(0,123,255,.3); transition: transform .3s; }
.nav-avatar:hover { transform: scale(1.1) rotate(5deg); }
.pg-header-merchant-minimal .nav-menu, .pg-header-merchant-minimal .nav-login-btn-link, .pg-header-merchant-minimal .nav-icons .nav-icon { display: none !important; }
.pg-header-merchant-minimal .nav-icons { gap: 18px; }
.pg-merchant-username { font-weight: 700; color: #2d3436; font-size: .98rem; max-width: 220px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.pg-merchant-wallet { text-decoration: none; color: #444; font-weight: 600; font-size: 1rem; padding: 8px 16px; border-radius: 20px; }
.pg-merchant-wallet:hover { color: #ff9500; background: rgba(0,0,0,.03); }
.nav-item-dropdown { position: relative; }
.dropdown-menu { display: none; position: absolute; top: 100%; left: 0; background: rgba(255,255,255,.95); backdrop-filter: blur(10px); box-shadow: 0 10px 30px rgba(0,0,0,.1); border: 1px solid rgba(255,255,255,.9); border-radius: 15px; padding: 10px 0; min-width: 200px; flex-direction: column; gap: 2px; z-index: 1001; list-style: none; margin: 10px 0 0; animation: fadeInDown .3s ease; }
.nav-item-dropdown:hover .dropdown-menu { display: flex; }
.dropdown-menu li { width: 100%; }
.dropdown-menu a { display: block; padding: 10px 20px; white-space: nowrap; color: #444; font-weight: 500; transition: all .2s; border-radius: 0; }
.dropdown-menu a:hover { background: rgba(255,149,0,.1); color: #ff9500; padding-left: 25px; }
.search-container { display: none; position: absolute; top: 100%; right: 0; background: white; padding: 10px; border-radius: 10px; box-shadow: 0 5px 15px rgba(0,0,0,.1); margin-top: 10px; }
.search-container.active { display: block; animation: fadeInDown .3s ease; }
.search-form { display: flex; align-items: center; gap: 5px; }
.search-input-header { padding: 8px 12px; border: 1px solid #ddd; border-radius: 20px; outline: none; font-size: .9rem; width: 200px; }
.search-btn-header { background: none; border: none; cursor: pointer; font-size: 1.2rem; display: flex; align-items: center; justify-content: center; color: #444; }
.about-modal-overlay { position: fixed; inset: 0; display: flex; background: rgba(0,0,0,.5); backdrop-filter: blur(5px); z-index: 2000; justify-content: center; align-items: center; padding: 20px; }
.about-modal-content { background: rgba(255,255,255,.95); padding: 40px; border-radius: 20px; width: 500px; max-width: 90%; box-shadow: 0 20px 50px rgba(0,0,0,.2); position: relative; text-align: center; border: 1px solid rgba(255,255,255,.8); }
.about-modal-close { position: absolute; top: 15px; right: 20px; background: none; border: none; font-size: 1.5rem; cursor: pointer; color: #888; }
.about-title { font-size: 1.8rem; font-weight: 700; color: #2d3436; margin: 0 0 10px; display: flex; align-items: center; justify-content: center; gap: 10px; }
.about-course { font-size: 1rem; color: #ff9500; font-weight: 600; margin-bottom: 25px; text-transform: uppercase; letter-spacing: 1px; }
.about-description { color: #636e72; line-height: 1.6; margin: 0 0 30px; font-size: .95rem; }
.team-list { list-style: none; margin: 0; text-align: left; background: #fdfdfd; border-radius: 15px; padding: 20px; border: 1px solid #eee; }
.team-member { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px dashed #eee; color: #2d3436; font-weight: 500; }
.team-member:last-child { border-bottom: 0; }
.member-name { color: #333; }
.member-id { color: #888; font-family: monospace; }
@keyframes fadeInDown { from { opacity: 0; transform: translateY(-10px); } to { opacity: 1; transform: translateY(0); } }
@media (max-width: 1050px) { .home-header { padding: 12px 22px; } .nav-menu { gap: 8px; } .nav-menu a { padding: 8px; font-size: .82rem; } .nav-icons { gap: 9px; } }
@media (max-width: 760px) { .home-header { width: calc(100% - 24px); top: 10px; padding: 9px 14px; } .brand-link { gap: 6px; } .brand-logo-img { height: 32px; } .brand-text { font-size: 1.12rem; } .nav-menu { display: none; } .nav-icons { gap: 11px; } .nav-icons .nav-icon { font-size: 1.25rem; } .nav-login-btn-link { margin-left: 0; } .nav-login-btn { padding: 7px 12px; font-size: .82rem; } .nav-avatar { width: 34px; height: 34px; } }
</style>
