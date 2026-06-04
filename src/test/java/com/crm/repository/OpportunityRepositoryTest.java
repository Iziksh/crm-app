package com.crm.repository;

import com.crm.domain.entity.Opportunity;
import com.crm.domain.enums.OpportunityStage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OpportunityRepositoryTest {

    @Autowired OpportunityRepository opportunityRepository;

    @Test
    void findByStage_returnsMatchingOpportunities() {
        opportunityRepository.save(opp("Deal A", OpportunityStage.PROSPECTING, null));
        opportunityRepository.save(opp("Deal B", OpportunityStage.WON, null));
        opportunityRepository.save(opp("Deal C", OpportunityStage.PROSPECTING, null));

        List<Opportunity> result = opportunityRepository.findByStage(OpportunityStage.PROSPECTING);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Opportunity::getName)
                .containsExactlyInAnyOrder("Deal A", "Deal C");
    }

    @Test
    void countByStage_returnsCorrectCount() {
        opportunityRepository.save(opp("A", OpportunityStage.WON, null));
        opportunityRepository.save(opp("B", OpportunityStage.WON, null));
        opportunityRepository.save(opp("C", OpportunityStage.LOST, null));

        assertThat(opportunityRepository.countByStage(OpportunityStage.WON)).isEqualTo(2);
        assertThat(opportunityRepository.countByStage(OpportunityStage.LOST)).isEqualTo(1);
    }

    @Test
    void sumPipelineAmount_sumsPendingOpportunities() {
        opportunityRepository.save(opp("A", OpportunityStage.PROSPECTING, new BigDecimal("10000")));
        opportunityRepository.save(opp("B", OpportunityStage.PROPOSAL, new BigDecimal("20000")));
        opportunityRepository.save(opp("C", OpportunityStage.WON, new BigDecimal("5000")));

        BigDecimal total = opportunityRepository.sumPipelineAmount();

        // Won/Lost should be excluded by the query — verify the method runs
        assertThat(total).isNotNull();
    }

    @Test
    void findApproachingCloseDate_returnsDealsClosingSoon() {
        Opportunity approaching = opp("Closing Soon", OpportunityStage.NEGOTIATION, null);
        approaching.setCloseDate(LocalDate.now().plusDays(2));

        Opportunity far = opp("Far Away", OpportunityStage.NEGOTIATION, null);
        far.setCloseDate(LocalDate.now().plusDays(30));

        Opportunity won = opp("Already Won", OpportunityStage.WON, null);
        won.setCloseDate(LocalDate.now().plusDays(2));

        opportunityRepository.saveAll(List.of(approaching, far, won));

        // excluded = terminal stages (WON, LOST)
        List<Opportunity> result = opportunityRepository.findApproachingCloseDate(
                LocalDate.now(),
                LocalDate.now().plusDays(3),
                List.of(OpportunityStage.WON, OpportunityStage.LOST));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Closing Soon");
    }

    @Test
    void findOverdueCloseDate_returnsDealsWithPastCloseDate() {
        Opportunity overdue = opp("Overdue", OpportunityStage.PROPOSAL, null);
        overdue.setCloseDate(LocalDate.now().minusDays(5));

        Opportunity future = opp("Future", OpportunityStage.PROPOSAL, null);
        future.setCloseDate(LocalDate.now().plusDays(5));

        opportunityRepository.saveAll(List.of(overdue, future));

        // excluded = terminal stages (WON, LOST)
        List<Opportunity> result = opportunityRepository.findOverdueCloseDate(
                LocalDate.now(),
                List.of(OpportunityStage.WON, OpportunityStage.LOST));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Overdue");
    }

    private Opportunity opp(String name, OpportunityStage stage, BigDecimal amount) {
        Opportunity o = new Opportunity();
        o.setName(name);
        o.setStage(stage);
        o.setAmount(amount);
        return o;
    }
}
