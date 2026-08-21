package com.saigonplantravel.backend.common.security;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.saigonplantravel.backend.admin.controller.AdminCategoryController;
import com.saigonplantravel.backend.admin.controller.AdminLoginController;
import com.saigonplantravel.backend.admin.controller.AdminPlaceCategoryController;
import com.saigonplantravel.backend.admin.controller.AdminPlaceController;
import com.saigonplantravel.backend.admin.controller.AdminPlaceOpeningHoursController;
import com.saigonplantravel.backend.auth.controller.AuthController;
import com.saigonplantravel.backend.auth.domain.UserRole;
import com.saigonplantravel.backend.auth.entity.UserAccount;
import com.saigonplantravel.backend.auth.repository.UserAccountRepository;
import com.saigonplantravel.backend.auth.security.JwtAuthenticationService;
import com.saigonplantravel.backend.auth.security.UserAccountDetailsService;
import com.saigonplantravel.backend.auth.security.UserPrincipal;
import com.saigonplantravel.backend.auth.service.AuthService;
import com.saigonplantravel.backend.common.security.jwt.AccessTokenClaims;
import com.saigonplantravel.backend.common.security.jwt.JwtService;
import com.saigonplantravel.backend.place.dto.PageResponse;
import com.saigonplantravel.backend.place.service.CategoryService;
import com.saigonplantravel.backend.place.service.PlaceImageService;
import com.saigonplantravel.backend.place.service.PlaceService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@WebMvcTest(
        controllers = {
            AdminLoginController.class,
            AdminCategoryController.class,
            AdminPlaceController.class,
            AdminPlaceCategoryController.class,
            AdminPlaceOpeningHoursController.class,
            AuthController.class
        })
@Import({
    SecurityConfig.class,
    RestAuthenticationEntryPoint.class,
    UserAccountDetailsService.class,
    AdminSecurityConfigTest.SecurityProbeController.class
})
@TestPropertySource(properties = "app.security.cors.allowed-origin=http://localhost:3000")
class AdminSecurityConfigTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserAccountRepository userAccountRepository;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationService jwtAuthenticationService;

    @MockitoBean
    private PlaceService placeService;

    @MockitoBean
    private PlaceImageService placeImageService;

    @MockitoBean
    private CategoryService categoryService;

    @BeforeEach
    void setUpPlacePage() {
        when(placeService.getPlacesForAdministration(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true, true));
    }

    @Test
    void redirectsAnonymousAdminRequestToAdminLogin() throws Exception {
        mockMvc.perform(get("/admin/places")).andExpect(status().isFound()).andExpect(redirectedUrl("/admin/login"));
    }

    @Test
    void allowsAnonymousAccessToAdminLoginPage() throws Exception {
        mockMvc.perform(get("/admin/login")).andExpect(status().isOk()).andExpect(view().name("admin/login"));
    }

    @Test
    void redirectsAnonymousCategoryCatalogToAdminLogin() throws Exception {
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin/login"));
    }

    @Test
    void rejectsCreateCategoryWithoutCsrf() throws Exception {
        mockMvc.perform(post("/admin/categories")
                        .param("name", "Science")
                        .param("slug", "science")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsUpdateCategoryWithoutCsrf() throws Exception {
        mockMvc.perform(post("/admin/categories/science")
                        .param("name", "Natural Science")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsPlaceStatusChangeWithoutCsrf() throws Exception {
        mockMvc.perform(post("/admin/places/demo-art-space/deactivate")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void forbidsUserFromChangingPlaceStatus() throws Exception {
        mockMvc.perform(post("/admin/places/demo-art-space/activate")
                        .with(user("user@example.com").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void redirectsAnonymousCategoryFormToAdminLogin() throws Exception {
        mockMvc.perform(get("/admin/places/demo-art-space/categories/edit"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin/login"));
    }

    @Test
    void forbidsUserFromCategoryForm() throws Exception {
        mockMvc.perform(get("/admin/places/demo-art-space/categories/edit")
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsCategoryReplacementWithoutCsrf() throws Exception {
        mockMvc.perform(post("/admin/places/demo-art-space/categories/edit")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void redirectsAnonymousOpeningHoursFormToAdminLogin() throws Exception {
        mockMvc.perform(get("/admin/places/demo-art-space/opening-hours/edit"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin/login"));
    }

    @Test
    void rejectsOpeningHoursReplacementWithoutCsrf() throws Exception {
        mockMvc.perform(post("/admin/places/demo-art-space/opening-hours/edit")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void redirectsAnonymousEditPlaceFormToAdminLogin() throws Exception {
        mockMvc.perform(get("/admin/places/demo-art-space/edit"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin/login"));
    }

    @Test
    void forbidsUserFromEditPlaceForm() throws Exception {
        mockMvc.perform(get("/admin/places/demo-art-space/edit")
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsUpdatePlacePostWithoutCsrf() throws Exception {
        mockMvc.perform(post("/admin/places/demo-art-space")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void redirectsAnonymousCreatePlaceFormToAdminLogin() throws Exception {
        mockMvc.perform(get("/admin/places/new"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin/login"));
    }

    @Test
    void forbidsUserFromCreatePlaceForm() throws Exception {
        mockMvc.perform(get("/admin/places/new").with(user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsCreatePlacePostWithoutCsrf() throws Exception {
        mockMvc.perform(post("/admin/places").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void forbidsUserFromProtectedAdminPage() throws Exception {
        mockMvc.perform(get("/admin/places").with(user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsAdminToAccessProtectedAdminPage() throws Exception {
        mockMvc.perform(get("/admin/places").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/places/list"));
    }

    @Test
    void rejectsAdminPostWithoutCsrf() throws Exception {
        mockMvc.perform(post("/admin/security-probe")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsAdminPostWithCsrf() throws Exception {
        mockMvc.perform(post("/admin/security-probe")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("accepted"));
    }

    @Test
    void keepsRestAuthenticationFailureAsJsonUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Authentication failed"));
    }

    @Test
    void keepsValidJwtAuthenticationWorking() throws Exception {
        UUID publicId = UUID.randomUUID();
        AccessTokenClaims claims = new AccessTokenClaims(publicId, UserRole.USER);
        UserPrincipal principal = new UserPrincipal(42L, publicId, "user@example.com", "Test User", UserRole.USER);
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.authorities());
        when(jwtService.parseAccessToken("valid-token")).thenReturn(claims);
        when(jwtAuthenticationService.createAuthentication(claims)).thenReturn(authentication);

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(publicId.toString()))
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    void formLoginUsesNormalizedEmailAndExistingPasswordHash() throws Exception {
        UserAccount userAccount = org.mockito.Mockito.mock(UserAccount.class);
        when(userAccount.getEmail()).thenReturn("admin@example.com");
        when(userAccount.getPasswordHash()).thenReturn(passwordEncoder.encode("secret"));
        when(userAccount.getRole()).thenReturn(UserRole.ADMIN);
        when(userAccount.getActive()).thenReturn(true);
        when(userAccountRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(userAccount));

        mockMvc.perform(post("/admin/login")
                        .with(csrf())
                        .param("email", "  ADMIN@EXAMPLE.COM ")
                        .param("password", "secret"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin/places"));

        verify(userAccountRepository).findByEmail("admin@example.com");
    }

    @Controller
    static class SecurityProbeController {
        @PostMapping("/admin/security-probe")
        @ResponseBody
        String probe() {
            return "accepted";
        }
    }
}
