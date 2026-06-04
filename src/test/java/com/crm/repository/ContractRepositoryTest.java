package com.crm.repository;

import com.crm.domain.entity.Contract;
import com.crm.domain.enums.ContractStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ContractRepositoryTest {

    @Autowired ContractRepository contractRepository;

    @Test
    void findByStatus_returnsMatchingContracts() {
        contractRepository.save(contract("Active 1", ContractStatus.ACTIVE, null));
        contractRepository.save(contract("Active 2", ContractStatus.ACTIVE, null));
        contractRepository.save(contract("Expired", ContractStatus.EXPIRED, null));

        List<Contract> result = contractRepository.findByStatus(ContractStatus.ACTIVE);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Contract::getTitle)
                .containsExactlyInAnyOrder("Active 1", "Active 2");
    }

    @Test
    void countByStatus_returnsCorrectCount() {
        contractRepository.save(contract("A", ContractStatus.ACTIVE, null));
        contractRepository.save(contract("B", ContractStatus.DRAFT, null));
        contractRepository.save(contract("C", ContractStatus.ACTIVE, null));

        assertThat(contractRepository.countByStatus(ContractStatus.ACTIVE)).isEqualTo(2);
        assertThat(contractRepository.countByStatus(ContractStatus.DRAFT)).isEqualTo(1);
    }

    @Test
    void findExpiringBetween_returnsActiveContractsExpiringInRange() {
        LocalDate soon = LocalDate.now().plusDays(15);
        LocalDate far  = LocalDate.now().plusDays(60);

        contractRepository.save(contract("Expiring Soon",   ContractStatus.ACTIVE, soon));
        contractRepository.save(contract("Expiring Far",    ContractStatus.ACTIVE, far));
        contractRepository.save(contract("Already Expired", ContractStatus.EXPIRED, soon));

        List<Contract> result = contractRepository.findExpiringBetween(
                ContractStatus.ACTIVE,
                LocalDate.now(),
                LocalDate.now().plusDays(30));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Expiring Soon");
    }

    @Test
    void findExpiredActive_returnsActiveContractsWithPastEndDate() {
        LocalDate past   = LocalDate.now().minusDays(5);
        LocalDate future = LocalDate.now().plusDays(5);

        contractRepository.save(contract("Past Active",   ContractStatus.ACTIVE, past));
        contractRepository.save(contract("Future Active", ContractStatus.ACTIVE, future));
        contractRepository.save(contract("Past Expired",  ContractStatus.EXPIRED, past));

        List<Contract> result = contractRepository.findExpiredActive(
                ContractStatus.ACTIVE, LocalDate.now());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Past Active");
    }

    @Test
    void findByEndDateBefore_returnsContractsEndingBeforeDate() {
        contractRepository.save(contract("Old 1", ContractStatus.ACTIVE, LocalDate.now().minusDays(10)));
        contractRepository.save(contract("Old 2", ContractStatus.ACTIVE, LocalDate.now().minusDays(1)));
        contractRepository.save(contract("Future", ContractStatus.ACTIVE, LocalDate.now().plusDays(10)));

        List<Contract> result = contractRepository.findByEndDateBefore(LocalDate.now());

        assertThat(result).hasSize(2);
    }

    private Contract contract(String title, ContractStatus status, LocalDate endDate) {
        Contract c = new Contract();
        c.setTitle(title);
        c.setStatus(status);
        c.setEndDate(endDate);
        return c;
    }
}
