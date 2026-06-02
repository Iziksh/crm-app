package com.crm.service;

import com.crm.domain.entity.Product;
import com.crm.dto.request.ProductRequest;
import com.crm.dto.response.ProductResponse;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        mapToEntity(product, request);
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return ProductResponse.from(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> findAll(Pageable pageable, String search) {
        if (search != null && !search.isBlank()) {
            return productRepository.findByNameContainingIgnoreCaseAndActive(search, true, pageable)
                    .map(ProductResponse::from);
        }
        return productRepository.findByActive(true, pageable).map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> findAll(Pageable pageable, String search, boolean activeOnly) {
        if (!activeOnly) {
            if (search != null && !search.isBlank()) {
                return productRepository.findByNameContainingIgnoreCaseAndActive(search, false, pageable)
                        .map(ProductResponse::from);
            }
            return productRepository.findAll(pageable).map(ProductResponse::from);
        }
        return findAll(pageable, search);
    }

    @Transactional(readOnly = true)
    public long count(String search) {
        if (search != null && !search.isBlank()) {
            return productRepository.countByNameContainingIgnoreCaseAndActive(search, true);
        }
        return productRepository.countByActive(true);
    }

    @Transactional(readOnly = true)
    public long countAll(String search) {
        if (search != null && !search.isBlank()) {
            return productRepository.findByNameContainingIgnoreCaseAndActive(search, true,
                    Pageable.unpaged()).getTotalElements()
                    + productRepository.findByNameContainingIgnoreCaseAndActive(search, false,
                    Pageable.unpaged()).getTotalElements();
        }
        return productRepository.count();
    }

    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getOrThrow(id);
        mapToEntity(product, request);
        return ProductResponse.from(productRepository.save(product));
    }

    public ProductResponse toggleActive(Long id) {
        Product product = getOrThrow(id);
        product.setActive(!product.isActive());
        return ProductResponse.from(productRepository.save(product));
    }

    public void delete(Long id) {
        productRepository.delete(getOrThrow(id));
    }

    private Product getOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    private void mapToEntity(Product product, ProductRequest request) {
        product.setSku(request.sku());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setCategory(request.category());
        product.setUnitPrice(request.unitPrice());
        product.setCurrency(request.currency() != null ? request.currency() : "USD");
    }
}
