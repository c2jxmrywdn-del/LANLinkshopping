package com.lanlink.shopping.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.lanlink.shopping.common.BusinessException;
import com.lanlink.shopping.dto.MarketingEmailCampaignDTO;
import com.lanlink.shopping.entity.UserProfile;
import com.lanlink.shopping.mapper.UserProfileMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MarketingEmailServiceTest {

    @Test
    void campaignSendsEachOptedInRecipientSeparatelyAndIncludesOptOut() {
        AgentLoadoutMcpClient client = mock(AgentLoadoutMcpClient.class);
        UserProfileMapper profileMapper = mock(UserProfileMapper.class);
        UserSettingsService settingsService = mock(UserSettingsService.class);
        when(client.isConfigured()).thenReturn(true);
        when(profileMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                profile(1L, "first@example.com"), profile(2L, "second@example.com")));
        when(settingsService.get(1L)).thenReturn(marketingOptInSettings());
        when(settingsService.get(2L)).thenReturn(marketingOptInSettings());
        MarketingEmailService service = new MarketingEmailService(client, profileMapper, settingsService);

        MarketingEmailCampaignDTO dto = campaign(List.of("first@example.com", "second@example.com"), true);
        var result = service.sendCampaign(dto);

        assertEquals(2, result.get("requested"));
        assertEquals(2, result.get("queued"));
        assertEquals(0, result.get("failed"));
        assertEquals(0, result.get("skippedNoConsent"));

        ArgumentCaptor<String> recipients = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjects = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodies = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(client, times(2)).sendMessage(recipients.capture(), subjects.capture(),
                bodies.capture(), isNull(), keys.capture());

        assertEquals(List.of("first@example.com", "second@example.com"), recipients.getAllValues());
        assertTrue(subjects.getAllValues().stream().allMatch(s -> s.startsWith("【商业推广】")));
        assertTrue(bodies.getAllValues().stream().allMatch(s -> s.contains("https://example.com/email-preferences/unsubscribe")));
        assertNotEquals(keys.getAllValues().get(0), keys.getAllValues().get(1));
        assertTrue(keys.getAllValues().stream().allMatch(k -> k.matches("[0-9a-f]{64}")));
    }

    @Test
    void campaignSkipsRecipientWithoutPromotionPreference() {
        AgentLoadoutMcpClient client = mock(AgentLoadoutMcpClient.class);
        UserProfileMapper profileMapper = mock(UserProfileMapper.class);
        UserSettingsService settingsService = mock(UserSettingsService.class);
        when(client.isConfigured()).thenReturn(true);
        when(profileMapper.selectList(any(Wrapper.class))).thenReturn(List.of(profile(1L, "person@example.com")));
        when(settingsService.get(1L)).thenReturn(Map.of("notify", Map.of(
                "email", true, "groups", Map.of("promotion", false))));
        MarketingEmailService service = new MarketingEmailService(client, profileMapper, settingsService);

        var result = service.sendCampaign(campaign(List.of("person@example.com"), true));

        assertEquals(0, result.get("queued"));
        assertEquals(1, result.get("skippedNoConsent"));
        verify(client, never()).sendMessage(anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void campaignRejectsWithoutBatchConsentConfirmation() {
        AgentLoadoutMcpClient client = mock(AgentLoadoutMcpClient.class);
        UserProfileMapper profileMapper = mock(UserProfileMapper.class);
        UserSettingsService settingsService = mock(UserSettingsService.class);
        when(client.isConfigured()).thenReturn(true);
        MarketingEmailService service = new MarketingEmailService(client, profileMapper, settingsService);

        assertThrows(BusinessException.class,
                () -> service.sendCampaign(campaign(List.of("person@example.com"), false)));
        verifyNoInteractions(profileMapper, settingsService);
        verify(client, never()).sendMessage(anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void campaignRejectsDuplicateRecipients() {
        AgentLoadoutMcpClient client = mock(AgentLoadoutMcpClient.class);
        UserProfileMapper profileMapper = mock(UserProfileMapper.class);
        UserSettingsService settingsService = mock(UserSettingsService.class);
        when(client.isConfigured()).thenReturn(true);
        MarketingEmailService service = new MarketingEmailService(client, profileMapper, settingsService);

        assertThrows(BusinessException.class, () -> service.sendCampaign(
                campaign(List.of("person@example.com", "PERSON@example.com"), true)));
        verifyNoInteractions(profileMapper, settingsService);
        verify(client, never()).sendMessage(anyString(), anyString(), anyString(), any(), anyString());
    }

    private static UserProfile profile(Long id, String email) {
        UserProfile profile = new UserProfile();
        profile.setUserId(id);
        profile.setEmail(email);
        return profile;
    }

    private static Map<String, Object> marketingOptInSettings() {
        Map<String, Object> groups = new LinkedHashMap<>();
        groups.put("promotion", true);
        Map<String, Object> notify = new LinkedHashMap<>();
        notify.put("email", true);
        notify.put("groups", groups);
        return Map.of("notify", notify);
    }

    private static MarketingEmailCampaignDTO campaign(List<String> recipients, boolean consent) {
        MarketingEmailCampaignDTO dto = new MarketingEmailCampaignDTO();
        dto.setRecipients(recipients);
        dto.setSubject("New supplier offers");
        dto.setText("See this week's B2B offers.");
        dto.setUnsubscribeUrl("https://example.com/email-preferences/unsubscribe");
        dto.setIdempotencyKey("campaign-2026-10");
        dto.setConsentConfirmed(consent);
        return dto;
    }
}
