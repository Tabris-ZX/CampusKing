import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import "./styles/common.css";
import "./styles/home.css";
import "./styles/cards.css";
import "./styles/battle.css";

createApp(App).use(router).mount("#app");
