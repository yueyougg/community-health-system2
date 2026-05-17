<template>
  <button :class="['base-button', size]" :style="styleObject" v-bind="$attrs">
    <slot></slot>
  </button>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  type: {
    type: String,
    default: "primary", // primary, success, warning, danger, info
  },
  size: {
    type: String,
    default: "default", // default, small, large
  },
});

const colorMap = {
  primary: "185, 64%", // Teal/Blue (#1f7a8c)
  success: "95, 53%",  // Green (#67C23A)
  warning: "36, 77%",  // Orange (#E6A23C)
  danger: "355, 87%",  // Red (#F56C6C)
  info: "220, 4%",     // Gray (#909399)
  default: "185, 64%", // Fallback
};

const styleObject = computed(() => {
  return {
    "--back-color": colorMap[props.type] || colorMap.primary,
  };
});
</script>

<style scoped>
.base-button {
  --bezier: cubic-bezier(0.22, 0.61, 0.36, 1);
  --edge-light: hsla(0, 0%, 50%, 0.8);
  --text-light: rgba(255, 255, 255, 0.4);
  /* --back-color is set via inline style */

  cursor: pointer;
  border-radius: 0.5em;
  min-height: 2.4em;
  min-width: 3em;
  display: inline-flex; /* changed from flex to inline-flex for button behavior */
  align-items: center;
  justify-content: center; /* center text */
  gap: 0.5em;

  font-family: inherit; /* inherit font */
  line-height: 1;
  font-weight: bold;

  background: linear-gradient(
    140deg,
    hsla(var(--back-color), 50%, 1) min(2em, 20%),
    hsla(var(--back-color), 50%, 0.6) min(8em, 100%)
  );
  color: hsla(0, 0%, 90%);
  border: 0;
  box-shadow: inset 0.4px 0.1px 0.4px var(--edge-light);

  transition: all 0.1s var(--bezier);
}

/* Size Variants */
.base-button.default {
  font-size: 14px;
  padding: 0.7em 1em;
}

.base-button.small {
  font-size: 12px;
  padding: 0.5em 0.8em;
  min-height: 2em;
}

.base-button.large {
  font-size: 16px;
  padding: 0.9em 1.2em;
}

.base-button:hover:not(:disabled) {
  --edge-light: hsla(0, 0%, 50%, 1);
  text-shadow: 0px 0px 10px var(--text-light);
  box-shadow: inset 0.4px 1px 4px var(--edge-light),
    2px 4px 8px hsla(0, 0%, 0%, 0.295);
  transform: scale(1.05); /* slightly reduced scale for better UX */
}

.base-button:active:not(:disabled) {
  --text-light: rgba(255, 255, 255, 1);

  background: linear-gradient(
    140deg,
    hsla(var(--back-color), 50%, 1) min(2em, 20%),
    hsla(var(--back-color), 50%, 0.6) min(8em, 100%)
  );
  box-shadow: inset 0.4px 1px 8px var(--edge-light),
    0px 0px 8px hsla(var(--back-color), 50%, 0.6);
  text-shadow: 0px 0px 20px var(--text-light);
  color: hsla(0, 0%, 100%, 1);
  letter-spacing: 0.1em;
  transform: scale(1);
}

.base-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  filter: grayscale(0.8);
}
</style>