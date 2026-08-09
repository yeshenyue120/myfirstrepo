package com.example.calorieserver.service;

import com.example.calorieserver.dto.LoginRequest;
import com.example.calorieserver.dto.RegisterRequest;
import com.example.calorieserver.dto.UpdateBodyInfoRequest;
import com.example.calorieserver.dto.UserResponse;
import com.example.calorieserver.entity.User;
import com.example.calorieserver.exception.BusinessException;
import com.example.calorieserver.repository.UserRepository;
import com.example.calorieserver.security.ForbiddenException;
import com.example.calorieserver.security.JwtUtil;
import com.example.calorieserver.security.SecurityUtil;
import com.example.calorieserver.util.TimeUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserService 单元测试：注册（唯一性校验/密码加密）、登录（邮箱回退用户名/密码校验）、
 * onboarding（14 步提交后系统计算）、体重变化重算目标（起始体重恢复）。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserService userService;

    private RegisterRequest registerReq() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("tester");
        req.setEmail("tester@example.com");
        req.setPassword("pass123");
        return req;
    }

    private LoginRequest loginReq(String account, String password) {
        LoginRequest req = new LoginRequest();
        req.setEmail(account);
        req.setPassword(password);
        return req;
    }

    private User userWithPassword(String password) {
        return User.builder()
                .id(1L)
                .username("tester")
                .email("tester@example.com")
                .password(new BCryptPasswordEncoder().encode(password))
                .role(User.Role.USER)
                .build();
    }

    // ===== 注册 =====

    @Test
    void register_emailExists_throws() {
        when(userRepository.existsByEmail("tester@example.com")).thenReturn(true);
        BusinessException ex = assertThrows(BusinessException.class, () -> userService.register(registerReq()));
        assertEquals("该邮箱已被注册", ex.getMessage());
    }

    @Test
    void register_usernameExists_throws() {
        when(userRepository.existsByEmail("tester@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("tester")).thenReturn(true);
        BusinessException ex = assertThrows(BusinessException.class, () -> userService.register(registerReq()));
        assertEquals("该用户名已被占用", ex.getMessage());
    }

    @Test
    void register_success_encodesPassword_returnsToken() {
        when(userRepository.existsByEmail("tester@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("tester")).thenReturn(false);
        when(jwtUtil.generateToken(1L, User.Role.USER)).thenReturn("jwt-token");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserResponse resp = userService.register(registerReq());

        assertEquals("jwt-token", resp.getToken());
        assertEquals("tester", resp.getUsername());
        assertEquals(User.Role.USER, resp.getRole());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertNotEquals("pass123", saved.getPassword()); // 密码必须加密存储
        assertTrue(new BCryptPasswordEncoder().matches("pass123", saved.getPassword()));
    }

    // ===== 登录 =====

    @Test
    void login_userNotFound_throws() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("nobody@example.com")).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> userService.login(loginReq("nobody@example.com", "pass123")));
    }

    @Test
    void login_wrongPassword_throws() {
        when(userRepository.findByEmail("tester@example.com")).thenReturn(Optional.of(userWithPassword("pass123")));
        assertThrows(BusinessException.class,
                () -> userService.login(loginReq("tester@example.com", "wrongpass")));
    }

    @Test
    void login_success_returnsToken() {
        when(userRepository.findByEmail("tester@example.com")).thenReturn(Optional.of(userWithPassword("pass123")));
        when(jwtUtil.generateToken(1L, User.Role.USER)).thenReturn("jwt-token");

        UserResponse resp = userService.login(loginReq("tester@example.com", "pass123"));

        assertEquals("jwt-token", resp.getToken());
        assertEquals("tester", resp.getUsername());
    }

    @Test
    void login_fallsBackToUsername() {
        when(userRepository.findByEmail("tester")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(userWithPassword("pass123")));
        when(jwtUtil.generateToken(1L, User.Role.USER)).thenReturn("jwt-token");

        UserResponse resp = userService.login(loginReq("tester", "pass123"));

        assertEquals("tester", resp.getUsername());
    }

    // ===== 修改密码 =====
    // changePassword 用静态 SecurityUtil.currentUserId() 取登录用户，需 mockStatic

    @Test
    void changePassword_notLoggedIn_throwsForbidden() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::currentUserId).thenReturn(null);
            assertThrows(ForbiddenException.class, () -> userService.changePassword("old", "new123"));
        }
    }

    @Test
    void changePassword_userNotFound_throws() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::currentUserId).thenReturn(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.empty());
            assertThrows(BusinessException.class, () -> userService.changePassword("old", "new123"));
        }
    }

    @Test
    void changePassword_wrongOldPassword_throws() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::currentUserId).thenReturn(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(userWithPassword("pass123")));
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.changePassword("wrongpass", "new123"));
            assertEquals("原密码错误", ex.getMessage());
        }
    }

    @Test
    void changePassword_sameAsOld_throws() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::currentUserId).thenReturn(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(userWithPassword("pass123")));
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.changePassword("pass123", "pass123"));
            assertEquals("新密码不能与原密码相同", ex.getMessage());
        }
    }

    @Test
    void changePassword_success_encodesNewPassword() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::currentUserId).thenReturn(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(userWithPassword("pass123")));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            userService.changePassword("pass123", "newpass123");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User saved = captor.getValue();
            assertNotEquals("newpass123", saved.getPassword()); // 新密码加密存储
            assertTrue(new BCryptPasswordEncoder().matches("newpass123", saved.getPassword()));
        }
    }

    // ===== 按邮箱查询 =====

    @Test
    void findByEmail_notFound_throws() {
        when(userRepository.findByEmail("x@y.com")).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> userService.findByEmail("x@y.com"));
    }

    @Test
    void findByEmail_success() {
        when(userRepository.findByEmail("tester@example.com")).thenReturn(Optional.of(userWithPassword("pass123")));
        assertEquals("tester@example.com", userService.findByEmail("tester@example.com").getEmail());
    }

    // ===== 14 步 onboarding 提交 =====

    private UpdateBodyInfoRequest onboardingReq() {
        UpdateBodyInfoRequest req = new UpdateBodyInfoRequest();
        req.setGender(User.Gender.MALE);
        req.setHeightCm(175.0);
        req.setWeightKg(70.0);
        req.setBirthDate(TimeUtil.today().minusYears(30)); // 年龄 30
        req.setTargetWeightKg(65.0); // 需减 5kg
        req.setTargetDays(null);      // 未填 → 应回填推荐天数
        req.setUsedWeightLossDrug(false);
        req.setEatingHabit(User.EatingHabit.REGULAR);
        req.setSocialEating(User.SocialEating.RARE);
        req.setHungerLevel(User.HungerLevel.RARE);
        req.setHasBodyFatScale(false);
        return req;
    }

    @Test
    void submitOnboarding_userNotFound_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> userService.submitOnboarding(1L, onboardingReq()));
    }

    @Test
    void submitOnboarding_success_computesTargets_fillsRecommendedDays() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).build()));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse resp = userService.submitOnboarding(1L, onboardingReq());

        assertNotNull(resp.getBmr());
        assertNotNull(resp.getTdee());
        assertNotNull(resp.getDailyCalorieTarget());
        assertNotNull(resp.getProteinRatio());
        assertNotNull(resp.getBodyFat());          // 未传体脂 → 估算
        assertNotNull(resp.getRecommendedDays());
        assertEquals(resp.getRecommendedDays(), resp.getTargetDays()); // 回填推荐天数
    }

    @Test
    void submitOnboarding_ageFallback_birthYearJanuary1() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).build()));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateBodyInfoRequest req = onboardingReq();
        req.setBirthDate(null);
        req.setAge(30);

        userService.submitOnboarding(1L, req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(LocalDate.of(TimeUtil.today().getYear() - 30, 1, 1), captor.getValue().getBirthDate());
    }

    // ===== 体重变化后重算目标 =====

    @Test
    void recalculate_userNotFound_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> userService.recalculateTargetsFromWeight(1L, 65.0));
    }

    @Test
    void recalculate_restoresOriginalWeight_afterRecalc() {
        User user = User.builder().id(1L).gender(User.Gender.MALE).heightCm(175.0).weightKg(70.0)
                .birthDate(TimeUtil.today().minusYears(30)).targetWeightKg(60.0).targetDays(100).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.recalculateTargetsFromWeight(1L, 65.0);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals(70.0, saved.getWeightKg()); // 起始体重被恢复（临时用新体重计算）
        assertNotNull(saved.getBmr());
        assertNotNull(saved.getTdee());
        assertNotNull(saved.getDailyCalorieTarget());
    }
}
