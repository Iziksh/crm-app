package com.crm.service;

import com.crm.domain.entity.Contact;
import com.crm.domain.entity.Lead;
import com.crm.domain.entity.Opportunity;
import com.crm.domain.enums.LeadStatus;
import com.crm.domain.enums.OpportunityStage;
import com.crm.dto.request.LeadRequest;
import com.crm.dto.response.LeadResponse;
import com.crm.dto.response.OpportunityResponse;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.AccountRepository;
import com.crm.repository.ContactRepository;
import com.crm.repository.LeadRepository;
import com.crm.repository.OpportunityRepository;
import com.crm.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock LeadRepository leadRepository;
    @Mock OpportunityRepository opportunityRepository;
    @Mock UserRepository userRepository;
    @Mock AccountRepository accountRepository;
    @Mock ContactRepository contactRepository;
    @Mock NotificationService notificationService;
    @Mock CrmEventPublisher eventPublisher;

    @InjectMocks LeadService leadService;

    @Test
    void create_savesLead_andReturnsResponse() {
        LeadRequest req = new LeadRequest("New Client", null, null, null, null,
                null, null, null, null, null, null, null, null, null, null);
        Lead saved = new Lead();
        saved.setTitle("New Client");
        saved.setStatus(LeadStatus.NEW);
        when(leadRepository.save(any())).thenReturn(saved);

        LeadResponse response = leadService.create(req, "admin");

        assertThat(response.title()).isEqualTo("New Client");
        verify(leadRepository).save(any());
    }

    @Test
    void findById_throwsNotFound_whenMissing() {
        when(leadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leadService.findById(99L));
    }

    @Test
    void delete_callsRepositoryDelete() {
        Lead lead = new Lead();
        lead.setTitle("Old Lead");
        when(leadRepository.findById(1L)).thenReturn(Optional.of(lead));

        leadService.delete(1L);

        verify(leadRepository).delete(lead);
    }

    @Test
    void countByStatus_delegatesToRepository() {
        when(leadRepository.countByStatus(LeadStatus.NEW)).thenReturn(5L);

        assertThat(leadService.countByStatus(LeadStatus.NEW)).isEqualTo(5L);
    }

    @Test
    void convert_createsOpportunity_fromLead() {
        Lead lead = new Lead();
        lead.setId(1L);
        lead.setTitle("Big Deal");
        lead.setFirstName("John");
        lead.setLastName("Doe");
        lead.setEmail("john@example.com");
        lead.setStatus(LeadStatus.CONTACTED);

        Opportunity savedOpp = new Opportunity();
        savedOpp.setName("Big Deal");
        savedOpp.setStage(OpportunityStage.PROSPECTING);

        Contact savedContact = new Contact();
        savedContact.setId(10L);

        when(leadRepository.findById(1L)).thenReturn(Optional.of(lead));
        when(contactRepository.save(any())).thenReturn(savedContact);
        when(leadRepository.save(any())).thenReturn(lead);
        when(opportunityRepository.save(any())).thenReturn(savedOpp);

        OpportunityResponse response = leadService.convert(1L);

        assertThat(response.name()).isEqualTo("Big Deal");
        verify(opportunityRepository).save(any());
        // Lead status updated to QUALIFIED
        assertThat(lead.getStatus()).isEqualTo(LeadStatus.QUALIFIED);
    }

    @Test
    void convert_throwsNotFound_whenLeadMissing() {
        when(leadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leadService.convert(99L));
    }

    @Test
    void update_savesChanges() {
        Lead lead = new Lead();
        lead.setTitle("Old Title");
        when(leadRepository.findById(1L)).thenReturn(Optional.of(lead));
        Lead updated = new Lead();
        updated.setTitle("New Title");
        updated.setStatus(LeadStatus.CONTACTED);
        when(leadRepository.save(any())).thenReturn(updated);

        LeadResponse response = leadService.update(1L,
                new LeadRequest("New Title", null, null, null, null, null,
                        LeadStatus.CONTACTED, null, null, null, null, null, null, null, null));

        assertThat(response.title()).isEqualTo("New Title");
        verify(leadRepository).save(any());
    }
}
