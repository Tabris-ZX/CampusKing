import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import { installEdgeHistoryWorkaround } from "./lib/edge-history-workaround";
import { apiRoot, assetRoot, assetUrl } from "./lib/runtime-config";
import "./styles/common.css";
import "./styles/home.css";
import "./styles/cards.css";
import "./styles/battle.css";
import "./styles/tuner.css";

installEdgeHistoryWorkaround();
document.documentElement.style.setProperty("--topbar-image", `url("${assetUrl("images/ui/topbar.png")}")`);
document.getElementById("app-favicon")?.setAttribute("href", assetRoot() ? assetUrl("favicon.ico") : `${apiRoot()}/api/assets/favicon.ico`);

createApp(App).use(router).mount("#app");
