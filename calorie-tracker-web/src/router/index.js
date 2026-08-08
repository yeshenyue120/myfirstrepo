import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/Register.vue')
  },
  {
    path: '/forgot-password',
    name: 'forgot-password',
    component: () => import('@/views/ForgotPassword.vue')
  },
  {
    path: '/reset-password',
    name: 'reset-password',
    component: () => import('@/views/ResetPassword.vue')
  },
  {
    path: '/onboarding',
    name: 'onboarding',
    component: () => import('@/views/Onboarding.vue')
  },
  {
    path: '/home',
    name: 'home',
    component: () => import('@/views/Home.vue')
  },
  {
  path: '/profile',
  name: 'profile',
  component: () => import('@/views/Profile.vue')
},
{
  path: '/edit-profile',
  name: 'edit-profile',
  component: () => import('@/views/EditProfile.vue')
},
{
  path: '/foods',
  name: 'foods',
  component: () => import('@/views/Foods.vue')
},
{
  path: '/food-library',
  name: 'food-library',
  component: () => import('@/views/FoodLibrary.vue')
},
{
  path: '/report',
  name: 'report',
  component: () => import('@/views/Report.vue')
},
{
  path: '/report/intake',
  name: 'report-intake',
  component: () => import('@/views/IntakeTrendDetail.vue')
},
{
  path: '/exercise-record',
  name: 'exercise-record',
  component: () => import('@/views/ExerciseRecord.vue')
},
{
  path: '/:pathMatch(.*)*',
  redirect: '/home'
}
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

// 登录守卫：未登录访问受保护页 → 登录页；已登录访问登录/注册页 → 首页
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const publicRoutes = ['/login', '/register', '/forgot-password', '/reset-password']

  if (publicRoutes.includes(to.path)) {
    if (token) return next('/home')
    return next()
  }

  // 未登录：跳转登录页
  if (!token) return next('/login')

  const user = JSON.parse(localStorage.getItem('user') || '{}')
  // 已登录但还没完成 14 步引导 → 强制去 onboarding
  if (!user.gender && to.path !== '/onboarding') {
    return next('/onboarding')
  }
  next()
})

export default router