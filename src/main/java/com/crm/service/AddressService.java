package com.crm.service;

import com.crm.domain.entity.Address;
import com.crm.dto.request.AddressRequest;
import com.crm.dto.response.AddressResponse;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.AccountRepository;
import com.crm.repository.AddressRepository;
import com.crm.repository.ContactRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AddressService {

    private final AddressRepository addressRepository;
    private final AccountRepository accountRepository;
    private final ContactRepository contactRepository;

    public AddressService(AddressRepository addressRepository,
                          AccountRepository accountRepository,
                          ContactRepository contactRepository) {
        this.addressRepository = addressRepository;
        this.accountRepository = accountRepository;
        this.contactRepository = contactRepository;
    }

    public AddressResponse create(AddressRequest request) {
        Address address = mapToEntity(new Address(), request);
        return AddressResponse.from(addressRepository.save(address));
    }

    @Transactional(readOnly = true)
    public AddressResponse findById(Long id) {
        return AddressResponse.from(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> findByAccount(Long accountId) {
        return addressRepository.findByAccount_Id(accountId)
                .stream().map(AddressResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> findByContact(Long contactId) {
        return addressRepository.findByContact_Id(contactId)
                .stream().map(AddressResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> findAll() {
        return addressRepository.findAll().stream().map(AddressResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Page<AddressResponse> findAll(Pageable pageable) {
        return addressRepository.findAll(pageable).map(AddressResponse::from);
    }

    @Transactional(readOnly = true)
    public long count() {
        return addressRepository.count();
    }

    public AddressResponse update(Long id, AddressRequest request) {
        Address address = getOrThrow(id);
        return AddressResponse.from(addressRepository.save(mapToEntity(address, request)));
    }

    public void delete(Long id) {
        addressRepository.delete(getOrThrow(id));
    }

    public AddressResponse toggleEnabled(Long id) {
        Address address = getOrThrow(id);
        address.setEnabled(!address.isEnabled());
        return AddressResponse.from(addressRepository.save(address));
    }

    private Address getOrThrow(Long id) {
        return addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", id));
    }

    private Address mapToEntity(Address address, AddressRequest request) {
        address.setType(request.type());
        address.setStreet(request.street());
        address.setCity(request.city());
        address.setState(request.state());
        address.setPostalCode(request.postalCode());
        address.setCountry(request.country());
        if (request.accountId() != null) {
            address.setAccount(accountRepository.findById(request.accountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account", "id", request.accountId())));
            address.setContact(null);
        } else if (request.contactId() != null) {
            address.setContact(contactRepository.findById(request.contactId())
                    .orElseThrow(() -> new ResourceNotFoundException("Contact", "id", request.contactId())));
            address.setAccount(null);
        }
        return address;
    }
}
