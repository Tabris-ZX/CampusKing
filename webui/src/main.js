import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import { installEdgeHistoryWorkaround } from "./lib/edge-history-workaround";
import { apiUrl } from "./lib/api-url";
import { assetRoot, assetUrl } from "./lib/runtime-config";
import "./styles/common.css";
import "./styles/home.css";
import "./styles/cards.css";
import "./styles/battle.css";
import "./styles/effects.css";
import "./styles/tuner.css";

installEdgeHistoryWorkaround();
document.documentElement.style.setProperty("--topbar-image", `url("${apiUrl("/assets/ui/topbar.webp")}")`);
document.documentElement.style.setProperty("--page-background-image", `url("${apiUrl("/assets/ui/background.webp")}")`);
document.documentElement.style.setProperty("--action-point-image", `url("${apiUrl("/assets/card-textures/cards/actionPoints")}")`);
document.getElementById("app-favicon")?.setAttribute("href", assetRoot() ? assetUrl("favicon.ico") : apiUrl("/assets/favicon.ico"));

createApp(App).use(router).mount("#app");
