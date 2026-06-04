package com.crm.repository;

import com.crm.domain.entity.Lead;
import com.crm.domain.enums.LeadStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LeadRepositoryTest {

    @Autowired LeadRepository leadRepository;

    @Test
    void findByStatus_returnsLeadsWithMatchingStatus() {
        leadRepository.save(lead("Lead A", LeadStatus.NEW));
        leadRepository.save(lead("Lead B", LeadStatus.CONTACTED));
        leadRepository.save(lead("Lead C", LeadStatus.NEW));

        List<Lead> result = leadRepository.findByStatus(LeadStatus.NEW);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Lead::getTitle).containsExactlyInAnyOrder("Lead A", "Lead C");
    }

    @Test
    void countByStatus_returnsCorrectCount() {
        leadRepository.save(lead("A", LeadStatus.WON));
        leadRepository.save(lead("B", LeadStatus.WON));
        leadRepository.save(lead("C", LeadStatus.LOST));

        assertThat(leadRepository.countByStatus(LeadStatus.WON)).isEqualTo(2);
        assertThat(leadRepository.countByStatus(LeadStatus.LOST)).isEqualTo(1);
        assertThat(leadRepository.countByStatus(LeadStatus.NEW)).isZero();
    }

    @Test
    void findByStatusNot_excludesSpecifiedStatus() {
        leadRepository.save(lead("Active", LeadStatus.NEW));
        leadRepository.save(lead("Won", LeadStatus.WON));
        leadRepository.save(lead("Lost", LeadStatus.LOST));

        List<Lead> result = leadRepository.findByStatusNot(LeadStatus.LOST);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Lead::getTitle).doesNotContain("Lost");
    }

    @Test
    void findOverdueCloseDate_returnsLeads_withPastCloseDateAndActiveStatus() {
        Lead overdue = lead("Overdue", LeadStatus.NEW);
        overdue.setCloseDate(LocalDate.now().minusDays(5));

        Lead future = lead("Future", LeadStatus.NEW);
        future.setCloseDate(LocalDate.now().plusDays(5));

        Lead won = lead("Won", LeadStatus.WON);
        won.setCloseDate(LocalDate.now().minusDays(5));

        leadRepository.saveAll(List.of(overdue, future, won));

        // excluded = terminal statuses (WON, LOST); overdue lead is NEW so not excluded
        List<Lead> result = leadRepository.findOverdueCloseDate(
                LocalDate.now(),
                List.of(LeadStatus.WON, LeadStatus.LOST));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Overdue");
    }

    @Test
    void findByStatus_returnsEmpty_whenNoMatches() {
        leadRepository.save(lead("Lead", LeadStatus.NEW));

        assertThat(leadRepository.findByStatus(LeadStatus.WON)).isEmpty();
    }

    private Lead lead(String title, LeadStatus status) {
        Lead l = new Lead();
        l.setTitle(title);
        l.setStatus(status);
        return l;
    }
}
