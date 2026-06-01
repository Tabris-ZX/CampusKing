import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import { installEdgeHistoryWorkaround } from "./lib/edge-history-workaround";
import "./styles/common.css";
import "./styles/home.css";
import "./styles/cards.css";
import "./styles/battle.css";

installEdgeHistoryWorkaround();

createApp(App).use(router).mount("#app");
