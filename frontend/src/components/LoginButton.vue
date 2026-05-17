<template>
  <button :class="['l-button', size]" :style="styleObject" v-bind="$attrs">
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
/* From Uiverse.io by mrhyddenn */ 
.l-button {
  background: transparent;
  position: relative;
  padding: 5px 15px;
  display: flex;
  align-items: center;
  font-size: 17px;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
  border: 1px solid rgb(40, 144, 241);
  border-radius: 25px;
  outline: none;
  overflow: hidden;
  color: rgb(40, 144, 241);
  transition: color 0.3s 0.1s ease-out;
  text-align: center;
}

.l-button span {
  margin: 10px;
}

.l-button::before {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  margin: auto;
  content: '';
  border-radius: 50%;
  display: block;
  width: 20em;
  height: 20em;
  left: -5em;
  text-align: center;
  transition: box-shadow 0.5s ease-out;
  z-index: -1;
}

.l-button:hover { 
  color: #fff;
  border: 1px solid rgb(40, 144, 241);
}

.l-button:hover::before {   
  box-shadow: inset 0 0 0 10em rgb(40, 144, 241);
}
 
</style>