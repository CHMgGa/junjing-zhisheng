<template>
  <div class="page ai-page">
    <section class="panel ai-shell">
      <div class="ai-header">
        <div>
          <p class="eyebrow">Dify AI</p>
          <h2 class="section-title">羊肚菌专业 AI 助手</h2>
          <p class="muted">农户和管理员共用同一个 AI 识别与问答模块。</p>
        </div>
      </div>

      <iframe
        class="dify-frame"
        :src="difyChatUrl"
        title="羊肚菌专业 AI 助手"
        allow="microphone; camera; clipboard-write"
      />
    </section>
  </div>
</template>

<script setup>
const difyChatUrl =
  import.meta.env.VITE_DIFY_CHAT_URL || 'http://172.22.44.178:8090/chat/IDCeWnE4p0dMGXW7'
</script>

<style scoped>
.ai-page {
  height: calc(100vh - 132px);
  min-height: 620px;
  overflow: hidden;
}

.ai-shell {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  height: 100%;
  min-height: 0;
  padding: 18px;
}

.ai-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 14px;
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--brand-strong);
  font-weight: 900;
}

.section-title {
  margin-bottom: 6px;
}

.dify-frame {
  width: 100%;
  height: 100%;
  min-height: 0;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: #fff;
}

@media (max-width: 768px) {
  .ai-page {
    height: calc(100vh - 104px);
    min-height: 560px;
  }

  .ai-shell {
    padding: 12px;
  }

  .ai-header {
    padding-bottom: 10px;
  }
}
</style>
