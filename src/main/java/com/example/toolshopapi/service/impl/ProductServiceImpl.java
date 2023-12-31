package com.example.toolshopapi.service.impl;

import com.example.toolshopapi.dto.product_dto.ProductDto;
import com.example.toolshopapi.dto.product_dto.ProductInputDto;
import com.example.toolshopapi.dto.product_dto.ProductInputFindDto;
import com.example.toolshopapi.dto.product_dto.ProductInputSortDto;
import com.example.toolshopapi.mapping.ProductMapper;
import com.example.toolshopapi.model.enums.LabelStatus;
import com.example.toolshopapi.model.models.product.Inventory;
import com.example.toolshopapi.model.models.product.Product;
import com.example.toolshopapi.repository.ProductRepository;
import com.example.toolshopapi.service.iterfaces.InventoryService;
import com.example.toolshopapi.service.iterfaces.ProductService;
import com.example.toolshopapi.service.utils.ProductSpecifications;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductDto save(ProductInputDto productDto, Integer availableQuantity) {
        if (productDto == null){
            throw new IllegalArgumentException("productDto is null please check value");
        }

        Product product = new Product();

        product.setPrice(productDto.getPrice());
        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setCategory(productDto.getCategory());
        product.setSoldQuantity(0);
        product.setLabel(LabelStatus.NEW);

        product = productRepository.save(product);


        Inventory inventory = new Inventory();
        inventory.setAvailableQuantity(availableQuantity);
        inventory.setReceiptTime(LocalDateTime.now());

        inventory.setProduct(product);

        inventoryService.save(inventory);
        product.setInventory(inventory);

        return productMapper.toProductDto(product);
    }

    @Override
    @Transactional
    public ProductDto update(ProductDto productDto) {
        if (productDto == null){
            throw new IllegalArgumentException("productDto is null please check value");
        }

        Product product = findProductByName(productDto.getName());

        product.setCategory(productDto.getCategory());
        product.setPrice(productDto.getPrice());
        product.setDescription(productDto.getDescription());
        product.setName(product.getName());
        product.setSoldQuantity(productDto.getSoldQuantity());
        if (product.getSoldQuantity() > 3){
            product.setLabel(LabelStatus.BESTSELLER);
        }

        return productMapper.toProductDto(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDto findByName(String name) {
        if (name == null){
            throw new IllegalArgumentException("name is null ");
        }
        Product product = findProductByName(name);

        return productMapper.toProductDto(product);
    }

    @Override
    public Product findById(Long productId){
        return productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException(String.format("product not found by id: %s",productId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getAll() {
        List<Product> products = productRepository.findAll();

        return products.stream()
                .map(productMapper::toProductDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(String name) {

        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        Product product = findProductByName(name);

        productRepository.delete(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllCategory() {
        return productRepository.getAllCategory()
                .orElseThrow(() -> new EntityNotFoundException("backes has no categoryes"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDto> searchAndSortProducts(ProductInputSortDto productInputSortDto) {
        Specification<Product> specification = buildProductSpecification(productInputSortDto);

        Sort sort = Sort.by(productInputSortDto.getSortDirection().equals("asc") ? Sort.Order.asc("price") : Sort.Order.desc("price"));
        Pageable pageable = PageRequest.of(productInputSortDto.getPage(), productInputSortDto.getSize(), sort);

        Page<Product> productPage = productRepository.findAll(specification, pageable);

        return productPage.map(productMapper::toProductDto);
    }

    private Product findProductByName(String name) {
        return productRepository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException("product with name " + name +
                        " not found"));
    }
    @Override
    @Transactional(readOnly = true)
    public Page<ProductDto> findProductsByNameOrByDescription(ProductInputFindDto productInputFindDto) {
        if (productInputFindDto == null) {
            throw new IllegalArgumentException("Parameter 'productInputFindDto' is null");
        }

        Pageable pageable = PageRequest.of(productInputFindDto.getPage(), productInputFindDto.getSize());
        Page<Product> productPage = productRepository.findProductsByNameOrByDescription(productInputFindDto.getQuery(), pageable);

        return productPage.map(productMapper::toProductDto);
    }




    private Specification<Product> buildProductSpecification(ProductInputSortDto productInputSortDto) {
        Specification<Product> specification = Specification.where(null);

        if (productInputSortDto.getMinPrice() != null && productInputSortDto.getMaxPrice() != null) {
            specification = specification.and(ProductSpecifications.hasPriceBetween(productInputSortDto.getMinPrice(), productInputSortDto.getMaxPrice()));
            return specification;
        }

        if (productInputSortDto.getMinPrice() != null) {
            specification = specification.and(ProductSpecifications.hasPriceGreaterThanEqual(productInputSortDto.getMinPrice()));
            return specification;
        }

        if (productInputSortDto.getMaxPrice() != null) {
            specification = specification.and(ProductSpecifications.hasPriceLessThanEqual(productInputSortDto.getMaxPrice()));
            return specification;
        }

        if (productInputSortDto.getCategory() != null) {
            specification = specification.and(ProductSpecifications.hasCategory(productInputSortDto.getCategory()));
            return specification;
        }

        return specification;
    }

}
