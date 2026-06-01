import { createRouter, createWebHashHistory } from "vue-router";
import { isAdminAuthenticated } from "../lib/admin";
import { showToast } from "../lib/toast";
import HomePage from "../views/HomePage.vue";
import CardsPage from "../views/CardsPage.vue";
import BattlePage from "../views/BattlePage.vue";
import AdminPage from "../views/AdminPage.vue";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: "/", name: "home", component: HomePage },
    { path: "/cards", name: "cards", component: CardsPage },
    { path: "/battle", name: "battle", component: BattlePage },
    { path: "/admin", name: "admin", component: AdminPage }
  ]
});

router.beforeEach(to => {
  if (to.name !== "admin" || isAdminAuthenticated()) {
    return true;
  }
  showToast("请输入管理员密码后再进入管理后台", "error");
  return { name: "home" };
});

export default router;
