package com.lanlink.shopping.service;

import com.lanlink.shopping.common.BusinessException;
import com.lanlink.shopping.dto.MarketingEmailCampaignDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MarketingEmailServiceTest {

    @Test
    void campaignSendsEachConsentedRecipientSeparatelyAndIncludesOptOut() {
        AgentLoadoutMcpClient client = mock(AgentLoadoutMcpClient.class);
        when(client.isConfigured()).thenReturn(true);
        MarketingEmailService service = new MarketingEmailService(client);

        MarketingEmailCampaignDTO dto = new MarketingEmailCampaignDTO();
        dto.setRecipients(List.of("first@example.com", "second@example.com"));
        dto.setSubject("New supplier offers");
        dto.setText("See this week's B2B offers.");
        dto.setUnsubscribeUrl("https://example.com/email-preferences/unsubscribe");
        dto.setIdempotencyKey("campaign-2026-10");
        dto.setConsentConfirmed(true);

        var result = service.sendCampaign(dto);

        assertEquals(2, result.get("requested"));
        assertEquals(2, result.get("queued"));
        assertEquals(0, result.get("failed"));

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
    void campaignRejectsWithoutExplicitConsent() {
        AgentLoadoutMcpClient client = mock(AgentLoadoutMcpClient.class);
        when(client.isConfigured()).thenReturn(true);
        MarketingEmailService service = new MarketingEmailService(client);

        MarketingEmailCampaignDTO dto = new MarketingEmailCampaignDTO();
        dto.setRecipients(List.of("person@example.com"));
        dto.setSubject("Offer");
        dto.setText("Product news");
        dto.setUnsubscribeUrl("https://example.com/unsubscribe");
        dto.setIdempotencyKey("campaign-2026-10");
        dto.setConsentConfirmed(false);

        assertThrows(BusinessException.class, () -> service.sendCampaign(dto));
        verify(client, never()).sendMessage(anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void campaignRejectsDuplicateRecipients() {
        AgentLoadoutMcpClient client = mock(AgentLoadoutMcpClient.class);
        when(client.isConfigured()).thenReturn(true);
        MarketingEmailService service = new MarketingEmailService(client);

        MarketingEmailCampaignDTO dto = new MarketingEmailCampaignDTO();
        dto.setRecipients(List.of("person@example.com", "PERSON@example.com"));
        dto.setSubject("Offer");
        dto.setText("Product news");
        dto.setUnsubscribeUrl("https://example.com/unsubscribe");
        dto.setIdempotencyKey("campaign-2026-10");
        dto.setConsentConfirmed(true);

        assertThrows(BusinessException.class, () -> service.sendCampaign(dto));
        verify(client, never()).sendMessage(anyString(), anyString(), anyString(), any(), anyString());
    }
}
