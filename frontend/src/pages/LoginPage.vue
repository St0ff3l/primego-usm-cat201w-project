<script setup>
import { ref } from 'vue'
import { legacyBase } from '../api'

const error = ref('')
const submitting = ref(false)

async function submitLogin(event) {
  error.value = ''
  submitting.value = true
  const body = new URLSearchParams(Object.fromEntries(new FormData(event.currentTarget).entries()))
  try {
    const response = await fetch(legacyBase + '/login', {
      method: 'POST',
      headers: { Accept: 'application/json', 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
      credentials: 'include',
      body,
    })
    const result = await response.json()
    if (!response.ok) {
      error.value = result.error || 'Invalid username or password'
      return
    }
    window.location.href = result.redirectUrl || '/index.jsp'
  } catch {
    error.value = 'Unable to connect. Please try again.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <div class="background-blob blob-red"></div>
    <div class="background-blob blob-yellow"></div>
    <div class="background-blob blob-orange"></div>
    <section class="login-card">
      <h2>Welcome Back</h2>
      <div v-if="error" class="error-msg" role="alert">{{ error }}</div>
      <form @submit.prevent="submitLogin">
        <div class="form-group">
          <label for="username">Username</label>
          <input id="username" name="username" type="text" required placeholder="Enter your username" autocomplete="username" />
        </div>
        <div class="form-group">
          <label for="password">Password</label>
          <input id="password" name="password" type="password" required placeholder="Enter your password" autocomplete="current-password" />
        </div>
        <button class="btn-login" type="submit" :disabled="submitting">{{ submitting ? 'Logging in...' : 'Login' }}</button>
      </form>
      <div class="signup-prompt">
        <span>Don't have an account? </span>
        <a href="/public/signup.jsp">Sign Up</a>
      </div>
      <a href="/index.jsp" class="back-link">← Back to Home</a>
    </section>
  </main>
</template>

<style scoped>
.auth-page { min-height: 100vh; width: 100%; display: flex; justify-content: center; align-items: center; position: relative; overflow: hidden; padding: 24px; color: #333; }
.background-blob { position: fixed; border-radius: 50%; z-index: -1; opacity: 1; filter: drop-shadow(30px 40px 50px rgba(0,0,0,.2)); pointer-events: none; }
.blob-red { width: 750px; height: 650px; top: -200px; left: -200px; transform: rotate(-10deg); background: linear-gradient(145deg,#ff5e55,#d92e25); box-shadow: inset 10px 10px 30px rgba(255,255,255,.5),inset -20px -20px 60px rgba(139,0,0,.4); }
.blob-yellow { width: 900px; height: 700px; top: -250px; right: -100px; transform: rotate(30deg); background: linear-gradient(145deg,#ffdb4d,#e6b800); box-shadow: inset 10px 10px 40px rgba(255,255,255,.7),inset -30px -30px 60px rgba(184,134,11,.3); }
.blob-orange { width: 1800px; height: 950px; bottom: -650px; left: -600px; transform: rotate(-10deg); background: linear-gradient(145deg,#ffad33,#e68a00); box-shadow: inset 15px 15px 50px rgba(255,255,255,.5),inset -40px -40px 80px rgba(160,82,45,.3); }
.login-card { width: 100%; max-width: 400px; padding: 40px; border: 1px solid rgba(255,255,255,.6); border-radius: 20px; background: rgba(255,255,255,.85); backdrop-filter: blur(20px); box-shadow: 0 8px 32px rgba(0,0,0,.1); }
h2 { margin: 0 0 30px; color: #2d3436; text-align: center; }
.form-group { margin-bottom: 20px; }
.form-group label { display: block; margin-bottom: 8px; color: #636e72; font-weight: 600; }
.form-group input { width: 100%; padding: 12px; border: 1px solid #ddd; border-radius: 10px; font-size: 1rem; transition: .3s; }
.form-group input:focus { outline: none; border-color: #ff9500; box-shadow: 0 0 0 3px rgba(255,149,0,.1); }
.btn-login { width: 100%; padding: 12px; border: 0; border-radius: 10px; background: linear-gradient(45deg,#ff3b30,#ff9500); color: white; font-size: 1.1rem; font-weight: 600; cursor: pointer; transition: .3s; }
.btn-login:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 5px 15px rgba(255,59,48,.3); }
.btn-login:disabled { opacity: .7; cursor: wait; }
.error-msg { margin-bottom: 15px; color: #ff3b30; font-size: .9rem; text-align: center; }
.signup-prompt { margin-top: 15px; text-align: center; color: #636e72; }
.signup-prompt a { color: #ff9500; text-decoration: none; font-weight: 600; }
.back-link { display: block; margin-top: 20px; color: #636e72; text-align: center; text-decoration: none; }
.back-link:hover { color: #2d3436; }
@media (max-width: 520px) { .login-card { padding: 32px 24px; } }
</style>
