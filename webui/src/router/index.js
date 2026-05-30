import { createRouter, createWebHashHistory } from "vue-router";
import HomePage from "../views/HomePage.vue";
import CardsPage from "../views/CardsPage.vue";
import BattlePage from "../views/BattlePage.vue";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: "/", name: "home", component: HomePage },
    { path: "/cards", name: "cards", component: CardsPage },
    { path: "/battle", name: "battle", component: BattlePage }
  ]
});

export default router;
