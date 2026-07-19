package com.crm.service;

import com.crm.domain.entity.Account;
import com.crm.domain.entity.Contact;
import com.crm.dto.request.ContactRequest;
import com.crm.dto.response.ContactResponse;
import com.crm.exception.DuplicateEmailException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.AccountRepository;
import com.crm.repository.ContactRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ContactService {

    private final ContactRepository contactRepository;
    private final AccountRepository accountRepository;
    private final CrmEventPublisher eventPublisher;
    private final WorkspaceContext workspaceContext;

    public ContactService(ContactRepository contactRepository,
                          AccountRepository accountRepository,
                          CrmEventPublisher eventPublisher,
                          WorkspaceContext workspaceContext) {
        this.contactRepository = contactRepository;
        this.accountRepository = accountRepository;
        this.eventPublisher = eventPublisher;
        this.workspaceContext = workspaceContext;
    }

    public ContactResponse create(ContactRequest request) {
        if (contactRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException("Contact", request.email());
        }
        Contact contact = mapToEntity(new Contact(), request);
        workspaceContext.currentUserPrimaryWorkspace().ifPresent(contact::setWorkspace);
        ContactResponse response = ContactResponse.from(contactRepository.save(contact));
        eventPublisher.publishCreated("CONTACT", response.id());
        return response;
    }

    @Transactional(readOnly = true)
    public ContactResponse findById(Long id) {
        return ContactResponse.from(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<ContactResponse> findAll(Pageable pageable) {
        return findAll(pageable, null, null);
    }

    @Transactional(readOnly = true)
    public Page<ContactResponse> search(String name, Pageable pageable) {
        return findAll(pageable, name, null);
    }

    @Transactional(readOnly = true)
    public Page<ContactResponse> findAll(Pageable pageable, String search, Long accountId) {
        return contactRepository.findAll(scopeSpec(search, accountId), pageable).map(ContactResponse::from);
    }

    private Specification<Contact> scopeSpec(String search, Long accountId) {
        boolean isAdmin = workspaceContext.isAdmin();
        List<Long> wsIds = isAdmin ? List.of() : workspaceContext.currentUserWorkspaceIds();
        return ScopeSpecs.scoped(isAdmin, wsIds, accountId, search, "firstName", "lastName");
    }

    @Transactional(readOnly = true)
    public long count(String search) {
        if (workspaceContext.isAdmin()) {
            if (search != null && !search.isBlank()) {
                return contactRepository.countByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(search, search);
            }
            return contactRepository.count();
        }
        List<Long> wsIds = workspaceContext.currentUserWorkspaceIds();
        if (wsIds.isEmpty()) return 0;
        if (search != null && !search.isBlank()) {
            return contactRepository.countSearchByWorkspaceIds(search, wsIds);
        }
        return contactRepository.countByWorkspace_IdIn(wsIds);
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> findAllForExport(String search) {
        if (workspaceContext.isAdmin()) {
            if (search != null && !search.isBlank()) {
                return contactRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                        search, search, org.springframework.data.domain.Pageable.unpaged())
                        .getContent().stream().map(ContactResponse::from).toList();
            }
            return contactRepository.findAll().stream().map(ContactResponse::from).toList();
        }
        List<Long> wsIds = workspaceContext.currentUserWorkspaceIds();
        if (wsIds.isEmpty()) return List.of();
        if (search != null && !search.isBlank()) {
            return contactRepository.searchAllByWorkspaceIds(search, wsIds,
                    org.springframework.data.domain.Pageable.unpaged())
                    .stream().map(ContactResponse::from).toList();
        }
        return contactRepository.findAllByWorkspaceIds(wsIds).stream().map(ContactResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> findByAccount(Long accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Account", "id", accountId);
        }
        return contactRepository.findByAccount_Id(accountId)
                .stream().map(ContactResponse::from).toList();
    }

    public ContactResponse update(Long id, ContactRequest request) {
        Contact contact = getOrThrow(id);
        if (!request.email().equals(contact.getEmail()) && contactRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException("Contact", request.email());
        }
        ContactResponse response = ContactResponse.from(contactRepository.save(mapToEntity(contact, request)));
        eventPublisher.publishUpdated("CONTACT", id);
        return response;
    }

    public void delete(Long id) {
        contactRepository.delete(getOrThrow(id));
        eventPublisher.publishDeleted("CONTACT", id);
    }

    private Contact getOrThrow(Long id) {
        return contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact", "id", id));
    }

    private Contact mapToEntity(Contact contact, ContactRequest request) {
        contact.setFirstName(request.firstName());
        contact.setLastName(request.lastName());
        contact.setEmail(request.email());
        contact.setPhone(request.phone());
        contact.setJobTitle(request.jobTitle());
        contact.setDepartment(request.department());
        contact.setCompany(request.company());
        contact.setStatus(request.status());
        contact.setNotes(request.notes());

        if (request.accountId() != null) {
            Account account = accountRepository.findById(request.accountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account", "id", request.accountId()));
            contact.setAccount(account);
        } else {
            contact.setAccount(null);
        }
        return contact;
    }
}
