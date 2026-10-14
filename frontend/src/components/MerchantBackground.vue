<template>
  <div class="merchant-background" aria-hidden="true">
    <div class="background-blob blob-red"></div>
    <div class="background-blob blob-yellow"></div>
    <div class="background-blob blob-orange"></div>
  </div>
</template>

<style scoped>
.merchant-background { position: fixed; z-index: 0; inset: 0; overflow: hidden; pointer-events: none; }
.background-blob { position: absolute; border-radius: 50%; filter: drop-shadow(30px 40px 50px rgba(0,0,0,.2)); }
.blob-red { top: -200px; left: -200px; width: 750px; height: 650px; transform: rotate(-10deg); background: #fff; }
.blob-yellow { top: -250px; right: -100px; width: 900px; height: 700px; transform: rotate(30deg); background: linear-gradient(145deg,#ffdb4d,#e6b800); box-shadow: inset 10px 10px 40px rgba(255,255,255,.7),inset -30px -30px 60px rgba(184,134,11,.3); }
.blob-orange { bottom: -650px; left: -600px; width: 1800px; height: 950px; transform: rotate(-10deg); background: #fff; }
</style>
