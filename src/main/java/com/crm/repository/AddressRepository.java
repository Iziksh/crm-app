package com.crm.repository;

import com.crm.domain.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long>, JpaSpecificationExecutor<Address> {
    List<Address> findByAccount_Id(Long accountId);
    List<Address> findByContact_Id(Long contactId);
    List<Address> findByEnabled(boolean enabled);
}
